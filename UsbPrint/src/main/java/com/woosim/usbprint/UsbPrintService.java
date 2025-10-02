package com.woosim.usbprint;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;

public class UsbPrintService {
    private static final String TAG = "UsbPrintService";
    static final int VID = 0x2A92;

    private final UsbDeviceConnection mDeviceConnection;
    private final UsbEndpoint mEndpointOut;
    private final UsbEndpoint mEndpointIn;
    private final Handler mHandler;
    private final WaiterThread mWaiterThread = new WaiterThread();

    UsbPrintService(UsbDeviceConnection connection, UsbInterface intf, Handler handler) {
        mDeviceConnection = connection;
        mHandler = handler;
        UsbEndpoint epOut = null;
        UsbEndpoint epIn = null;

        // look for our bulk endpoints
        for (int i = 0 ; i < intf.getEndpointCount() ; i++) {
            UsbEndpoint ep = intf.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epOut = ep;
                } else {
                    epIn = ep;
                }
            }
        }
        if (epOut == null || epIn == null) {
            throw new IllegalArgumentException("not all endpoints found");
        }
        mEndpointOut = epOut;
        mEndpointIn = epIn;
    }

    void start() {
        mWaiterThread.start();
    }

    void stop() {
        synchronized (mWaiterThread) {
            mWaiterThread.mStop = true;
        }
    }

    // before Android 9 (API 28), max data length is 16KB
    void send(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        synchronized (this) {
            UsbRequest request = new UsbRequest();
            request.initialize(mDeviceConnection, mEndpointOut);
            if (!request.queue(buffer)) {
                Log.e(TAG, "out request queue failed");
            }
        }
    }

    private void receive(ByteBuffer buffer) {
        int bytes = buffer.position();
        if (bytes > 0) {
            byte[] data = new byte[bytes];
            buffer.position(0);
            buffer.get(data);
            mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, 0, data).sendToTarget();
        }
    }

    private class WaiterThread extends Thread {
        boolean mStop;
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(128);
            UsbRequest requestIn = new UsbRequest();
            requestIn.initialize(mDeviceConnection, mEndpointIn);
            requestIn.setClientData(buffer);
            if (!requestIn.queue(buffer)) {
                Log.e(TAG, "in request queue failed");
            }
            while (true) {
                synchronized (this) {
                    if (mStop)  return;
                }
                UsbRequest request = mDeviceConnection.requestWait();
                if (request == null) {
                    Log.e(TAG, "Error occurred in requestWait");
                    break;
                } else if (request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN) {
                    // Receive data from InEp
                    receive((ByteBuffer) request.getClientData());
                    buffer.clear();
                    if (!requestIn.queue(buffer)) {
                        Log.e(TAG, "inner request queue failed");
                    }
                } else {
                    // Receive data from OutEp
                    request.close();
                }
            }
        }
    }
}

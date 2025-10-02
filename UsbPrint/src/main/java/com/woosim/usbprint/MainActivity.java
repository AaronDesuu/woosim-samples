package com.woosim.usbprint;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.woosim.printer.WoosimBarcode;
import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimImage;
import com.woosim.printer.WoosimService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String ACTION_USB_PERMISSION = "com.android.woosim.USB_PERMISSION";

    public static final int MESSAGE_READ = 3;

    private PendingIntent mPermissionIntent;
    private UsbManager mManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mDeviceConnection;
    private UsbInterface mInterface;
    private UsbPrintService mPrintService = null;
    private WoosimService mWoosim = null;

    private TextView mTrack1View;
    private TextView mTrack2View;
    private TextView mTrack3View;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION),PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        else
            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);

        mWoosim = new WoosimService(mHandler);
        mTrack1View = findViewById(R.id.textViewTrack1);
        mTrack2View = findViewById(R.id.textViewTrack2);
        mTrack3View = findViewById(R.id.textViewTrack3);

        // listen for new devices
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        // check for existing devices
        for (UsbDevice device : mManager.getDeviceList().values()) {
            if (device.getVendorId() == UsbPrintService.VID) {
                mManager.requestPermission(device, mPermissionIntent);
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        setDeviceInterface(null, null);
        super.onDestroy();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getVendorId() == UsbPrintService.VID) {
                    mManager.requestPermission(device, mPermissionIntent);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                String deviceName = device.getDeviceName();
                if (mDevice != null && mDevice.getDeviceName().equals(deviceName)) {
                    setDeviceInterface(null, null);
                }
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // Set up device communication
                            setDeviceInterface(device, device.getInterface(0));
                        }
                    }
                    else {
                        Log.w(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    private void setDeviceInterface(UsbDevice device, UsbInterface intf) {
        if (mDeviceConnection != null) {
            if (mInterface != null) {
                mDeviceConnection.releaseInterface(mInterface);
                mInterface = null;
            }
            mDeviceConnection.close();
            mDevice = null;
            mDeviceConnection = null;
        }
        if (device != null && intf != null) {
            UsbDeviceConnection connection = mManager.openDevice(device);
            if (connection != null) {
                Log.d(TAG, "device open succeeded");
                if (connection.claimInterface(intf, true)) {
                    Log.d(TAG, "claim interface succeeded");
                    mDevice = device;
                    mDeviceConnection = connection;
                    mInterface = intf;
                    mPrintService = new UsbPrintService(mDeviceConnection, intf, mHandler);
                    mPrintService.start();
                    return;
                } else {
                    Log.e(TAG, "claim interface failed");
                    connection.close();
                }
            } else {
                Log.e(TAG, "device open failed");
            }
        }
        if (mDeviceConnection == null && mPrintService != null) {
            mPrintService.stop();
            mPrintService = null;
        }
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    mWoosim.processRcvData((byte[])msg.obj, msg.arg1);
                    break;
                case WoosimService.MESSAGE_PRINTER:
                    if (msg.arg1 == WoosimService.MSR) {
                        if (msg.arg2 == 0) {
                            Toast.makeText(getApplicationContext(), R.string.msr_failure, Toast.LENGTH_SHORT).show();
                        } else {
                            byte[][] track = (byte[][])msg.obj;
                            if (track[0] != null) {
                                String str = new String(track[0]);
                                mTrack1View.setText(str);
                            }
                            if (track[1] != null) {
                                String str = new String(track[1]);
                                mTrack2View.setText(str);
                            }
                            if (track[2] != null) {
                                String str = new String(track[2]);
                                mTrack3View.setText(str);
                            }
                        }
                    }
                    break;
            }
        }
    };

    private void sendData(byte[] data) {
        if (mPrintService == null) {
            Toast.makeText(this, R.string.connect_failure, Toast.LENGTH_SHORT).show();
            return;
        }
        if (data.length > 0)
            mPrintService.send(data);
    }

    /**
     * On click function for sample print button.
     */
    public void printReceipt(View v) {
        InputStream inStream = getResources().openRawResource(R.raw.receipt);
        sendData(WoosimCmd.initPrinter());
        try {
            byte[] data = new byte[inStream.available()];
            while (inStream.read(data) != -1)
            {
                sendData(data);
            }
        } catch (IOException e) {
            Log.e(TAG, "sample 2inch receipt print fail.", e);
        } finally {
            try {
                inStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void printImage(View v) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.logo, options);
        if (bmp == null) {
            Log.e(TAG, "resource decoding is failed");
            return;
        }
        sendData(WoosimCmd.setPageMode());
        sendData(WoosimImage.printBitmap(0, 0, 384, 200, bmp));
        bmp.recycle();
        sendData(WoosimCmd.PM_setStdMode());
    }

    public void print1DBarcode(View v) {
        final byte[] barcode =  {0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x30};
        final byte[] barcode8 = {0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37};
        final byte[] barcodeUPCE = {0x30,0x36,0x35,0x31,0x30,0x30,0x30,0x30,0x34,0x33,0x32,0x37};
        final byte[] cmdPrint = WoosimCmd.printData();
        final String title1 = "UPC-A Barcode\r\n";
        byte[] UPCA = WoosimBarcode.createBarcode(WoosimBarcode.UPC_A, 2, 60, true, barcode);
        final String title2 = "UPC-E Barcode\r\n";
        byte[] UPCE = WoosimBarcode.createBarcode(WoosimBarcode.UPC_E, 2, 60, true, barcodeUPCE);
        final String title3 = "EAN13 Barcode\r\n";
        byte[] EAN13 = WoosimBarcode.createBarcode(WoosimBarcode.EAN13, 2, 60, true, barcodeUPCE);
        final String title4 = "EAN8 Barcode\r\n";
        byte[] EAN8 = WoosimBarcode.createBarcode(WoosimBarcode.EAN8, 2, 60, true, barcode8);
        final String title5 = "CODE39 Barcode\r\n";
        byte[] CODE39 = WoosimBarcode.createBarcode(WoosimBarcode.CODE39, 2, 60, true, barcode);
        final String title6 = "ITF Barcode\r\n";
        byte[] ITF = WoosimBarcode.createBarcode(WoosimBarcode.ITF, 2, 60, true, barcode);
        final String title7 = "CODEBAR Barcode\r\n";
        byte[] CODEBAR = WoosimBarcode.createBarcode(WoosimBarcode.CODEBAR, 2, 60, true, barcode);
        final String title8 = "CODE93 Barcode\r\n";
        byte[] CODE93 = WoosimBarcode.createBarcode(WoosimBarcode.CODE93, 2, 60, true, barcode);
        final String title9 = "CODE128 Barcode\r\n";
        byte[] CODE128 = WoosimBarcode.createBarcode(WoosimBarcode.CODE128, 2, 60, true, barcode);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
        try {
            byteStream.write(WoosimCmd.initPrinter());
            byteStream.write(title1.getBytes()); byteStream.write(UPCA); byteStream.write(cmdPrint);
            byteStream.write(title2.getBytes()); byteStream.write(UPCE); byteStream.write(cmdPrint);
            byteStream.write(title3.getBytes()); byteStream.write(EAN13); byteStream.write(cmdPrint);
            byteStream.write(title4.getBytes()); byteStream.write(EAN8); byteStream.write(cmdPrint);
            byteStream.write(title5.getBytes()); byteStream.write(CODE39); byteStream.write(cmdPrint);
            byteStream.write(title6.getBytes()); byteStream.write(ITF); byteStream.write(cmdPrint);
            byteStream.write(title7.getBytes()); byteStream.write(CODEBAR); byteStream.write(cmdPrint);
            byteStream.write(title8.getBytes()); byteStream.write(CODE93); byteStream.write(cmdPrint);
            byteStream.write(title9.getBytes()); byteStream.write(CODE128); byteStream.write(cmdPrint);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendData(byteStream.toByteArray());
    }

    public void print2DBarcode(View v) {
        final byte[] barcode = {0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x30};
        final byte[] cmdPrint = WoosimCmd.printData();
        final String title1 = "PDF417 2D Barcode\r\n";
        byte[] PDF417 = WoosimBarcode.create2DBarcodePDF417(2, 3, 4, 2, false, barcode);
        final String title2 = "DATAMATRIX 2D Barcode\r\n";
        byte[] dataMatrix = WoosimBarcode.create2DBarcodeDataMatrix(0, 0, 6, barcode);
        final String title3 = "QR-CODE 2D Barcode\r\n";
        byte[] QRCode = WoosimBarcode.create2DBarcodeQRCode(0, (byte)0x4d, 5, barcode);
        final String title4 = "Micro PDF417 2D Barcode\r\n";
        byte[] microPDF417 = WoosimBarcode.create2DBarcodeMicroPDF417(2, 2, 0, 2, barcode);
        final String title5 = "Truncated PDF417 2D Barcode\r\n";
        byte[] truncPDF417 = WoosimBarcode.create2DBarcodeTruncPDF417(2, 3, 4, 2, false, barcode);
        // Maxicode can be printed only with RX version
        final String title6 = "Maxicode 2D Barcode\r\n";
        final byte[] mxcode = {0x41,0x42,0x43,0x44,0x45,0x31,0x32,0x33,0x34,0x35,0x61,0x62,0x63,0x64,0x65};
        byte[] maxCode = WoosimBarcode.create2DBarcodeMaxicode(4, mxcode);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
        try {
            byteStream.write(WoosimCmd.initPrinter());
            byteStream.write(title1.getBytes()); byteStream.write(PDF417); byteStream.write(cmdPrint);
            byteStream.write(title2.getBytes()); byteStream.write(dataMatrix); byteStream.write(cmdPrint);
            byteStream.write(title3.getBytes()); byteStream.write(QRCode); byteStream.write(cmdPrint);
            byteStream.write(title4.getBytes()); byteStream.write(microPDF417); byteStream.write(cmdPrint);
            byteStream.write(title5.getBytes()); byteStream.write(truncPDF417); byteStream.write(cmdPrint);
            byteStream.write(title6.getBytes()); byteStream.write(maxCode); byteStream.write(cmdPrint);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendData(byteStream.toByteArray());
    }

    public void printGS1Databar(View v) {
        final byte[] data = {0x30,0x30,0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x30};
        final byte[] cmdPrint = WoosimCmd.printData();
        final String title0 = "GS1 Databar type0\r\n";
        byte[] gs0 = WoosimBarcode.createGS1Databar(0, 2, data);
        final String title1 = "GS1 Databar type1\r\n";
        byte[] gs1 = WoosimBarcode.createGS1Databar(1, 2, data);
        final String title2 = "GS1 Databar type2\r\n";
        byte[] gs2 = WoosimBarcode.createGS1Databar(2, 2, data);
        final String title3 = "GS1 Databar type3\r\n";
        byte[] gs3 = WoosimBarcode.createGS1Databar(3, 2, data);
        final String title4 = "GS1 Databar type4\r\n";
        byte[] gs4 = WoosimBarcode.createGS1Databar(4, 2, data);
        final String title5 = "GS1 Databar type5\r\n";
        final byte[] data5 = {0x5b,0x30,0x31,0x5d,0x39,0x30,0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x30,0x38,
                0x5b,0x33,0x31,0x30,0x33,0x5d,0x30,0x31,0x32,0x32,0x33,0x33};
        byte[] gs5 = WoosimBarcode.createGS1Databar(5, 2, data5);
        final String title6 = "GS1 Databar type6\r\n";
        final byte[] data6 = {0x5b,0x30,0x31,0x5d,0x39,0x30,0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x30,0x38,
                0x5b,0x33,0x31,0x30,0x33,0x5d,0x30,0x31,0x32,0x32,0x33,0x33,
                0x5b,0x31,0x35,0x5d,0x39,0x39,0x31,0x32,0x33,0x31};
        byte[] gs6 = WoosimBarcode.createGS1Databar(6, 4, data6);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
        try {
            byteStream.write(WoosimCmd.initPrinter());
            byteStream.write(title0.getBytes()); byteStream.write(gs0); byteStream.write(cmdPrint);
            byteStream.write(title1.getBytes()); byteStream.write(gs1); byteStream.write(cmdPrint);
            byteStream.write(title2.getBytes()); byteStream.write(gs2); byteStream.write(cmdPrint);
            byteStream.write(title3.getBytes()); byteStream.write(gs3); byteStream.write(cmdPrint);
            byteStream.write(title4.getBytes()); byteStream.write(gs4); byteStream.write(cmdPrint);
            byteStream.write(title5.getBytes()); byteStream.write(gs5); byteStream.write(cmdPrint);
            byteStream.write(title6.getBytes()); byteStream.write(gs6); byteStream.write(cmdPrint);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendData(byteStream.toByteArray());
    }

    public void setMSRDoubleTrackMode(View v) {
        clearMSRInfo();
        mWoosim.clearRcvBuffer();
        sendData(WoosimCmd.MSR_doubleTrackMode());
    }

    public void setMSRTripleTrackMode(View v) {
        clearMSRInfo();
        mWoosim.clearRcvBuffer();
        sendData(WoosimCmd.MSR_tripleTrackMode());
    }

    public void cancelMSRMode(View v) {
        sendData(WoosimCmd.MSR_exit());
    }

    public void clearMSRInfo(View v) {
        clearMSRInfo();
    }

    public void clearMSRInfo() {
        mTrack1View.setText("");
        mTrack2View.setText("");
        mTrack3View.setText("");
    }
}
package com.woosim.wifiprint;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

class WifiPrintService {
    private static final String TAG = "WifiPrintService";

    private final Handler mHandler;
    private int mState;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    // Constants that indicate the current connection state
    private static final int STATE_NONE = 0;       // we're doing nothing
    private static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    static final int STATE_CONNECTED = 2;          // now connected to a remote device

    /**
     * Constructor.
     * @param handler  A Handler to send messages back to the UI Activity
     */
    WifiPrintService(Handler handler) {
        mState = STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(int state) {
        mState = state;
    }

    synchronized int getState() {
        return mState;
    }

    synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    synchronized void connect(String ip, String port) {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(ip, port);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    private synchronized void connected(Socket socket) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE, R.string.msg_connect, 0).sendToTarget();
        setState(STATE_CONNECTED);
    }

    /**
     * Write to the ConnectedThread in an un-synchronized manner
     * Using thread to avoid NetworkOnMainThreadException from Android 7
     * @param out The bytes to write
     */
    void write(final byte[] out) {
        new Thread(() -> mConnectedThread.write(out)).start();
    }

    private void connectionFailed(int strId) {
        if (mState == STATE_NONE)
            return;
        // Send a failure message back to the Activity
        mHandler.obtainMessage(MainActivity.MESSAGE_TOAST, strId, 0).sendToTarget();
        this.stop();
    }

    private void connectionLost() {
        // When the application is destroyed, just return
        if (mState == STATE_NONE)
            return;
        // Send a message back to the Activity
        mHandler.obtainMessage(MainActivity.MESSAGE_TOAST, R.string.msg_connect_lost, 0).sendToTarget();
        this.stop();
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a device.
     * It runs straight through the connection either succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final Socket mmSocket;
        private final String mmDeviceIP;
        private final int mmPort;

        ConnectThread(String ip, String port) {
            Log.d(TAG, "ConnectThread with "+ ip +":" + port);
            mmSocket = new Socket();
            mmDeviceIP = ip;
            mmPort = Integer.parseInt(port);
        }

        public void run() {
            // Make a connection to the Socket
            try {
                SocketAddress socketAddress = new InetSocketAddress(mmDeviceIP, mmPort);
                mmSocket.connect(socketAddress, 2000);
            } catch (UnknownHostException e) {
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "unable to close socket during connection failure");
                    e1.printStackTrace();
                }
                connectionFailed(R.string.msg_unknown_ip);
                return;
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "unable to close socket during connection failure");
                    e1.printStackTrace();
                }
                connectionFailed(R.string.msg_connect_fail);
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "unable to close socket during connection failure", e);
            }
        }
    }

    /**
     * This thread runs after a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(Socket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    int bytes = mmInStream.read(buffer);
                    // buffer can be over-written by next input stream data, so it should be copied
                    byte[] rcvData = Arrays.copyOf(buffer, bytes);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, rcvData).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        synchronized void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {
            try {
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "unable to close stream or socket during disconnection", e);
            }
        }
    }
}

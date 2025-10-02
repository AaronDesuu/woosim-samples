package com.woosim.btprint;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.woosim.printer.WoosimBarcode;
import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimImage;
import com.woosim.printer.WoosimService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_READ = 3;
    public static final int PERMISSION_REQUEST = 100;

    public static final String DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothPrintService mPrintService = null;
    private WoosimService mWoosim = null;

    private int mCharExt = 1;
    private int mAlignment = WoosimCmd.ALIGN_LEFT;
    private TextView mTrack1View;
    private TextView mTrack2View;
    private TextView mTrack3View;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.toast_bt_na, Toast.LENGTH_LONG).show();
            finish();
        }
        checkPermission();
        mWoosim = new WoosimService(mHandler);

        RadioGroup alignmentGroup = findViewById(R.id.radioGroupAlignment);
        alignmentGroup.setOnCheckedChangeListener(mAlignmentChangeListener);

        Spinner charExtSpinner = findViewById(R.id.spinnerCharExt);
        charExtSpinner.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.char_size_array, android.R.layout.simple_spinner_dropdown_item));
        charExtSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position == 1) mCharExt = 2;
                        else if (position == 2) mCharExt = 3;
                        else if (position == 3) mCharExt = 4;
                        else if (position == 4) mCharExt = 5;
                        else if (position == 5) mCharExt = 6;
                        else if (position == 6) mCharExt = 7;
                        else if (position == 7) mCharExt = 8;
                        else mCharExt = 1;
                    }
                    public void onNothingSelected(AdapterView<?> parent) { }
                }
        );

        mTrack1View = findViewById(R.id.textViewTrack1);
        mTrack2View = findViewById(R.id.textViewTrack2);
        mTrack3View = findViewById(R.id.textViewTrack3);
    }

    RadioGroup.OnCheckedChangeListener mAlignmentChangeListener = (group, checkedId) -> {
        if (checkedId == R.id.radioLeft)
            mAlignment = WoosimCmd.ALIGN_LEFT;
        else if (checkedId == R.id.radioCenter)
            mAlignment = WoosimCmd.ALIGN_CENTER;
        else if (checkedId == R.id.radioRight)
            mAlignment = WoosimCmd.ALIGN_RIGHT;
    };

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {Manifest.permission.BLUETOOTH_CONNECT};
                requestPermissions(permissions, PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && (grantResults.length > 0)) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If Bluetooth is not on, request that it be enabled.
        if (mBluetoothAdapter.isEnabled()) {
            if (mPrintService == null)
                setupPrintService();
        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mBluetoothLauncher.launch(intent);
        }
    }

    ActivityResultLauncher<Intent> mBluetoothLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            setupPrintService();
        } else {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            finish();
        }
    });

    private void setupPrintService() {
        // Initialize the BluetoothPrintService to perform bluetooth connections
        mPrintService = new BluetoothPrintService(mHandler);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mPrintService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mPrintService.getState() == BluetoothPrintService.STATE_NONE) {
                // Start the Bluetooth print services
                mPrintService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        // Stop the Bluetooth print services
        if (mPrintService != null)
            mPrintService.stop();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (mPrintService != null && mPrintService.getState() == BluetoothPrintService.STATE_CONNECTED) {
            menu.findItem(R.id.search).setVisible(false);
            menu.findItem(R.id.disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.search).setVisible(true);
            menu.findItem(R.id.disconnect).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            Intent intent = new Intent(this, DeviceListActivity.class);
            mConnectLauncher.launch(intent);
            return true;
        } else if (item.getItemId() == R.id.disconnect) {
            if (mPrintService != null)
                mPrintService.start();
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    ActivityResultLauncher<Intent> mConnectLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent intent = result.getData();
            if (intent != null && intent.getExtras() != null) {
                // Get the device MAC address
                String address = intent.getExtras().getString(DEVICE_ADDRESS);
                // Get the Bluetooth device object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mPrintService.connect(device);
            }
        }
    });

    // The Handler that gets information back from the BluetoothPrintService
    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        switch (msg.what) {
            case MESSAGE_DEVICE_NAME:
                String mConnectedDeviceName = (String) msg.obj;
                Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.arg1, Toast.LENGTH_SHORT).show();
                break;
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
        return true;
    });

    private void sendData(byte[] data) {
        // Check that we're actually connected before trying printing
        if (mPrintService.getState() != BluetoothPrintService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        } else {
            if (data.length > 0)
                mPrintService.write(data);
        }
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

    public void printText(View v) {
        CheckBox emphasisChkBox = findViewById(R.id.checkBoxEmphasis);
        CheckBox underlineChkBox = findViewById(R.id.checkBoxUnderline);
        EditText editText = findViewById(R.id.editText);
        boolean emphasis = emphasisChkBox.isChecked();
        boolean underline = underlineChkBox.isChecked();
        String string = editText != null ? editText.getText().toString() : null;

        if (string != null && string.length() > 0) {
            sendData(WoosimCmd.initPrinter());
            sendData(WoosimCmd.setTextStyle(emphasis, underline, false, mCharExt, mCharExt));
            sendData(WoosimCmd.setTextAlign(mAlignment));
            sendData(string.getBytes(StandardCharsets.US_ASCII));
            sendData(WoosimCmd.printLineFeed(2));
        }
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
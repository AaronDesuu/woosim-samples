package com.woosim.sample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_READ = 3;
    public static final int PERMISSION_REQUEST = 100;

    public static final String DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBluetoothAdapter = null;
    protected static  BluetoothPrintService mPrintService = null;

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

        // Make a list for sample activities
        final ListView simpleListView= findViewById(R.id.simpleListView);
        SimpleAdapter adapter = new SimpleAdapter(this,
                getData(),
                android.R.layout.simple_list_item_1,
                new String[] { "title" },
                new int[] { android.R.id.text1 });
        if (simpleListView != null) {
            simpleListView.setAdapter(adapter);
            //perform listView item click event
            simpleListView.setOnItemClickListener((adapterView, view, position, l) -> {
                Map<String, Object> map = (Map<String, Object>) simpleListView.getItemAtPosition(position);
                Intent intent = (Intent) map.get("intent");
                startActivity(intent);
            });
        }
    }

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

    private ArrayList<HashMap<String, Object>> getData() {
        ArrayList<HashMap<String, Object>> appList = new ArrayList<>();
        addItem(appList, getString(R.string.title_sign_pad), new Intent(this, SignPad.class));
        addItem(appList, getString(R.string.title_multi_language), new Intent(this, MultiLanguage.class));
        addItem(appList, getString(R.string.title_example), new Intent(this, Example.class));
        return appList;
    }

    private void addItem(ArrayList<HashMap<String, Object>> list, String name, Intent intent) {
        HashMap<String, Object> temp = new HashMap<>();
        temp.put("title", name);
        temp.put("intent", intent);
        list.add(temp);
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
        }
        return true;
    });
}
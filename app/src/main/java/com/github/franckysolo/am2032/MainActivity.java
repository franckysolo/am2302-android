package com.github.franckysolo.am2032;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "fsp";
    // BLE HM11
    private static final UUID SERVICE_UUID =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final int REQUEST_ENABLE_BIT = 1;
    private static final long SCAN_PERIOD = 10000;
    private Handler mHandler;
    public DeviceListAdapter mDeviceListAdapter;
    private ListView mDeviceListView;
    public TextView deviceSignal;


    public TextView tv_temperature;
    public TextView tv_humidity;

    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner mLeScanner;
    public BluetoothGatt mGatt;
    public ScanSettings mSettings;
    public List<ScanFilter> mFilters;
    public boolean onScanning = false;
    public MenuItem mProgressItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "SDK version : " + Build.VERSION.SDK_INT);
        mHandler = new Handler();
        initBle();
        initViews();
    }

    @Override
    protected void onResume() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BIT);
        } else {
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            mFilters = new ArrayList<ScanFilter>();
            scanDevices(false);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanDevices(false);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_ENABLE_BIT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    private void initBle() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void initViews() {
        mDeviceListAdapter = new DeviceListAdapter(this);
        mDeviceListView = (ListView) findViewById(R.id.deviceList);
        mDeviceListView.setAdapter(mDeviceListAdapter);
        mDeviceListView.setOnItemClickListener(new DeviceListListener(this, mDeviceListAdapter));
        deviceSignal = (TextView) findViewById(R.id.rssi);
        tv_humidity = (TextView) findViewById(R.id.tv_humidity);
        tv_temperature = (TextView) findViewById(R.id.tv_temperature);
    }

    private void scanDevices(final boolean enable) {
        // ActionBar ab = getSupportActionBar();
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLeScanner.stopScan(mScanCallback);
                }

            }, SCAN_PERIOD);
            mLeScanner.startScan(mScanCallback);
        } else {
            mLeScanner.stopScan(mScanCallback);
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        Log.i(TAG, "Connexion au capteur");
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanDevices(false);

        }
    }

    public ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("Scan result", result.toString());
            final int rssi = result.getRssi();
            final BluetoothDevice device = result.getDevice();
            if (device != null && rssi != 0){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceListAdapter.addDevice(device, rssi);
                        mDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("Scan result", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i("Scan failed", String.valueOf(errorCode));
        }
    };

    private boolean registerCharacteristics(BluetoothGatt gatt) {
        final String name = gatt.getDevice().getName();
        BluetoothGattService service = gatt.getService(SERVICE_UUID);

        if (service == null) {
            Log.e(TAG, "Impossible d'accéder au service du capteur : " + name);
            return false;
        } else {
            Log.i(TAG, "Service du capteur accessible");

        }

        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(CHARACTERISTIC_UUID);

        if (characteristic == null) {
            Log.e(TAG, "Impossible de lire caractéristiques du capteur");
            return false;
        }

        //  On lit les caractéristiques du tracker
        gatt.setCharacteristicNotification(characteristic, true);
        gatt.readCharacteristic(characteristic);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(SERVICE_UUID);
        if (descriptor == null) {
            Log.e(TAG, "Impossible d'accéder au déscripteur de caractéristiques");
            return false;
        }

        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        gatt.writeDescriptor(descriptor);

        return true;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(TAG, "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e(TAG, "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                registerCharacteristics(gatt);
            } else {
                Log.d(TAG, "Impossible de se connecter à GATT");
            }
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, characteristic.toString());
            final byte[] bytes = characteristic.getValue();
            if (bytes != null && bytes.length > 0) {
                displayData(bytes);
            }
        }
    };

    private void displayData(final byte[] bytes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String stringTemplate = new String(bytes);
                Log.d(TAG, "Sensor DATA : " + stringTemplate);
                final String[] parts = stringTemplate.split(":");
                tv_humidity.setText(parts[0] + "%");
                tv_temperature.setText(parts[1].trim() + (char) 0x00B0 + "C");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!onScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceListAdapter.clear();
                        mDeviceListAdapter.notifyDataSetChanged();
                    }
                });
                scanDevices(true);
                break;
            case R.id.menu_stop:
                scanDevices(false);
                break;
        }
        return true;
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        if (!onScanning) {
//            menu.findItem(R.id.menu_stop).setVisible(false);
//            menu.findItem(R.id.menu_scan).setVisible(true);
//        } else {
//            menu.findItem(R.id.menu_stop).setVisible(true);
//            menu.findItem(R.id.menu_scan).setVisible(false);
//        }
//        return super.onPrepareOptionsMenu(menu);
//    }
}

package com.github.franckysolo.am2032;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

/**
 * Created by franckysolo on 24/12/16.
 */

public class DeviceListListener implements AdapterView.OnItemClickListener  {
    MainActivity mContext;
    DeviceListAdapter mAdapter;

    public DeviceListListener(MainActivity activity, DeviceListAdapter adapter) {
        mContext = activity;
        mAdapter = adapter;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // On click sur item de la liste des trackers
        final BluetoothDevice device = mContext.mDeviceListAdapter.getDevice(position);
        if (device == null) {
            Log.e(mContext.TAG, "L'appareil bluetooth a perdu la connexion");
            return;
        }
        // si Ble est en train de scanner
        if (mContext.onScanning) {
            mContext.mLeScanner.stopScan(mContext.mScanCallback);
            mContext.onScanning = false;
            // On coupe le scan
        }
        // Et on se connecte au tracker
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mContext.connectToDevice(device);
            }
        });
    }
}

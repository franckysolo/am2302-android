package com.github.franckysolo.am2032;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by franckysolo on 24/12/16.
 *
 * Listes des appareils Bluetooth détecté via BLE
 */

public class DeviceListAdapter extends BaseAdapter {

    private ArrayList<BluetoothDevice> mDeviceList;
    private ArrayList<Integer> mSignalList;
    private LayoutInflater mInflater;

    public DeviceListAdapter(MainActivity mainActivity) {
        super();
        mDeviceList = new ArrayList<BluetoothDevice>();
        mSignalList = new ArrayList<Integer>();
        mInflater = mainActivity.getLayoutInflater();
    }

    public void clear() {
        mDeviceList.clear();
    }

    public void addDevice(BluetoothDevice device, int rssi) {
        if (!mDeviceList.contains(device)) {
            mDeviceList.add(device);
            mSignalList.add(rssi);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_device, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView) convertView.findViewById(R.id.name);
            viewHolder.address = (TextView) convertView.findViewById(R.id.address);
            viewHolder.rssi = (TextView) convertView.findViewById(R.id.rssi);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice device = mDeviceList.get(position);
        final int signal = mSignalList.get(position);
        final String deviceSignal = String.valueOf(signal);

        if (viewHolder != null) {

            if (device.getName() == null) {
                viewHolder.name.setText(R.string.unknown_peripheral);
            } else {
                viewHolder.name.setText(device.getName());
            }

            if (device.getAddress() == null) {
                viewHolder.address.setText(R.string.unknown_address);
            } else {
                viewHolder.address.setText(device.getAddress());
            }

            if (deviceSignal == null) {
                viewHolder.rssi.setText(R.string.no_signal);
            } else {
                viewHolder.rssi.setText(deviceSignal);
            }

            if (signal >= -50) { // High quality
                viewHolder.icon.setColorFilter(Color.parseColor("#009900"));
            } else if (signal < -51 && signal >= -75) { // Medium quality
                viewHolder.icon.setColorFilter(Color.parseColor("#ff9900"));
            } else {
                if (signal < -75 && signal >= -85) { // Low quality
                    viewHolder.icon.setColorFilter(Color.parseColor("#990000"));
                } else {
                    viewHolder.icon.setColorFilter(Color.LTGRAY);
                }
            }
        }

        return convertView;
    }

    static class ViewHolder {
        TextView name;
        TextView address;
        TextView rssi;
        ImageView icon;
    }
}

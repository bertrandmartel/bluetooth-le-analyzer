/**
 * This file is part of RFdroid.
 * <p/>
 * Copyright (C) 2016  Bertrand Martel
 * <p/>
 * Foobar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.akinaru.rfdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.akinaru.rfdroid.R;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Bluetooth scanned devices
 *
 * @author Bertrand Martel
 */
public class ScanItemArrayAdapter extends ArrayAdapter<BluetoothObject> {

    /**
     * list of devices
     */
    List<BluetoothObject> scanningList = new ArrayList<>();

    private Context mContext;

    public ScanItemArrayAdapter(Context context, int textViewResourceId,
                                List<BluetoothObject> objects) {
        super(context, textViewResourceId, objects);

        this.mContext = context;
        this.scanningList = objects;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;

        try {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.listview_item, parent, false);
                holder = new ViewHolder();

                holder.deviceName = (TextView) convertView.findViewById(R.id.text1);
                holder.deviceAddress = (TextView) convertView.findViewById(R.id.text2);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.deviceAddress.setText(scanningList.get(position).getDeviceAddress());
            holder.deviceName.setText(scanningList.get(position).getDeviceName());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getCount() {
        return scanningList.size();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public static class ViewHolder {
        public TextView deviceAddress;
        public TextView deviceName;
    }

}
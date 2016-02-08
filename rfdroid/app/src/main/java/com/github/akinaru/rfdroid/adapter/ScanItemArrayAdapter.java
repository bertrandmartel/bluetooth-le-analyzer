/**
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2016 Bertrand Martel
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.akinaru.rfdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.akinaru.rfdroid.R;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bertrand Martel
 */
public class ScanItemArrayAdapter extends ArrayAdapter<BluetoothObject> {

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
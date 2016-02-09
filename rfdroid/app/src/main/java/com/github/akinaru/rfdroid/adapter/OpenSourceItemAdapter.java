package com.github.akinaru.rfdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.akinaru.rfdroid.R;

/**
 * Created by akinaru on 09/02/16.
 */
public class OpenSourceItemAdapter extends BaseAdapter {

    private static final String[][] COMPONENTS = new String[][]{
            
            {"DiscreteSeekBar", "https://github.com/AnderWeb/discreteSeekBar"},
            {"MPAndroidChart",
                    "https://github.com/PhilJay/MPAndroidChart"},
            {"nv-bluetooth", "https://github.com/TakahikoKawasaki/nv-bluetooth"},
            {"RFDuino", "https://github.com/RFduino/RFduino"},
            {"rfduino-makefile", "https://github.com/akinaru/rfduino-makefile"},
            {"Material Design Icons", "https://github.com/google/material-design-icons"}
    };

    private LayoutInflater mInflater;

    public OpenSourceItemAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return COMPONENTS.length;
    }

    @Override
    public Object getItem(int position) {
        return COMPONENTS[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.open_source_items, parent, false);
        }

        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView url = (TextView) convertView.findViewById(R.id.url);

        title.setText(COMPONENTS[position][0]);
        url.setText(COMPONENTS[position][1]);

        return convertView;
    }
}
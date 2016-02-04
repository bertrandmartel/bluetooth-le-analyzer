package com.github.akinaru.rfdroid.chart;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Created by akinaru on 02/02/16.
 */
public class ReceptionRateValueFormatter implements ValueFormatter {

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return value + "%";
    }

}

package com.github.akinaru.rfdroid;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

public class MyYAxisValueFormatter implements YAxisValueFormatter {

    @Override
    public String getFormattedValue(float value, YAxis yAxis) {
        return String.format("%d", (long) value) + " %";
    }
}

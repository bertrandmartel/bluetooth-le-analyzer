package com.github.akinaru.rfdroid;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

public class DataAxisFormatter implements YAxisValueFormatter {

    private String format = "";

    public DataAxisFormatter(String format) {
        this.format = format;
    }

    @Override
    public String getFormattedValue(float value, YAxis yAxis) {
        return String.format("%d", (long) value) + " " + format;
    }
}

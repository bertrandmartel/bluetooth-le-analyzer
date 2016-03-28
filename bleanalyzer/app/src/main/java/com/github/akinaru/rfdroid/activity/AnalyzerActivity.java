/****************************************************************************
 * This file is part of Bluetooth LE Analyzer.                              *
 * <p/>                                                                     *
 * Copyright (C) 2016  Bertrand Martel                                      *
 * <p/>                                                                     *
 * Foobar is free software: you can redistribute it and/or modify           *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation, either version 3 of the License, or        *
 * (at your option) any later version.                                      *
 * <p/>                                                                     *
 * Foobar is distributed in the hope that it will be useful,                *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * <p/>                                                                     *
 * You should have received a copy of the GNU General Public License        *
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.          *
 */
package com.github.akinaru.rfdroid.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.akinaru.rfdroid.R;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;
import com.github.akinaru.rfdroid.chart.DataAxisFormatter;
import com.github.akinaru.rfdroid.inter.IADListener;
import com.github.akinaru.rfdroid.inter.IBtActivity;
import com.github.akinaru.rfdroid.inter.IScheduledMeasureListener;
import com.github.akinaru.rfdroid.service.BtAnalyzerService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Analyzer activity
 *
 * @author Bertrand Martel
 */
public class AnalyzerActivity extends BaseActivity implements IADListener, IScheduledMeasureListener, IBtActivity {

    private String TAG = this.getClass().getName();

    /**
     * bar chart object
     */
    private BarChart mChart;

    /**
     * description item table
     */
    private TableLayout tablelayout;

    /**
     * bluetooth device selected
     */
    private BluetoothObject btDevice = null;

    /**
     * last packet received textview
     */
    private TextView lastPacketReceivedTv = null;

    /**
     * sampling time textview
     */
    private TextView samplingTimeTv = null;

    /**
     * total packet received textview
     */
    private TextView totalPacketReceiveTv = null;

    /**
     * average packet received textview
     */
    private TextView averagePacketReceivedTv = null;

    /**
     * date format for timestamp
     */
    private SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * device name textview
     */
    private TextView deviceNameTv = null;

    /**
     * device address textview
     */
    private TextView deviceAddressTv = null;

    /**
     * scan icon
     */
    private ImageButton scanImage;
    private ProgressBar progressBar;

    protected void onCreate(Bundle savedInstanceState) {

        setLayout(R.layout.activity_analyzer);
        super.onCreate(savedInstanceState);

        //set device address to analyze
        String deviceAddr = getIntent().getExtras().getString("deviceAddress");
        String deviceName = getIntent().getExtras().getString("deviceName");
        int adInterval = getIntent().getExtras().getInt("advertizingInterval");
        btDevice = new BluetoothObject(deviceAddr, deviceName, adInterval);

        //set description item table
        tablelayout = (TableLayout) findViewById(R.id.tablelayout);
        altTableRow(2);

        //initialize chart
        initChart();

        //setup textviews
        deviceNameTv = (TextView) findViewById(R.id.device_name);
        deviceAddressTv = (TextView) findViewById(R.id.device_address);
        lastPacketReceivedTv = (TextView) findViewById(R.id.last_packet_received_value);
        samplingTimeTv = (TextView) findViewById(R.id.sampling_time_value);
        totalPacketReceiveTv = (TextView) findViewById(R.id.total_packet_received_value);
        averagePacketReceivedTv = (TextView) findViewById(R.id.average_packet_received_value);

        //setup default values
        initTv();
        deviceNameTv.setText(deviceName);
        deviceAddressTv.setText(deviceAddr);

        //bind to service
        if (mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(this, BtAnalyzerService.class);
            mBound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    /**
     * initiliaze chart
     */
    private void initChart() {
        mChart = (BarChart) findViewById(R.id.chart1);
        mChart.setDrawBarShadow(false);
        mChart.setDrawValueAboveBar(true);
        mChart.setDescription("");
        // if more than 60 entries are displayed in the chart, no values will be
        // drawn
        mChart.setMaxVisibleValueCount(60);
        // scaling can now only be done on x- and y-axis separately
        mChart.setPinchZoom(false);
        mChart.setDrawGridBackground(false);
        mChart.setDescriptionColor(Color.parseColor("#000000"));

        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setSpaceBetweenLabels(0);

        YAxisValueFormatter custom = new DataAxisFormatter("%");

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setValueFormatter(custom);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setValueFormatter(custom);
        rightAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);

        leftAxis.setDrawGridLines(true);
        rightAxis.setDrawGridLines(false);

        mChart.animateY(1000);

        mChart.getLegend().setEnabled(true);

        mChart.setVisibility(View.GONE);
    }

    /**
     * initialize textviews
     */
    private void initTv() {
        lastPacketReceivedTv.setText("-");
        samplingTimeTv.setText("-");
        averagePacketReceivedTv.setText("-");
        totalPacketReceiveTv.setText("-");
    }

    /**
     * alternate colors for description rows
     *
     * @param alt_row
     */
    public void altTableRow(int alt_row) {
        int childViewCount = tablelayout.getChildCount();

        for (int i = 0; i < childViewCount; i++) {
            TableRow row = (TableRow) tablelayout.getChildAt(i);

            for (int j = 0; j < row.getChildCount(); j++) {

                TextView tv = (TextView) row.getChildAt(j);
                if (i % alt_row != 0) {
                    tv.setBackground(getResources().getDrawable(
                            R.drawable.alt_row_color));
                } else {
                    tv.setBackground(getResources().getDrawable(
                            R.drawable.row_color));
                }
            }
        }
    }

    /**
     * update data for visualization chart
     *
     * @param valueList list of values for the chart
     * @param unit      data unit
     */
    private void setData(List<Integer> valueList, String unit) {

        mChart.setVisibility(View.VISIBLE);

        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<BarEntry> yVals1 = new ArrayList<BarEntry>();

        for (int i = 0; i < valueList.size(); i++) {
            xVals.add(i + "s");
            yVals1.add(new BarEntry(valueList.get(i), i));
        }

        String legend = getResources().getString(R.string.caption_receptin_rate);

        if (!unit.equals("%"))
            legend = getResources().getString(R.string.caption_packet_count);

        BarDataSet set1 = new BarDataSet(yVals1, legend);
        set1.setBarSpacePercent(35f);
        set1.setColor(Color.parseColor("#0288D1"));

        ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        dataSets.add(set1);

        BarData data = new BarData(xVals, dataSets);
        data.setValueTextSize(10f);

        data.setDrawValues(false);

        YAxisValueFormatter custom = new DataAxisFormatter(unit);
        YAxis leftAxis = mChart.getAxisLeft();
        YAxis rightAxis = mChart.getAxisRight();

        leftAxis.setValueFormatter(custom);
        rightAxis.setValueFormatter(custom);

        mChart.setData(data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        try {
            if (mBound) {
                unbindService(mServiceConnection);
                mBound = false;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mService != null) {
            if (mService.isScanning()) {
                hideProgressBar();
                mService.stopScan();
                nvDrawer.getMenu().findItem(R.id.scan_btn_nv).setIcon(R.drawable.ic_looks);
                nvDrawer.getMenu().findItem(R.id.scan_btn_nv).setTitle(getResources().getString(R.string.menu_title_start_scan));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {

            if (mBluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(this, BtAnalyzerService.class);
                // bind the service to current activity and create it if it didnt exist before
                startService(intent);
                mBound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

            } else {
                Toast.makeText(this, getResources().getString(R.string.toast_bluetooth_disabled), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Manage Bluetooth Service
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.v(TAG, "connected to service");

            mService = ((BtAnalyzerService.LocalBinder) service).getService();
            mService.setSelectionningDevice(true);
            mService.setADListener(AnalyzerActivity.this);
            mService.setScheduledMeasureListener(AnalyzerActivity.this);
            mService.setBtDevice(btDevice);
            triggerNewScan();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    public void onADframeReceived(final long time, final List<Long> history) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lastPacketReceivedTv.setText(sf.format(time));
                totalPacketReceiveTv.setText("" + history.size());
            }
        });
    }

    @Override
    public void onNewMeasure(final long samplingTime,
                             final int finalPacketReceptionRate,
                             final List<Integer> globalSumPerSecond,
                             final List<Integer> globalPacketReceivedPerSecond,
                             final float averagePacket) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                samplingTimeTv.setText(samplingTime + "s");
                setData(globalPacketReceivedPerSecond, "");
                mChart.invalidate();
                averagePacketReceivedTv.setText("" + String.format("%.2f", averagePacket) + getResources().getString(R.string.addition_average_packet));
            }
        });
    }

    @Override
    public void onMeasureClear() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initTv();
                mChart.setVisibility(View.GONE);
            }
        });
    }
}

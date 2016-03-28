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

import android.app.ProgressDialog;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.akinaru.rfdroid.R;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothEvents;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;
import com.github.akinaru.rfdroid.bluetooth.listener.IPushListener;
import com.github.akinaru.rfdroid.bluetooth.rfduino.IRfduinoDevice;
import com.github.akinaru.rfdroid.chart.DataAxisFormatter;
import com.github.akinaru.rfdroid.chart.DataChartType;
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

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Rfdroid device activity
 *
 * @author Bertrand Martel
 */
public class RFdroidActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener, IADListener, IScheduledMeasureListener, IBtActivity {

    /**
     * debug tag
     */
    private String TAG = this.getClass().getName();

    private ProgressDialog dialog = null;

    private ProgressDialog deviceFoundDialog = null;

    private int adInterval = 20;

    protected BarChart mChart;

    private DiscreteSeekBar discreteSeekBar;

    private TableLayout tablelayout;

    private BluetoothObject btDevice = null;

    private TextView intervalTv = null;
    private TextView globalReceptionRateTv = null;
    private TextView lastPacketReceivedTv = null;
    private TextView samplingTimeTv = null;
    private TextView totalPacketReceiveTv = null;
    private TextView averagePacketReceivedTv = null;

    private SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss.SSS");

    private DataChartType dataChartType = DataChartType.RECEPTION_RATE;

    private SharedPreferences sharedpreferences;

    private final static String PREFERENCES = "storage";

    private boolean openingDrawer = false;

    protected void onCreate(Bundle savedInstanceState) {

        setLayout(R.layout.activity_rfdroid);
        super.onCreate(savedInstanceState);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);

        deviceFoundDialog = ProgressDialog.show(RFdroidActivity.this, "", getResources().getString(R.string.toast_looking_for_rfdroid), true);
        deviceFoundDialog.setCancelable(false);
        deviceFoundDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    onBackPressed();
                    return true;
                }
                return false;
            }
        });

        final ScrollView view = (ScrollView) findViewById(R.id.scrollview);
        view.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (!openingDrawer) {
                    if (view.getScrollX() == 0 && view.getScrollY() == 0) {
                        discreteSeekBar.showFloater(250);
                    } else {
                        discreteSeekBar.hideFloater(1);
                    }
                }
            }
        });

        sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        int dataChartTypeVal = sharedpreferences.getInt("dataChartType", DataChartType.RECEPTION_RATE.ordinal());

        if (dataChartTypeVal == DataChartType.PACKET_NUMBER.ordinal()) {
            dataChartType = DataChartType.PACKET_NUMBER;
        }

        tablelayout = (TableLayout) findViewById(R.id.tablelayout);

        altTableRow(2);

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

        discreteSeekBar = (DiscreteSeekBar) findViewById(R.id.discrete1);
        discreteSeekBar.keepShowingPopup(true);

        discreteSeekBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
            @Override
            public int transform(int value) {
                return value * 5;
            }
        });

        discreteSeekBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    discreteSeekBar.showFloater(250);
                }
            }
        });

        discreteSeekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                setAdvertisingInteval();
            }
        });

        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {

            discreteSeekBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setAdvertisingInteval();
                }
            });
        }

        intervalTv = (TextView) findViewById(R.id.interval_head);
        globalReceptionRateTv = (TextView) findViewById(R.id.global_reception_rate);
        lastPacketReceivedTv = (TextView) findViewById(R.id.last_packet_received_value);
        samplingTimeTv = (TextView) findViewById(R.id.sampling_time_value);
        totalPacketReceiveTv = (TextView) findViewById(R.id.total_packet_received_value);
        averagePacketReceivedTv = (TextView) findViewById(R.id.average_packet_received_value);

        initTv();

        if (mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(this, BtAnalyzerService.class);
            mBound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    private void setAdvertisingInteval() {

        adInterval = discreteSeekBar.getProgress() * 5;

        Log.v(TAG, "setting AD interval to " + adInterval + "ms");

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (btDevice != null && !btDevice.getDeviceAddress().equals("")) {

                    final String deviceAddress = btDevice.getDeviceAddress();

                    if (mService.isScanning()) {

                        scanMenuItem.setIcon(R.drawable.ic_bluetooth);
                        mService.stopScan();
                    }

                    if (!mService.getConnectionList().containsKey(deviceAddress) ||
                            !mService.getConnectionList().get(deviceAddress).isConnected()) {

                        dialog = ProgressDialog.show(RFdroidActivity.this, "", getResources().getString(R.string.connection_to_rfdroid), true);
                        mService.connect(deviceAddress);

                    } else {

                        if (!mService.getConnectionList().get(deviceAddress).isConnected()) {
                            mService.connect(deviceAddress);
                        } else {

                            if (mService.getConnectionList().get(deviceAddress).getDevice() instanceof IRfduinoDevice) {

                                IRfduinoDevice device = (IRfduinoDevice) mService.getConnectionList().get(deviceAddress).getDevice();

                                device.setAdvertisingInterval(adInterval, new IPushListener() {
                                    @Override
                                    public void onPushFailure() {
                                        Log.v(TAG, "onPushFailure");
                                        mService.disconnect(deviceAddress);
                                        refreshViewOnError();
                                    }

                                    @Override
                                    public void onPushSuccess() {
                                        Log.v(TAG, "onPushSuccess");
                                        mService.disconnect(deviceAddress);
                                        refreshViewOnSucccess();
                                    }
                                });
                            }
                        }
                    }

                }
            }
        });
    }

    private void initTv() {
        intervalTv.setText("-");
        globalReceptionRateTv.setText("-");
        lastPacketReceivedTv.setText("-");
        samplingTimeTv.setText("-");
        averagePacketReceivedTv.setText("-");
        totalPacketReceiveTv.setText("-");
    }

    private void switchChartData(MenuItem menuItem) {

        if (dataChartType == DataChartType.RECEPTION_RATE) {
            dataChartType = DataChartType.PACKET_NUMBER;
            menuItem.setTitle(getResources().getString(R.string.menu_item_title_reception_rate));
            setData(mService.getGlobalPacketReceivedPerSecond(), "");
            mChart.invalidate();
        } else {
            dataChartType = DataChartType.RECEPTION_RATE;
            menuItem.setTitle(getResources().getString(R.string.menu_item_title_packet_count));
            setData(mService.getGlobalSumPerSecond(), "%");
            mChart.invalidate();
        }
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt("dataChartType", dataChartType.ordinal());
        editor.commit();
    }

    private void closeDialog() {
        if (dialog != null) {
            dialog.cancel();
            dialog = null;
        }
    }

    protected ActionBarDrawerToggle setupDrawerToggle() {
        super.setupDrawerToggle();
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);

                if (slideOffset == 0) {
                    openingDrawer = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            discreteSeekBar.showFloater(250);
                        }
                    });
                } else if (slideOffset == 1) {
                    openingDrawer = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            discreteSeekBar.hideFloater(1);
                        }
                    });
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
    }

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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        mChart.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void setData(List<Integer> valueList, String format) {

        mChart.setVisibility(View.VISIBLE);

        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<BarEntry> yVals1 = new ArrayList<BarEntry>();

        if (valueList != null) {
            for (int i = 0; i < valueList.size(); i++) {
                xVals.add(i + "s");
                yVals1.add(new BarEntry(valueList.get(i), i));
            }
        }

        String legend = getResources().getString(R.string.caption_receptin_rate);

        if (!format.equals("%"))
            legend = getResources().getString(R.string.caption_packet_count);

        BarDataSet set1 = new BarDataSet(yVals1, legend);
        set1.setBarSpacePercent(35f);
        set1.setColor(Color.parseColor("#0288D1"));

        ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        dataSets.add(set1);

        BarData data = new BarData(xVals, dataSets);
        data.setValueTextSize(10f);

        data.setDrawValues(false);

        YAxisValueFormatter custom = new DataAxisFormatter(format);
        YAxis leftAxis = mChart.getAxisLeft();
        YAxis rightAxis = mChart.getAxisRight();

        leftAxis.setValueFormatter(custom);
        rightAxis.setValueFormatter(custom);

        mChart.setData(data);
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
     * trigger a BLE scan
     */
    public void triggerNewScan() {

        if (mService != null && !mService.isScanning()) {

            if (scanImage != null)
                scanImage.setVisibility(View.GONE);
            if (progressBar != null)
                progressBar.setVisibility(View.VISIBLE);

            Log.v(TAG, "start scan");
            mService.disconnectall();
            mService.startScan();

        } else {
            Toast.makeText(RFdroidActivity.this, getResources().getString(R.string.toast_already_scanning), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        //currentService.disconnect(deviceAddress);
        unregisterReceiver(mGattUpdateReceiver);

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
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mService != null) {
            mService.disconnectall();

            if (mService.isScanning()) {
                progressBar.setVisibility(View.GONE);
                scanImage.setVisibility(View.VISIBLE);
                mService.stopScan();
            }
        }
        closeDialog();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (BluetoothEvents.BT_EVENT_SCAN_START.equals(action)) {
                Log.v(TAG, "Scan has started");
            } else if (BluetoothEvents.BT_EVENT_SCAN_END.equals(action)) {
                Log.v(TAG, "Scan has ended");
            } else if (BluetoothEvents.BT_EVENT_DEVICE_DISCOVERED.equals(action)) {
                Log.v(TAG, "New device has been discovered");

                BluetoothObject btDeviceTmp = BluetoothObject.parseArrayList(intent);

                if (btDeviceTmp != null &&
                        btDeviceTmp.getAdvertizingInterval() != -1 &&
                        discreteSeekBar != null &&
                        intervalTv != null &&
                        globalReceptionRateTv != null) {

                    btDevice = btDeviceTmp;

                    if (deviceFoundDialog != null)
                        deviceFoundDialog.dismiss();

                    adInterval = btDevice.getAdvertizingInterval();
                    discreteSeekBar.setProgress(btDevice.getAdvertizingInterval() / 5);
                    intervalTv.setText(getResources().getString(R.string.interval) + " - " + btDevice.getAdvertizingInterval() + "ms");
                    globalReceptionRateTv.setText("0%");
                }

            } else if (BluetoothEvents.BT_EVENT_DEVICE_DISCONNECTED.equals(action)) {

                Log.v(TAG, "Device disconnected");

                BluetoothObject btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null) {
                    closeDialog();
                }

            } else if (BluetoothEvents.BT_EVENT_DEVICE_CONNECTED.equals(action)) {

                Log.v(TAG, "device connected");

                BluetoothObject btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null && !btDevice.getDeviceAddress().equals("")) {

                    final String deviceAddress = btDevice.getDeviceAddress();

                    if (mService.getConnectionList().get(deviceAddress).getDevice() instanceof IRfduinoDevice) {

                        IRfduinoDevice device = (IRfduinoDevice) mService.getConnectionList().get(deviceAddress).getDevice();

                        device.setAdvertisingInterval(adInterval, new IPushListener() {
                            @Override
                            public void onPushFailure() {
                                Log.v(TAG, "onPushFailure");
                                mService.disconnect(deviceAddress);
                                refreshViewOnError();
                            }

                            @Override
                            public void onPushSuccess() {
                                Log.v(TAG, "onPushSuccess");
                                mService.disconnect(deviceAddress);
                                refreshViewOnSucccess();
                            }
                        });
                    }
                }
            }
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.v(TAG, "focus : " + hasFocus);
        if (hasFocus) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    discreteSeekBar.showFloater(250);
                }
            });
        } else {
            discreteSeekBar.hideFloater(250);
        }
    }

    private void refreshViewOnError() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                closeDialog();
                triggerNewScan();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        discreteSeekBar.showFloater(250);
                    }
                });
            }
        });
    }

    private void refreshViewOnSucccess() {

        Log.v(TAG, "refreshViewOnSucccess");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                closeDialog();
                triggerNewScan();

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        discreteSeekBar.showFloater(250);
                    }
                });
            }
        });
    }

    /**
     * Manage Bluetooth Service lifecycle
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.v(TAG, "Connected to service");

            mService = ((BtAnalyzerService.LocalBinder) service).getService();
            mService.clearScanningList();
            mService.setADListener(RFdroidActivity.this);
            mService.setScheduledMeasureListener(RFdroidActivity.this);
            triggerNewScan();

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    discreteSeekBar.hideFloater(1);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    /**
     * add filter to intent to receive notification from bluetooth service
     *
     * @return intent filter
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothEvents.BT_EVENT_SCAN_START);
        intentFilter.addAction(BluetoothEvents.BT_EVENT_SCAN_END);
        intentFilter.addAction(BluetoothEvents.BT_EVENT_DEVICE_DISCOVERED);
        intentFilter.addAction(BluetoothEvents.BT_EVENT_DEVICE_CONNECTED);
        intentFilter.addAction(BluetoothEvents.BT_EVENT_DEVICE_DISCONNECTED);
        return intentFilter;
    }

    @Override
    public void onADframeReceived(final long time, final List<Long> history) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lastPacketReceivedTv.setText(sf.format(time));
                if (btDevice != null && history.size() > 0) {
                    totalPacketReceiveTv.setText("" + history.size() + " / " + ((time - history.get(0)) / btDevice.getAdvertizingInterval()));
                } else {
                    totalPacketReceiveTv.setText("" + history.size());
                }
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
                globalReceptionRateTv.setText(finalPacketReceptionRate + "%");
                if (dataChartType == DataChartType.RECEPTION_RATE)
                    setData(globalSumPerSecond, "%");
                else
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
                if (RFdroidActivity.this.btDevice != null && mService != null && mService.getBtDevice() != null) {
                    mService.getBtDevice().setAdvertizingInterval(adInterval);
                    RFdroidActivity.this.btDevice.setAdvertizingInterval(adInterval);
                    discreteSeekBar.setProgress(RFdroidActivity.this.btDevice.getAdvertizingInterval() / 5);
                    intervalTv.setText(getResources().getString(R.string.interval) + " - " + RFdroidActivity.this.btDevice.getAdvertizingInterval() + "ms");
                    globalReceptionRateTv.setText("0%");
                }
            }
        });
    }
}
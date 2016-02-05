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
package com.github.akinaru.rfdroid;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.akinaru.rfdroid.bluetooth.events.BluetoothEvents;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;
import com.github.akinaru.rfdroid.bluetooth.listener.IPushListener;
import com.github.akinaru.rfdroid.bluetooth.rfduino.IRfduinoDevice;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dotti device management main activity
 *
 * @author Bertrand Martel
 */
public class RFdroidActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, IADListener {

    /**
     * debug tag
     */
    private String TAG = this.getClass().getName();

    private ProgressDialog dialog = null;

    private boolean toSecondLevel = false;

    private boolean bound = false;

    private int adInterval = 20;

    /**
     * define if bluetooth is enabled on device
     */
    private final static int REQUEST_ENABLE_BT = 1;

    /**
     * Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * current index of connecting device item in device list
     */
    private int list_item_position = 0;

    private RFdroidService currentService = null;

    protected BarChart mChart;

    protected String[] mMonths = new String[]{
            "1s", "2s", "3s", "4s", "5s", "6s", "7s", "8s", "9s", "10s", "11s", "12s"
    };

    private Toolbar toolbar = null;
    private DrawerLayout mDrawer = null;

    private GestureDetector mGestureDetector;

    private ActionBarDrawerToggle drawerToggle;

    private ProgressBar progress_bar;

    private NavigationView nvDrawer;

    private DiscreteSeekBar discreteSeekBar;

    private ScheduledExecutorService scheduler;

    private TableLayout tablelayout;

    private BluetoothObject btDevice = null;

    private TextView intervalTv = null;
    private TextView globalReceptionRateTv = null;
    private TextView lastPacketReceivedTv = null;
    private TextView samplingTimeTv = null;
    private TextView totalPacketReceiveTv = null;
    private TextView averagePacketReceivedTv = null;
    private List<Long> history = new ArrayList<>();
    private List<Integer> globalSumPerSecond = new ArrayList<>();

    private SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss.SSS");

    private ScheduledFuture<?> measurementTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfdroid);

        tablelayout = (TableLayout) findViewById(R.id.tablelayout);

        altTableRow(2);

        scheduler = Executors.newScheduledThreadPool(1);

        //Button button_stop_scanning = (Button) findViewById(R.id.stop_scanning_button);
        progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar_item);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("RFdroid - reception rate");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();

        mDrawer.setDrawerListener(drawerToggle);

        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        // Setup drawer view
        setupDrawerContent(nvDrawer);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Smart is not supported on your device", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (progress_bar != null) {
            progress_bar.setEnabled(false);
            progress_bar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

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

        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setSpaceBetweenLabels(0);
        mChart.getAxisLeft().setDrawGridLines(false);

        YAxisValueFormatter custom = new MyYAxisValueFormatter();

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setLabelCount(8, false);
        leftAxis.setValueFormatter(custom);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setLabelCount(8, false);
        rightAxis.setValueFormatter(custom);
        rightAxis.setSpaceTop(15f);

        mChart.animateY(1000);

        mChart.getLegend().setEnabled(false);

        /*
        mChart.getAxisLeft().setDrawLabels(true);
        mChart.getAxisRight().setDrawLabels(true);
        //mChart.getXAxis().setDrawLabels(true);
        mChart.getLegend().setEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setDrawGridBackground(false);

        mChart.setClickable(false);
        mChart.setDoubleTapToZoomEnabled(false);
        mChart.setPinchZoom(false);
        mChart.getXAxis().setEnabled(true);
        mChart.invalidate();
        */

        discreteSeekBar = (DiscreteSeekBar) findViewById(R.id.discrete1);
        discreteSeekBar.keepShowingPopup(true);

        discreteSeekBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
            @Override
            public int transform(int value) {
                return value * 5;
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

                if (measurementTask != null) {
                    measurementTask.cancel(true);
                    measurementTask = null;
                }
                adInterval = seekBar.getProgress() * 5;

                Log.i(TAG, "setting AD interval to " + adInterval + "ms");

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        if (btDevice != null && !btDevice.getDeviceAddress().equals("")) {

                            final String deviceAddress = btDevice.getDeviceAddress();

                            if (currentService.isScanning()) {

                                currentService.stopScan();

                                if (progress_bar != null) {
                                    progress_bar.setEnabled(false);
                                    progress_bar.setVisibility(View.GONE);
                                }
                            }

                            if (!currentService.getConnectionList().containsKey(deviceAddress) ||
                                    !currentService.getConnectionList().get(deviceAddress).isConnected()) {

                                dialog = ProgressDialog.show(RFdroidActivity.this, "", "Connecting ...", true);

                                currentService.connect(deviceAddress);

                            } else {

                                if (!currentService.getConnectionList().get(deviceAddress).isConnected()) {

                                    currentService.connect(deviceAddress);

                                } else {

                                    if (currentService.getConnectionList().get(deviceAddress).getDevice() instanceof IRfduinoDevice) {

                                        IRfduinoDevice device = (IRfduinoDevice) currentService.getConnectionList().get(deviceAddress).getDevice();

                                        device.setAdvertisingInterval(adInterval, new IPushListener() {
                                            @Override
                                            public void onPushFailure() {
                                                Log.i(TAG, "onPushFailure");
                                                currentService.disconnect(deviceAddress);
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        closeDialog();
                                                        triggerNewScan();
                                                        scheduler.schedule(new Runnable() {

                                                            @Override
                                                            public void run() {
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        discreteSeekBar.showFloater(250);
                                                                    }
                                                                });

                                                            }
                                                        }, 500, TimeUnit.MILLISECONDS);
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onPushSuccess() {
                                                Log.i(TAG, "onPushSuccess");
                                                currentService.disconnect(deviceAddress);
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        closeDialog();
                                                        triggerNewScan();
                                                        scheduler.schedule(new Runnable() {

                                                            @Override
                                                            public void run() {
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        discreteSeekBar.showFloater(250);
                                                                    }
                                                                });

                                                            }
                                                        }, 500, TimeUnit.MILLISECONDS);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            }

                        }
                    }
                });
            }
        });

        intervalTv = (TextView) findViewById(R.id.interval_head);
        globalReceptionRateTv = (TextView) findViewById(R.id.global_reception_rate);
        lastPacketReceivedTv = (TextView) findViewById(R.id.last_packet_received_value);
        samplingTimeTv = (TextView) findViewById(R.id.sampling_time_value);
        totalPacketReceiveTv = (TextView) findViewById(R.id.total_packet_received_value);
        averagePacketReceivedTv = (TextView) findViewById(R.id.average_packet_received_value);

        intervalTv.setText("-");
        globalReceptionRateTv.setText("-");
        lastPacketReceivedTv.setText("-");
        samplingTimeTv.setText("-");
        averagePacketReceivedTv.setText("-");
        totalPacketReceiveTv.setText("-");
    }

    private void setupDrawerContent(NavigationView navigationView) {

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.scan_status:
                changeScanStatus(menuItem);
                break;
            case R.id.interval_max: {
                displayChangeIntervalMaxDialog();
                break;
            }
        }
        mDrawer.closeDrawers();
    }

    private void changeScanStatus(MenuItem menuItem) {

        if (currentService != null && currentService.isScanning()) {

            if (measurementTask != null) {
                measurementTask.cancel(true);
                measurementTask = null;
            }
            currentService.stopScan();

            if (progress_bar != null) {
                progress_bar.setEnabled(false);
                progress_bar.setVisibility(View.GONE);
            }

            menuItem.setTitle("Start scanning");

        } else {

            triggerNewScan();

            if (progress_bar != null) {
                progress_bar.setEnabled(true);
                progress_bar.setVisibility(View.VISIBLE);
            }

            menuItem.setTitle("Stop scanning");
        }
    }

    private void displayChangeIntervalMaxDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChangeMaxIntervalDialog cdd = new ChangeMaxIntervalDialog(RFdroidActivity.this);
                cdd.show();
            }
        });
    }

    private void closeDialog() {
        if (dialog != null) {
            dialog.cancel();
            dialog = null;
        }

        discreteSeekBar.showFloater(250);
    }

    private ActionBarDrawerToggle setupDrawerToggle() {

        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if (slideOffset > 0)
                    discreteSeekBar.hideFloater(1);
                else
                    discreteSeekBar.showFloater(250);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Make sure this is the method with just `Bundle` as the signature
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
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

    private void setData(List<Integer> valueList) {

        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<BarEntry> yVals1 = new ArrayList<BarEntry>();

        for (int i = 0; i < valueList.size(); i++) {
            xVals.add(i + "s");
            yVals1.add(new BarEntry(valueList.get(i), i));
        }

        BarDataSet set1 = new BarDataSet(yVals1, "history");
        set1.setBarSpacePercent(35f);
        set1.setColor(Color.parseColor("#0288D1"));

        ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        dataSets.add(set1);

        BarData data = new BarData(xVals, dataSets);
        data.setValueTextSize(10f);

        data.setDrawValues(false);

        mChart.setData(data);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {

            if (mBluetoothAdapter.isEnabled()) {


                Intent intent = new Intent(this, RFdroidService.class);

                // bind the service to current activity and create it if it didnt exist before
                startService(intent);
                bound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

            } else {

                Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * trigger a BLE scan
     */
    public void triggerNewScan() {

        if (progress_bar != null) {

            if (currentService != null && !currentService.isScanning()) {

                if (progress_bar != null) {
                    progress_bar.setEnabled(true);
                    progress_bar.setVisibility(View.VISIBLE);
                }

                Log.i(TAG, "START SCAN");

                currentService.disconnectall();
                currentService.startScan();

            } else {
                Toast.makeText(RFdroidActivity.this, "Scanning already engaged...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "RFdroidActivity onDestroy");
        //currentService.disconnect(deviceAddress);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        toSecondLevel = false;

        if (mBluetoothAdapter.isEnabled()) {

            Intent intent = new Intent(this, RFdroidService.class);

            // bind the service to current activity and create it if it didnt exist before
            //startService(intent);
            bound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        }

        scheduler.schedule(new Runnable() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        discreteSeekBar.showFloater(250);
                    }
                });

            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!toSecondLevel) {

            if (currentService != null) {
                currentService.disconnectall();
            }
        }

        if (currentService != null) {
            if (currentService.isScanning())
                currentService.stopScan();
        }

        try {
            if (bound) {
                unbindService(mServiceConnection);
                bound = false;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        closeDialog();
        discreteSeekBar.hideFloater(1);

        if (measurementTask != null) {
            measurementTask.cancel(true);
            measurementTask = null;
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (BluetoothEvents.BT_EVENT_SCAN_START.equals(action)) {
                Log.i(TAG, "Scan has started");
            } else if (BluetoothEvents.BT_EVENT_SCAN_END.equals(action)) {
                Log.i(TAG, "Scan has ended");
            } else if (BluetoothEvents.BT_EVENT_DEVICE_DISCOVERED.equals(action)) {

                Log.i(TAG, "New device has been discovered");

                btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null &&
                        btDevice.getAdvertizingInterval() != -1 &&
                        discreteSeekBar != null &&
                        intervalTv != null &&
                        globalReceptionRateTv != null) {

                    discreteSeekBar.setProgress(btDevice.getAdvertizingInterval() / 5);
                    intervalTv.setText("Interval - " + btDevice.getAdvertizingInterval() + "ms");
                    globalReceptionRateTv.setText("0%");
                }

            } else if (BluetoothEvents.BT_EVENT_DEVICE_DISCONNECTED.equals(action)) {

                Log.i(TAG, "Device disconnected");

                BluetoothObject btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null) {

                    closeDialog();

                }

            } else if (BluetoothEvents.BT_EVENT_DEVICE_CONNECTED.equals(action)) {

                Log.i(TAG, "device connected");

                BluetoothObject btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null && !btDevice.getDeviceAddress().equals("")) {

                    final String deviceAddress = btDevice.getDeviceAddress();

                    if (currentService.getConnectionList().get(deviceAddress).getDevice() instanceof IRfduinoDevice) {

                        IRfduinoDevice device = (IRfduinoDevice) currentService.getConnectionList().get(deviceAddress).getDevice();

                        device.setAdvertisingInterval(adInterval, new IPushListener() {
                            @Override
                            public void onPushFailure() {
                                Log.i(TAG, "onPushFailure");
                                currentService.disconnect(deviceAddress);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        closeDialog();
                                        triggerNewScan();
                                        scheduler.schedule(new Runnable() {

                                            @Override
                                            public void run() {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        discreteSeekBar.showFloater(250);
                                                    }
                                                });

                                            }
                                        }, 500, TimeUnit.MILLISECONDS);
                                    }
                                });
                            }

                            @Override
                            public void onPushSuccess() {
                                Log.i(TAG, "onPushSuccess");
                                currentService.disconnect(deviceAddress);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        closeDialog();
                                        triggerNewScan();
                                        scheduler.schedule(new Runnable() {

                                            @Override
                                            public void run() {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        discreteSeekBar.showFloater(250);
                                                    }
                                                });

                                            }
                                        }, 500, TimeUnit.MILLISECONDS);
                                    }
                                });
                            }
                        });
                    }
                }
            }
        }
    };

    /**
     * Manage Bluetooth Service lifecycle
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.i(TAG, "Connected to service");

            currentService = ((RFdroidService.LocalBinder) service).getService();

            currentService.clearScanningList();

            currentService.setADListener(RFdroidActivity.this);

            triggerNewScan();

            measurementTask = scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {

                    if (btDevice != null && btDevice.getAdvertizingInterval() > 0 && history.size() > 0) {

                        int packetReceptionRate = calculateReceptionRate();

                        List<Long> historyCopy = new ArrayList<Long>(history);
                        int rateCurrentSecond = calculateRateLastMillis(1000, historyCopy);

                        if (packetReceptionRate > 100)
                            packetReceptionRate = 100;

                        globalSumPerSecond.add(rateCurrentSecond);

                        final int finalPacketReceptionRate = packetReceptionRate;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                samplingTimeTv.setText(getSamplingTime(new Date().getTime()) + "s");
                                globalReceptionRateTv.setText(finalPacketReceptionRate + "%");
                                setData(globalSumPerSecond);
                                mChart.invalidate();
                                averagePacketReceivedTv.setText("" + getAveragePacket(new Date().getTime()) + " in 1 second");
                            }
                        });
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private int calculateReceptionRate() {
        long ts = new Date().getTime();
        return (int) ((history.size() * 100) / ((ts - history.get(0)) / btDevice.getAdvertizingInterval()));
    }

    private int calculateRateLastMillis(int millis, List<Long> historyList) {

        long currentTS = new Date().getTime() - millis;

        int count = 0;
        boolean control = false;

        for (int i = historyList.size() - 1; i >= 0 && !control; i--) {

            if (historyList.get(i) < currentTS)
                control = true;
            else
                count++;
        }
        return (int) ((count * 100) / (millis / btDevice.getAdvertizingInterval()));
    }

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
    public void onADframeReceived(String deviceAdress) {

        final Date time = new Date();
        history.add(time.getTime());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lastPacketReceivedTv.setText(sf.format(time));
                if (btDevice != null && history.size() > 0) {
                    totalPacketReceiveTv.setText("" + history.size() + " / " + ((time.getTime() - history.get(0)) / btDevice.getAdvertizingInterval()));
                } else {
                    totalPacketReceiveTv.setText("" + history.size());
                }
            }
        });
    }

    private float getAveragePacket(long currentTS) {

        if (history.size() > 0) {
            if ((currentTS - history.get(0)) != 0) {
                return ((history.size() * 1000) / ((currentTS - history.get(0))));
            }
            return 1;
        }
        return 0;
    }

    private long getSamplingTime(long ts) {
        return (ts - history.get(0)) / 1000;
    }
}
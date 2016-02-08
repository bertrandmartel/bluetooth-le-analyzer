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
package com.github.akinaru.rfdroid.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.akinaru.rfdroid.R;
import com.github.akinaru.rfdroid.adapter.ScanItemArrayAdapter;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothEvents;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;
import com.github.akinaru.rfdroid.chart.DataAxisFormatter;
import com.github.akinaru.rfdroid.inter.IADListener;
import com.github.akinaru.rfdroid.inter.IScheduledMeasureListener;
import com.github.akinaru.rfdroid.service.RFdroidService;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Dotti device management main activity
 *
 * @author Bertrand Martel
 */
public class BtDevicesActivity extends AppCompatActivity implements IADListener, IScheduledMeasureListener {

    /**
     * debug tag
     */
    private String TAG = this.getClass().getName();

    private ProgressDialog dialog = null;

    private boolean bound = false;

    /**
     * define if bluetooth is enabled on device
     */
    private final static int REQUEST_ENABLE_BT = 1;

    /**
     * Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    private RFdroidService currentService = null;

    protected BarChart mChart;

    private Toolbar toolbar = null;
    private DrawerLayout mDrawer = null;

    private GestureDetector mGestureDetector;

    private ActionBarDrawerToggle drawerToggle;

    private ProgressBar progress_bar;

    private NavigationView nvDrawer;

    private ScheduledExecutorService scheduler;

    private TableLayout tablelayout;

    private BluetoothObject btDevice = null;

    private TextView lastPacketReceivedTv = null;
    private TextView samplingTimeTv = null;
    private TextView totalPacketReceiveTv = null;
    private TextView averagePacketReceivedTv = null;

    private SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss.SSS");

    private SharedPreferences sharedpreferences;

    private final static String PREFERENCES = "storage";

    /**
     * list of device to display
     */
    private ListView scanningListView = null;

    private ScanItemArrayAdapter scanningAdapter = null;

    private int list_item_position = -1;

    private TextView deviceNameTv = null;
    private TextView deviceAddressTv = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btdevices);

        deviceNameTv = (TextView) findViewById(R.id.device_name);
        deviceAddressTv = (TextView) findViewById(R.id.device_address);

        sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        tablelayout = (TableLayout) findViewById(R.id.tablelayout);

        altTableRow(2);

        scheduler = Executors.newScheduledThreadPool(1);

        //Button button_stop_scanning = (Button) findViewById(R.id.stop_scanning_button);
        //progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar_item);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("BLE device - reception rate");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.inflateMenu(R.menu.toolbar_menu);


        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();

        mDrawer.setDrawerListener(drawerToggle);

        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        nvDrawer.setVisibility(View.GONE);
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

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

        /*
        if (progress_bar != null) {
            progress_bar.setEnabled(false);
            progress_bar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }
        */

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

        lastPacketReceivedTv = (TextView) findViewById(R.id.last_packet_received_value);
        samplingTimeTv = (TextView) findViewById(R.id.sampling_time_value);
        totalPacketReceiveTv = (TextView) findViewById(R.id.total_packet_received_value);
        averagePacketReceivedTv = (TextView) findViewById(R.id.average_packet_received_value);

        initTv();

        if (mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(this, RFdroidService.class);
            bound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void initTv() {
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
            case R.id.switch_chart_data: {
                break;
            }
        }
        mDrawer.closeDrawers();
    }

    private void changeScanStatus(MenuItem menuItem) {

        if (currentService != null && currentService.isScanning()) {
            Log.i(TAG, "scanning stopped...");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BtDevicesActivity.this, "scanning has stopped", Toast.LENGTH_SHORT).show();
                }
            });
            currentService.stopScan();
        } else {
            Log.i(TAG, "scanning ...");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BtDevicesActivity.this, "scanning ...", Toast.LENGTH_SHORT).show();
                }
            });
            scanningListView.setItemChecked(-1, true);
            triggerNewScan();
        }
    }

    private void closeDialog() {
        if (dialog != null) {
            dialog.cancel();
            dialog = null;
        }
    }

    private ActionBarDrawerToggle setupDrawerToggle() {

        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
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
        Log.i(TAG, "option selected : " + item.getItemId()
        );
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
            case R.id.scanning_button:
                changeScanStatus(item);
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

    private void setData(List<Integer> valueList, String format) {

        mChart.setVisibility(View.VISIBLE);

        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<BarEntry> yVals1 = new ArrayList<BarEntry>();

        for (int i = 0; i < valueList.size(); i++) {
            xVals.add(i + "s");
            yVals1.add(new BarEntry(valueList.get(i), i));
        }

        String legend = "reception rate per second";

        if (!format.equals("%"))
            legend = "packet count per second";

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

        if (currentService != null && !currentService.isScanning()) {

            Log.i(TAG, "START SCAN");

            if (scanningAdapter != null) {
                scanningAdapter.clear();
                scanningAdapter.notifyDataSetChanged();
            }

            currentService.clearScanningList();

            currentService.disconnectall();
            currentService.startScan();

        } else {
            Toast.makeText(BtDevicesActivity.this, "Scanning already engaged...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "RFdroidActivity onDestroy");
        //currentService.disconnect(deviceAddress);
        unregisterReceiver(mGattUpdateReceiver);

        try {
            if (bound) {
                unbindService(mServiceConnection);
                bound = false;
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

        if (currentService != null) {
            currentService.disconnectall();

            if (currentService.isScanning()) {
                /*
                if (progress_bar != null) {
                    progress_bar.setEnabled(false);
                    progress_bar.setVisibility(View.GONE);
                }
                */
                currentService.stopScan();
            }
        }
        closeDialog();
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

                if (btDevice != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (scanningAdapter != null) {
                                scanningAdapter.add(btDevice);
                                scanningAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }

            } else if (BluetoothEvents.BT_EVENT_DEVICE_DISCONNECTED.equals(action)) {

                Log.i(TAG, "Device disconnected");

                BluetoothObject btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null) {

                    closeDialog();

                }

            } else if (BluetoothEvents.BT_EVENT_DEVICE_CONNECTED.equals(action)) {
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

            currentService.setSelectionningDevice(true);

            currentService.clearScanningList();

            currentService.setADListener(BtDevicesActivity.this);
            currentService.setScheduledMeasureListener(BtDevicesActivity.this);

            scanningListView = (ListView) findViewById(R.id.scan_list);

            final ArrayList<BluetoothObject> list = new ArrayList<>();

            scanningAdapter = new ScanItemArrayAdapter(BtDevicesActivity.this,
                    android.R.layout.simple_list_item_1, list);

            scanningListView.setAdapter(scanningAdapter);

            scanningListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {


                    BluetoothObject btDevice = scanningAdapter.getItem(position);

                    if (currentService.getBtDevice() != null && btDevice != null && !btDevice.getDeviceAddress().equals(currentService.getBtDevice().getDeviceAddress())) {
                        currentService.stopScan();
                    }

                    scanningAdapter.notifyDataSetChanged();

                    if (deviceNameTv != null) {
                        deviceNameTv.setText(btDevice.getDeviceName());
                        deviceNameTv.setVisibility(View.VISIBLE);
                    }

                    if (deviceAddressTv != null) {
                        deviceAddressTv.setText(btDevice.getDeviceAddress());
                        deviceAddressTv.setVisibility(View.VISIBLE);
                    }

                    currentService.setBtDevice(btDevice);

                    if (!currentService.isScanning())
                        triggerNewScan();

                    scanningListView.setVisibility(View.GONE);
                }
            });

            triggerNewScan();
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
                averagePacketReceivedTv.setText("" + String.format("%.2f", averagePacket) + " in 1 second");
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


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {

            if (scanningListView.getVisibility() == View.VISIBLE) {
                onBackPressed();
            } else {
                if (currentService.isScanning()
                        )
                    currentService.stopScan();
                scanningListView.setVisibility(View.VISIBLE);
                if (deviceNameTv != null)
                    deviceNameTv.setVisibility(View.GONE);
                if (deviceAddressTv != null)
                    deviceAddressTv.setVisibility(View.GONE);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
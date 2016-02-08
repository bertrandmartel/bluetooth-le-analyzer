package com.github.akinaru.rfdroid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.github.akinaru.rfdroid.R;

/**
 * Created by akinaru on 07/02/16.
 */
public class MenuActivity extends AppCompatActivity {

    private Toolbar toolbar = null;

    private DrawerLayout mDrawer = null;

    private ActionBarDrawerToggle drawerToggle;

    private NavigationView nvDrawer;

    protected void onCreate(Bundle savedRessource) {
        super.onCreate(savedRessource);
        setContentView(R.layout.activity_menu);

        ImageButton btDevices = (ImageButton) findViewById(R.id.bluetooth_menu);
        ImageButton rfduino = (ImageButton) findViewById(R.id.rfduino_menu);

        btDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, BtDevicesActivity.class);
                startActivity(i);
            }
        });

        rfduino.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, RFdroidActivity.class);
                startActivity(i);
            }
        });

        toolbar = (Toolbar) findViewById(R.id.toolbar_item);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Select Bluetooth device type");
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
    }

    // Make sure this is the method with just `Bundle` as the signature
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
    }

    private void setupDrawerContent(NavigationView navigationView) {

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        //selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

}

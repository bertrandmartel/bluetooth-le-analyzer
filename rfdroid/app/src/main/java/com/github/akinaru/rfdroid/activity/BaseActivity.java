package com.github.akinaru.rfdroid.activity;

import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by akinaru on 18/03/16.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected Toolbar toolbar = null;

    protected DrawerLayout mDrawer = null;

    protected ActionBarDrawerToggle drawerToggle;

    protected NavigationView nvDrawer;

    @Override
    public void onBackPressed() {
        if (this.mDrawer.isDrawerOpen(GravityCompat.START)) {
            this.mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}

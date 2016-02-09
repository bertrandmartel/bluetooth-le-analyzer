package com.github.akinaru.rfdroid.menu;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;

import com.github.akinaru.rfdroid.R;
import com.github.akinaru.rfdroid.dialog.AboutDialog;
import com.github.akinaru.rfdroid.dialog.OpenSourceItemsDialog;

/**
 * Created by akinaru on 08/02/16.
 */
public class MenuUtils {

    public static void selectDrawerItem(MenuItem menuItem, DrawerLayout mDrawer, Context context) {

        switch (menuItem.getItemId()) {
            case R.id.switch_chart_data: {
                break;
            }
            case R.id.report_bugs: {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "kiruazoldik92@gmail.com", null));
                intent.putExtra(Intent.EXTRA_SUBJECT, "RFdroid Issue");
                intent.putExtra(Intent.EXTRA_TEXT, "Your error report here...");
                context.startActivity(Intent.createChooser(intent, "Report a problem"));
                break;
            }
            case R.id.open_source_components: {
                OpenSourceItemsDialog d = new OpenSourceItemsDialog(context);
                d.show();
                break;
            }
            case R.id.about_app: {
                AboutDialog dialog = new AboutDialog(context);
                dialog.show();
                break;
            }
        }
        mDrawer.closeDrawers();
    }
}

package com.github.akinaru.rfdroid;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

/**
 * Created by akinaru on 04/02/16.
 */
public class ChangeMaxIntervalDialog extends Dialog implements
        android.view.View.OnClickListener {

    public Context context;
    public Dialog dialog;

    public ChangeMaxIntervalDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.change_interval_max_dialog);
        Button button_change = (Button) findViewById(R.id.button_change);
        Button button_cancel = (Button) findViewById(R.id.button_cancel);
        button_change.setOnClickListener(this);
        button_cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_change:
                break;
            case R.id.button_cancel:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }
}
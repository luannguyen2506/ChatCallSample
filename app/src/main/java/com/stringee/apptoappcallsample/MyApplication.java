package com.stringee.apptoappcallsample;

import android.app.Application;

import com.stringee.kit.ui.commons.Common;
import com.stringee.kit.ui.commons.Constant;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Common.preferences = getSharedPreferences(Constant.PREF_BASE, MODE_PRIVATE);
    }
}

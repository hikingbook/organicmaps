package com.mapswithme.util;

import android.app.Application;

import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.location.LocationHelper;

import java.io.IOException;

public enum OrganicmapsFrameworkAdapter {
    INSTANCE;

    private static final String TAG = OrganicmapsFrameworkAdapter.class.getName();
    private static final int SEARCH_IN_VIEWPORT_ZOOM = 16;
    private MwmApplication mwmApplication;
    private Application application;

    public void initApplication(Application application) {
        if (this.application == null) {
            this.application = application;
            mwmApplication = new MwmApplication();
        }
    }

    public Application getApplication() {
        return this.application;
    }

    public void onCreateMwmApplication() {
        mwmApplication.onCreate();
    }

    public void initCore() {
        try {
            onCreateMwmApplication();
            mwmApplication.init();
            LocationHelper.INSTANCE.onEnteredIntoFirstRun();
            if (!LocationHelper.INSTANCE.isActive())
                LocationHelper.INSTANCE.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

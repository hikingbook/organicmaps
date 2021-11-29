package com.mapswithme.util;

import static com.mapswithme.maps.MwmActivity.EXTRA_LOCATION_DIALOG_IS_ANNOYING;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MapFragment;
import com.mapswithme.maps.MwmActivity;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;
import com.mapswithme.maps.location.CompassData;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.util.log.LoggerFactory;

import java.io.IOException;

public enum OrganicmapsFrameworkAdapter {
    INSTANCE;

    private static final String TAG = OrganicmapsFrameworkAdapter.class.getName();
    private static final int SEARCH_IN_VIEWPORT_ZOOM = 16;
    private MwmApplication mwmApplication;
    private MwmActivity mwmActivity;
    private Application application;
    private String applicationID;
    private FragmentActivity activity;
    private Fragment fragment;
    private View mapView;
    private Looper mainLooper;
    private SharedPreferences sharedPreferences;

    public void initApplication(Application application) {
        initLoggerFactory();
        if (this.application == null) {
            this.application = application;
            if (application instanceof MwmApplication) {
                mwmApplication = (MwmApplication) application;
                Log.d("initApplication", "is MwmApplication:"+mwmApplication);
            } else {
                mwmApplication = new MwmApplication();
                Log.d("initApplication", "is new MwmApplication:"+mwmApplication);
            }
        }
    }

    public Application getApplication() {
        return this.application;
    }

    public MwmApplication getMwmApplication() {
        return this.mwmApplication;
    }

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

    public FragmentActivity getActivity() {
        return this.activity;
    }

    public Boolean isMwmApplication() {
        return (application instanceof MwmApplication);
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getApplicationID() {
        return this.applicationID;
    }

    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    public Fragment getFragment() {
        return this.fragment;
    }

    public void setView(View view) {
        this.mapView = view;
    }

    public View getView() {
        return this.mapView;
    }

    public void setMainLooper(Looper looper) {
        this.mainLooper = this.application.getMainLooper();
    }

    public Looper getMainLooper() {
        return this.application.getMainLooper();
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public SharedPreferences getSharedPreferences() {
        return this.sharedPreferences;
    }

    private void initLoggerFactory() {
        LoggerFactory.INSTANCE.initialize(this.application);
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

    public boolean arePlatformAndCoreInitialized() {
        return mwmApplication.arePlatformAndCoreInitialized();
    }

    public void initMwmActivity(FragmentActivity activity, Fragment fragment, View view) {
        setFragment(fragment);
        setView(view);
        mwmActivity = new MwmActivity(activity);
    }

    public void onCreateMwmActivity(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mwmActivity.mLocationErrorDialogAnnoying = savedInstanceState.getBoolean(EXTRA_LOCATION_DIALOG_IS_ANNOYING);
        }
        mwmActivity.mIsTabletLayout = getApplication().getResources().getBoolean(R.bool.tabletLayout);
        if (!mwmActivity.mIsTabletLayout)
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        mwmActivity.initViews(false);
    }

    public void onResumeMwmActivity() {
        if (mwmActivity.mOnmapDownloader != null)
            mwmActivity.mOnmapDownloader.onResume();
    }

    public void onPauseMwmActivity() {
        if (mwmActivity.mOnmapDownloader != null)
            mwmActivity.mOnmapDownloader.onPause();
    }

    public void onStartMwmActivity() {
        Framework.nativePlacePageActivationListener((Framework.PlacePageActivationListener) getFragment());
        if (MapFragment.nativeIsEngineCreated())
            LocationHelper.INSTANCE.attach((LocationHelper.UiCallback) getFragment());
    }

    public void onStopMwmActivity() {
        LocationHelper.INSTANCE.detach(!mwmActivity.isFinishing());
        Framework.nativeRemovePlacePageActivationListener();
    }

    public void onLocationError() {
        if (mwmActivity.mLocationErrorDialogAnnoying)
            return;
    }

    public void onLocationNotFound() {
        mwmActivity.showLocationNotFoundDialog();
    }

    public void onCompassUpdated(@NonNull CompassData compass) {
        MapFragment.nativeCompassUpdated(compass.getNorth(), false);
    }

    public void onRenderingRestored() {
        mwmActivity.runTasks();
    }

    public void onRenderingInitializationFinished() {
        mwmActivity.runTasks();
    }

    public void onRenderingCreated() {
        mwmActivity.checkMeasurementSystem();
        LocationHelper.INSTANCE.attach((LocationHelper.UiCallback) getFragment());
    }

}

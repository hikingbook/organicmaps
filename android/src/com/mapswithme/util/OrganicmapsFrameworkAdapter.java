package com.mapswithme.util;

import static com.mapswithme.maps.MwmActivity.EXTRA_LOCATION_DIALOG_IS_ANNOYING;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.location.Location;
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
import com.mapswithme.maps.background.AppBackgroundTracker;
import com.mapswithme.maps.bookmarks.data.BookmarkCategory;
import com.mapswithme.maps.bookmarks.data.BookmarkManager;
import com.mapswithme.maps.bookmarks.data.MapObject;
import com.mapswithme.maps.downloader.OnmapDownloader;
import com.mapswithme.maps.location.CompassData;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.util.log.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

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
    private AppBackgroundTracker mBackgroundTracker;
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
        mwmActivity = new MwmActivity();
    }

    public Application getApplication() {
        return this.application;
    }

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

    public FragmentActivity getActivity() {
        return this.activity;
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

    public View getView() {
        return this.mapView;
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public SharedPreferences getSharedPreferences() {
        return this.sharedPreferences;
    }

    public void setBackgroundTracker(AppBackgroundTracker appBackgroundTracker) {
        this.mBackgroundTracker = appBackgroundTracker;
    }

    public AppBackgroundTracker getBackgroundTracker() {
        return this.mBackgroundTracker;
    }

    private void initLoggerFactory() {
        LoggerFactory.INSTANCE.initialize(this.application);
    }

    private void onCreateMwmApplication() {
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

    public void initActivity(FragmentActivity activity, Fragment fragment, View view) {
        setActivity(activity);
        setFragment(fragment);
        this.mapView = view;
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
    public void nativeDeactivatePopup() {
        Framework.nativeDeactivatePopup();
    }

    public void nativeDeactivateMapSelection() {
        Framework.nativeDeactivateMapSelection();
    }

    public Location getSavedLocation() {
        return LocationHelper.INSTANCE.getSavedLocation();
    }

    public void myPositionClick() {
        LocationHelper.INSTANCE.setStopLocationUpdateByUser(false);
        LocationHelper.INSTANCE.switchToNextMode();
        LocationHelper.INSTANCE.restart();
    }

    public void setViewportCenter(double lat, double lon) {
        Framework.nativeSetViewportCenter(lat, lon, SEARCH_IN_VIEWPORT_ZOOM, true);
    }

    public void zoomToPoint(double lat, double lon) {
        Framework.nativeZoomToPoint(lat, lon, SEARCH_IN_VIEWPORT_ZOOM, true);
    }

    public void updateNumMapLimit(boolean isVisible, String text, int color, int backgroundColor) {
        if (mwmActivity.mOnmapDownloader == null) {
            return;
        }
        mwmActivity.mOnmapDownloader.updateNumMapLimit(isVisible, text, color, backgroundColor);
    }

    public void setDownloaderDelegate(OnmapDownloader.IDownloaderDelegate downloaderDelegate) {
        mwmActivity.mOnmapDownloader.downloaderDelegate = downloaderDelegate;
    }

    public  void drawCirlce(Location location, float radius, int color, int width) {

    }

    /**
     * Bookmark CRUD
     */
    public long createBookmark(String catName, String name, String description, int color, double lat, double lon, int iconType) {
        long catId = searchCategoryIDWithName(catName);
        return BookmarkManager.INSTANCE.nativeAddBookmark(
                catId,
                name,
                description,
                color,
                lat,
                lon,
                iconType
        );
    }

    public void updateBookmark(long bmkId, String name, String description, int color, double lat, double lon) {
        BookmarkManager.INSTANCE.nativeUpdateBookmark(
                bmkId,
                name,
                description,
                color,
                lat,
                lon
        );
    }

    public void deleteBookmark(long bmkId) {
        BookmarkManager.INSTANCE.deleteBookmark(bmkId);
    }

    public void deleteAllBookmarksWithCategory(long catId) {
        BookmarkManager.INSTANCE.nativeDeleteAllBookmarkWithCategory(catId);
    }

    public long searchBookmarkIDWithName(String bmkName, long catId) {
        return BookmarkManager.INSTANCE.nativeSearchBookmarkIDWithName(bmkName, catId);
    }

    public void showBookmark(long bmkId) {
        BookmarkManager.INSTANCE.showBookmarkOnMap(bmkId);
    }

    /**
     * Bookmark Category CRUD
     */
    public long createCategory(@NonNull String catName) {
        return BookmarkManager.INSTANCE.createCategory(catName);
    }

    public void deleteCategory(long catId) {
        BookmarkManager.INSTANCE.deleteCategory(catId);
    }

    public void deleteAllCategory(List<Long> catIdList) {
        for (long catId : catIdList) {
            deleteCategory(catId);
        }
    }

    public void toggleCategoryVisibility(Long catId) {
        BookmarkCategory category =  BookmarkManager.INSTANCE.getCategoryById(catId);
        BookmarkManager.INSTANCE.toggleCategoryVisibility(category);
    }

    public void setCategoryVisibility(String catName, boolean visible) {
        long catId = OrganicmapsFrameworkAdapter.INSTANCE.searchCategoryIDWithName(catName);
        if (catId != Long.MAX_VALUE) {
            BookmarkManager.INSTANCE.setVisibility(catId, visible);
        }
    }

    public boolean isCategoryVisible(long catId) {
        return BookmarkManager.INSTANCE.isVisible(catId);
    }

    public long searchCategoryIDWithName(@NonNull String catName) {
        return BookmarkManager.INSTANCE.nativeSearchCategoryIDWithName(catName);
    }

    public void showBookmarkCategoryOnMap(long catId) {
        BookmarkManager.INSTANCE.showBookmarkCategoryOnMap(catId);
    }

    /**
     * Track CRUD
     */
    public long createTrack(long catId, String name, String description, Double[][] locations, int color, double lineWidth) {
        double[][] doubleLocations = new double[locations.length][2];
        for (int i = 0; i < locations.length; i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                doubleLocations[i] = Stream.of(locations[i]).mapToDouble(Double::doubleValue).toArray();
            } else {
                doubleLocations[i] = com.annimon.stream.Stream.of(locations[i]).mapToDouble(Double::doubleValue).toArray();
            }
        }

        return BookmarkManager.INSTANCE.nativeAddTrack(
                catId,
                name,
                description,
                doubleLocations,
                color,
                lineWidth
        );
    }

    public long addLocationIntoTrack(Location location, long catId) {
        return BookmarkManager.INSTANCE.nativeAddLocationIntoTrack(
                location.getLatitude(),
                location.getLongitude(),
                catId);
    }

    public boolean isTrackExistedInCategory(long catId) {
        if (catId != Long.MAX_VALUE) {
            int count = BookmarkManager.INSTANCE.nativeGetTracksCount(catId);
            return count > 0;
        }
        return false;
    }

    public void deleteTrack(long trackId) {
        BookmarkManager.INSTANCE.deleteTrack(trackId);
    }

    public void deleteAllTracksInCategory(long catId) {
        BookmarkManager.INSTANCE.nativeDeleteAllTracksInCategory(catId);
    }

    public int countTracksInCategory(long catId) {
        if (catId != Long.MAX_VALUE) {
            return BookmarkManager.INSTANCE.nativeGetTracksCount(catId);
        }
        return 0;
    }

    /**
     * Track Line CRUD
     */
    public int drawLineWithLocations(Double[][] locations, int color, double lineWidth) {
        double[][] doubleLocations = new double[locations.length][2];
        for (int i = 0; i < locations.length; i++) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                doubleLocations[i] = Stream.of(locations[i]).mapToDouble(Double::doubleValue).toArray();
            } else {
                doubleLocations[i] = com.annimon.stream.Stream.of(locations[i]).mapToDouble(Double::doubleValue).toArray();
            }
        }

        return BookmarkManager.INSTANCE.nativeDrawLineWithLocations(
                doubleLocations,
                color,
                lineWidth
        );
    }

    public void removeLine(int lineId) {
        BookmarkManager.INSTANCE.nativeRemoveLine(lineId);
    }

    public void clearLines() {
        BookmarkManager.INSTANCE.nativeClearLines();
    }

    /**
     * Iso Lines
     */
    public boolean isIsoLinesEnabled() {
        return Framework.nativeIsIsolinesLayerEnabled();
    }

    public void toggleIsoLines() {
        Framework.nativeSetIsolinesLayerEnabled(!Framework.nativeIsIsolinesLayerEnabled());
    }

    /**
     * Calculate Distance
     */
    public String getFlatDistance(double dstLatitude, double dstLongitude, double v) {
        MapObject myPosition = LocationHelper.INSTANCE.getMyPosition();
        return Framework.nativeGetDistanceAndAzimuthFromLatLon(dstLatitude, dstLongitude, myPosition.getLat(), myPosition.getLon(), v).getDistance();
    }

    /**
     * Map Font Size
     */
    public void setLargeFontsSize(boolean value) {
        Config.setLargeFontsSize(value);
    }

    public void isLargeFontsSize() {
        Config.isLargeFontsSize();
    }
}

package app.organicmaps.sdk;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.organicmaps.MwmActivity;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.sdk.bookmarks.data.PredefinedColors;
import app.organicmaps.sdk.downloader.MapManager;
import app.organicmaps.downloader.OnmapDownloader;
import app.organicmaps.intent.Factory;
import app.organicmaps.sdk.location.LocationHelper;
import app.organicmaps.sdk.location.LocationListener;
import app.organicmaps.sdk.location.LocationState;
import app.organicmaps.sdk.location.SensorHelper;
import app.organicmaps.sdk.util.Config;

public enum FrameworkAdapter {
    INSTANCE;

    private static final String TAG = FrameworkAdapter.class.getName();
    private MwmApplication mwmApplication = new MwmApplication();
    private final MwmActivity mwmActivity = new MwmActivity();

    private Application application;
    private String applicationID;
    private AppCompatActivity activity;
    private Fragment fragment;
    private SharedPreferences sharedPreferences;
    private MapRenderingListener mapRenderingListener;

    public void initApplicationIfNeed(Application application, String applicationID) {
        if (this.application != null) {
            return;
        }

        this.applicationID = applicationID;
        this.application = application;

        if (application instanceof MwmApplication) {
            mwmApplication = (MwmApplication) application;
        }
    }

    public Application getApplication() {
        return this.application;
    }

    public MwmApplication getMwmApplication() {
        return this.mwmApplication;
    }

    public FragmentActivity getActivity() {
        return this.activity;
    }

    public String getApplicationID() {
        return this.applicationID;
    }

    public Fragment getFragment() {
        return this.fragment;
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public SharedPreferences getSharedPreferences() {
        return this.sharedPreferences;
    }

    public void initCoreIfNeed(@NonNull Runnable onComplete) {
        try {
            if (!arePlatformAndCoreInitialized()) {
                mwmApplication.onCreate();
                mwmApplication.initOrganicMaps(onComplete);
            }
            else {
                onComplete.run();
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public boolean arePlatformAndCoreInitialized() {
        return mwmApplication.getOrganicMaps() != null && mwmApplication.getOrganicMaps().arePlatformAndCoreInitialized();
    }

    public boolean isMapEngineCreated() {
        return arePlatformAndCoreInitialized() && Map.isEngineCreated();
    }

    public LocationHelper getLocationHelper() {
        if (!arePlatformAndCoreInitialized()) {
            return null;
        }
        return mwmApplication.getLocationHelper();
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void restartLocation() {
        LocationHelper locationHelper = getLocationHelper();
        if (locationHelper != null) {
            locationHelper.restartWithNewMode();
        }
    }

    public void initActivity(AppCompatActivity activity,
                             MapRenderingListener mapRenderingListener,
                             Fragment fragment) {
        this.activity = activity;
        this.mapRenderingListener = mapRenderingListener;
        this.fragment = fragment;
    }

    public void onCreateMwmActivity(Bundle savedInstanceState) {
        mwmActivity.mIsTabletLayout = getApplication().getResources().getBoolean(R.bool.tabletLayout);
        if (!mwmActivity.mIsTabletLayout)
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        mwmActivity.initViews(false, savedInstanceState);
    }

    public boolean onStartMwmActivity(PlacePageActivationListener placePageActivationListener, ViewportListener viewportListener, LocationState.ModeChangeListener modeChangeListener, LocationListener locationListener) {
        if (!arePlatformAndCoreInitialized()) {
            return false;
        }
        Framework.nativePlacePageActivationListener(placePageActivationListener);
        Framework.nativeSetViewportListener(viewportListener);
        LocationState.nativeSetListener(modeChangeListener);

        LocationHelper locationHelper = getLocationHelper();
        if (locationHelper != null) {
            locationHelper.addListener(locationListener);
        }
        return true;
    }

    public boolean onResumeMwmActivity() {
        if (!arePlatformAndCoreInitialized()) {
            return false;
        }
        try {
            if (mwmActivity.mMapFragment != null) {
                if (mwmActivity.isMapRendererActive()) {
                    mwmActivity.mMapFragment.onResume();
                } else {
                    if (isMapFragmentAttached()) {
                        mwmActivity.mMapFragment.destroySurface(true);
                    }
                    activity.getSupportFragmentManager().beginTransaction().remove(mwmActivity.mMapFragment).commitNowAllowingStateLoss();
                    mwmActivity.initViews(false, null);
                }
            }

            if (mwmActivity.mOnmapDownloader != null) {
                mwmActivity.mOnmapDownloader.onResume();
            }

            SensorHelper sensorHelper = getSensorHelper();
            if (sensorHelper != null) {
                sensorHelper.addListener(mwmActivity);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    public boolean onPauseMwmActivity() {
        if (!arePlatformAndCoreInitialized()) {
            return false;
        }

        if (mwmActivity.mOnmapDownloader != null) {
            mwmActivity.mOnmapDownloader.onPause();
        }
        if (isMapFragmentAttached() && mwmActivity.mMapFragment != null) {
            mwmActivity.mMapFragment.onPause();
        }

        SensorHelper sensorHelper = getSensorHelper();
        if (sensorHelper != null) {
            sensorHelper.removeListener(mwmActivity);
        }
        return true;
    }

    public boolean onStopMwmActivity(PlacePageActivationListener placePageActivationListener, LocationListener locationListener) {
        if (!arePlatformAndCoreInitialized()) {
            return false;
        }
        Framework.nativeRemovePlacePageActivationListener(placePageActivationListener);
        Framework.nativeSetViewportListener(null);
        LocationState.nativeRemoveListener();

        LocationHelper locationHelper = getLocationHelper();
        if (locationHelper != null) {
            locationHelper.removeListener(locationListener);
        }
        return true;
    }

    public void onLocationUpdated(Location location) {
        mwmActivity.onLocationUpdated(location);
    }

    public void onRenderingRestored() {
        mwmActivity.onRenderingRestored();
    }

    public void onRenderingInitializationFinished() {
        mwmActivity.onRenderingInitializationFinished();
    }

    public void onRenderingCreated() {
        mwmActivity.onRenderingCreated();
    }

    public Location getSavedLocation() {
        LocationHelper locationHelper = getLocationHelper();
        if (locationHelper == null) {
            return null;
        }
        return locationHelper.getSavedLocation();
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void switchLocationStateToNextMode() {
        if (!isMapEngineCreated()) {
            return;
        }
        LocationState.nativeSwitchToNextMode();
        LocationHelper locationHelper = getLocationHelper();
        if (locationHelper != null && !locationHelper.isActive()) {
            locationHelper.start();
        }
    }

    public @Nullable OnmapDownloader getOnmapDownloader() {
        return mwmActivity.mOnmapDownloader;
    }

    public void updateCompassOffset(int offsetY, int offsetX) {
        mwmActivity.updateCompassOffset(offsetY, offsetX);
    }

    public void updateBottomWidgetsOffset(int offsetX, int offsetY) {
        mwmActivity.updateBottomWidgetsOffset(offsetX, offsetY);
    }

    public long createBookmark(String catName, String name, String description, @PredefinedColors.Color int color, double lat, double lon, int iconType) {
        if (!arePlatformAndCoreInitialized()) {
            return Long.MAX_VALUE;
        }
        long catId = searchCategoryIDWithName(catName);
        if (catId == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
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

    public void deleteBookmark(long bmkId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.deleteBookmark(bmkId);
        BookmarkManager.INSTANCE.nativeResetRecentlyDeletedBookmark();
    }

    public void deleteAllBookmarksWithCategory(long catId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.nativeDeleteAllBookmarkWithCategory(catId);
        BookmarkManager.INSTANCE.nativeResetRecentlyDeletedBookmark();
    }

    public long searchBookmarkIDWithName(String bmkName, long catId) {
        if (!arePlatformAndCoreInitialized()) {
            return Long.MAX_VALUE;
        }
        return BookmarkManager.INSTANCE.nativeSearchBookmarkIDWithName(bmkName, catId);
    }

    public void showBookmark(long bmkId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.showBookmarkOnMap(bmkId);
    }

    public long createCategory(@NonNull String catName) {
        if (!arePlatformAndCoreInitialized()) {
            return Long.MAX_VALUE;
        }
        return BookmarkManager.INSTANCE.createCategory(catName);
    }

    public void deleteCategory(long catId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.deleteCategory(catId);
    }

    public List<BookmarkCategory> getBookmarkCategories() {
        if (!arePlatformAndCoreInitialized()) {
            return new ArrayList<>();
        }
        return Arrays.asList(BookmarkManager.INSTANCE.nativeGetBookmarkCategories());
    }

    public void setCategoryVisibility(String catName, boolean visible) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        long catId = FrameworkAdapter.INSTANCE.searchCategoryIDWithName(catName);
        if (catId == Long.MAX_VALUE) {
            return;
        }
        BookmarkManager.INSTANCE.setVisibility(catId, visible);
    }

    public boolean isCategoryVisible(long catId) {
        if (!arePlatformAndCoreInitialized()) {
            return false;
        }
        return BookmarkManager.INSTANCE.isVisible(catId);
    }

    public long searchCategoryIDWithName(@NonNull String catName) {
        if (!arePlatformAndCoreInitialized()) {
            return Long.MAX_VALUE;
        }
        return BookmarkManager.INSTANCE.nativeSearchCategoryIDWithName(catName);
    }

    public void showBookmarkCategoryOnMap(long catId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.showBookmarkCategoryOnMap(catId);
    }

    public long addTracks(long catId, String name, String description, Location[][] locations, @PredefinedColors.Color int color, double lineWidth) {
        if (!arePlatformAndCoreInitialized()) {
            return Long.MAX_VALUE;
        }
        double[][][] doubleLocations = Arrays.stream(locations)
                        .map(row -> Arrays.stream(row)
                        .map(location -> new double[]{location.getLatitude(), location.getLongitude(), location.getAltitude()})
                        .toArray(double[][]::new))
                        .toArray(double[][][]::new);
        double[][] doubleTimestamps = Arrays.stream(locations)
                .map(row -> Arrays.stream(row)
                        .mapToDouble(Location::getTime)
                        .toArray())
                        .toArray(double[][]::new);

        try {
            return BookmarkManager.INSTANCE.nativeAddTracks(
                    catId,
                    name,
                    description,
                    doubleLocations,
                    doubleTimestamps,
                    color,
                    lineWidth
            );
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    public void deleteAllTracksInCategory(long catId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.nativeDeleteAllTracksInCategory(catId);
    }

    public int drawLineWithLocations(Location[] locations, @PredefinedColors.Color int color, double lineWidth) {
        if (!arePlatformAndCoreInitialized() || locations.length <= 1) {
            return Integer.MAX_VALUE;
        }

        double[][] doubleLocations = Arrays.stream(locations)
                .map(location -> new double[]{location.getLatitude(), location.getLongitude(), location.getAltitude()})
                .toArray(double[][]::new);

        return BookmarkManager.INSTANCE.nativeDrawLineWithLocations(
                doubleLocations,
                color,
                lineWidth
        );
    }

    public void removeLine(int lineId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.nativeRemoveLine(lineId);
    }

    public void clearLines() {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.nativeClearLines();
    }

    public void setLargeFontsSize(boolean value) {
        Config.setLargeFontsSize(value);
    }

    public boolean showLocation(Location location) {
        if (!arePlatformAndCoreInitialized()) {
            return false;
        }
        String geoUrl = Framework.nativeGetGe0Url(location.getLatitude(), location.getLongitude(), 15, "");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(geoUrl));
        Factory.UrlProcessor urlProcessor = new Factory.UrlProcessor();
        return urlProcessor.process(intent, mwmActivity);
    }

    public String getCountryRoot() {
        if (!arePlatformAndCoreInitialized()) {
            return "Countries";
        }
        return MapManager.nativeGetRoot();
    }

    public MapRenderingListener getMapRenderingListener() {
        return mapRenderingListener;
    }

    private SensorHelper getSensorHelper() {
        if (!arePlatformAndCoreInitialized()) {
            return null;
        }
        return mwmApplication.getSensorHelper();
    }

    private boolean isMapFragmentAttached() {
        return mwmActivity.mMapFragment != null && mwmActivity.mMapFragment.isAdded() && mwmActivity.mMapFragment.getContext() != null;
    }
}

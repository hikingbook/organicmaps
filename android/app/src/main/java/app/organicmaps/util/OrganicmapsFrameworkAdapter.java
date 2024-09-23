package app.organicmaps.util;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import app.organicmaps.Framework;
import app.organicmaps.Map;
import app.organicmaps.MwmActivity;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.bookmarks.data.BookmarkCategory;
import app.organicmaps.bookmarks.data.BookmarkManager;
import app.organicmaps.display.DisplayManager;
import app.organicmaps.downloader.OnmapDownloader;
import app.organicmaps.intent.Factory;
import app.organicmaps.location.LocationHelper;
import app.organicmaps.location.LocationListener;
import app.organicmaps.location.LocationState;
import app.organicmaps.location.SensorHelper;
import app.organicmaps.maplayer.isolines.IsolinesManager;
import app.organicmaps.maplayer.subway.SubwayManager;
import app.organicmaps.util.log.LogsManager;

public enum OrganicmapsFrameworkAdapter {
    INSTANCE;

    private static final String TAG = OrganicmapsFrameworkAdapter.class.getName();
    private static final int SEARCH_IN_VIEWPORT_ZOOM = 16;
    private MwmApplication mwmApplication = new MwmApplication();
    private final MwmActivity mwmActivity = new MwmActivity();

    private Application application;
    private String applicationID;
    private AppCompatActivity activity;
    private Fragment fragment;
    private SharedPreferences sharedPreferences;

    public void initApplicationIfNeed(Application application, String applicationID) {
        if (this.application != null) {
            return;
        }

        this.applicationID = applicationID;
        this.application = application;

        try {
            LogsManager.INSTANCE.initFileLogging(application);
        }
        catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        if (application instanceof MwmApplication) {
            mwmApplication = (MwmApplication) application;
        }
    }

    public Application getApplication() {
        return this.application;
    }

    public void setActivity(AppCompatActivity activity) {
        this.activity = activity;
    }

    public FragmentActivity getActivity() {
        return this.activity;
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
                mwmApplication.init(onComplete);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public boolean arePlatformAndCoreInitialized() {
        return mwmApplication.arePlatformAndCoreInitialized();
    }

    public boolean isMapEngineCreated() {
        return arePlatformAndCoreInitialized() && Map.isEngineCreated();
    }

    @NonNull
    public LocationHelper getLocationHelper() {
        if (mwmApplication.getLocationHelper() == null) {
            mwmApplication.onCreate();
        }
        return mwmApplication.getLocationHelper();
    }

    @NonNull
    public SensorHelper getSensorHelper() {
        if (mwmApplication.getSensorHelper() == null) {
            mwmApplication.onCreate();
        }
        return mwmApplication.getSensorHelper();
    }

    @NonNull
    public DisplayManager getDisplayManager() {

        if (mwmApplication.getDisplayManager() == null) {
            mwmApplication.onCreate();
        }
        return mwmApplication.getDisplayManager();

    }

    @NonNull
    public IsolinesManager getIsolinesManager() {

        if (mwmApplication.getIsolinesManager() == null) {
            mwmApplication.onCreate();
        }
        return mwmApplication.getIsolinesManager();
    }

    @NonNull
    public SubwayManager getSubwayManager() {
        if (mwmApplication.getSubwayManager() == null) {
            mwmApplication.onCreate();
        }
        return mwmApplication.getSubwayManager();
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void restartLocation() {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        getLocationHelper().restartWithNewMode();
    }

    public void initActivity(AppCompatActivity activity, Fragment fragment) {
        setActivity(activity);
        setFragment(fragment);
    }

    public void onCreateMwmActivity() {
        mwmActivity.mIsTabletLayout = getApplication().getResources().getBoolean(R.bool.tabletLayout);
        if (!mwmActivity.mIsTabletLayout)
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        mwmActivity.initViews(false);
    }

    public void onResumeMwmActivity() {
        if (mwmActivity.mOnmapDownloader != null && arePlatformAndCoreInitialized())
            mwmActivity.mOnmapDownloader.onResume();

        SensorHelper.from(activity).addListener(mwmActivity);
    }

    public void onPauseMwmActivity() {
        if (mwmActivity.mOnmapDownloader != null && arePlatformAndCoreInitialized())
            mwmActivity.mOnmapDownloader.onPause();

        SensorHelper.from(activity).removeListener(mwmActivity);
    }

    public void onStartMwmActivity(Framework.PlacePageActivationListener placePageActivationListener, LocationState.ModeChangeListener modeChangeListener, LocationListener locationListener) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        Framework.nativePlacePageActivationListener(placePageActivationListener);
        LocationState.nativeSetListener(modeChangeListener);
        getLocationHelper().addListener(locationListener);
    }

    public void onStopMwmActivity(Framework.PlacePageActivationListener placePageActivationListener, LocationListener locationListener) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        Framework.nativeRemovePlacePageActivationListener(placePageActivationListener);
        LocationState.nativeRemoveListener();
        getLocationHelper().removeListener(locationListener);
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
    public void nativeDeactivatePopup() {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        Framework.nativeDeactivatePopup();
    }

    public void nativeDeactivateMapSelection() {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        Framework.nativeDeactivateMapSelection();
    }

    public Location getSavedLocation() {
        return getLocationHelper().getSavedLocation();
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void myPositionClick() {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        LocationState.nativeSwitchToNextMode();
        if (!getLocationHelper().isActive())
            getLocationHelper().start();
    }

    public void setViewportCenter(double lat, double lon) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        Framework.nativeSetViewportCenter(lat, lon, SEARCH_IN_VIEWPORT_ZOOM);
    }

    public void zoomToPoint(double lat, double lon) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        Framework.nativeZoomToPoint(lat, lon, SEARCH_IN_VIEWPORT_ZOOM, true);
    }

    public void updateNumMapLimit(boolean isVisible, String text, int color, int backgroundColor) {
        if (mwmActivity.mOnmapDownloader == null) {
            return;
        }
        mwmActivity.mOnmapDownloader.updateNumMapLimit(isVisible, text, color, backgroundColor);
    }

    public void setDownloaderDelegate(OnmapDownloader.IDownloaderDelegate downloaderDelegate) {
        if (mwmActivity.mOnmapDownloader == null) {
            return;
        }
        mwmActivity.mOnmapDownloader.downloaderDelegate = downloaderDelegate;
    }

    public void updateCompassOffset(int offsetY, int offsetX) {
        mwmActivity.updateCompassOffset(offsetY, offsetX);
    }

    public void updateBottomWidgetsOffset(int offsetX, int offsetY) {
        mwmActivity.updateBottomWidgetsOffset(offsetX, offsetY);
    }

    /**
     * Bookmark CRUD
     */
    public long createBookmark(String catName, String name, String description, int color, double lat, double lon, int iconType) {
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

    public void updateBookmark(long bmkId, String name, String description, int color, double lat, double lon) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
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

    /**
     * Bookmark Category CRUD
     */
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

    public void toggleCategoryVisibility(Long catId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkCategory category =  BookmarkManager.INSTANCE.getCategoryById(catId);
        BookmarkManager.INSTANCE.toggleCategoryVisibility(category);
    }

    public void setCategoryVisibility(String catName, boolean visible) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        long catId = OrganicmapsFrameworkAdapter.INSTANCE.searchCategoryIDWithName(catName);
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

    /**
     * Track CRUD
     */
    public long createTrack(long catId, String name, String description, Double[][] locations, int color, double lineWidth) {
        if (!arePlatformAndCoreInitialized() || locations.length <= 1) {
            return Long.MAX_VALUE;
        }
        double[][] doubleLocations = new double[locations.length][2];
        for (int i = 0; i < locations.length; i++) {
            doubleLocations[i] = Stream.of(locations[i]).mapToDouble(Double::doubleValue).toArray();
        }

        try {
            return BookmarkManager.INSTANCE.nativeAddTrack(
                    catId,
                    name,
                    description,
                    doubleLocations,
                    color,
                    lineWidth
            );
        } catch (Throwable e) {
            return Long.MAX_VALUE;
        }
    }

    public boolean isTrackExistedInCategory(long catId) {
        if (!arePlatformAndCoreInitialized() || catId == Long.MAX_VALUE) {
            return false;
        }
        int count = BookmarkManager.INSTANCE.nativeGetTracksCount(catId);
        return count > 0;
    }

    public void deleteTrack(long trackId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.deleteTrack(trackId);
    }

    public void deleteAllTracksInCategory(long catId) {
        if (!arePlatformAndCoreInitialized()) {
            return;
        }
        BookmarkManager.INSTANCE.nativeDeleteAllTracksInCategory(catId);
    }

    public int countTracksInCategory(long catId) {
        if (!arePlatformAndCoreInitialized() || catId == Long.MAX_VALUE) {
            return 0;
        }
        return BookmarkManager.INSTANCE.nativeGetTracksCount(catId);
    }

    /**
     * Track Line CRUD
     */
    public int drawLineWithLocations(Double[][] locations, int color, double lineWidth) {
        if (!arePlatformAndCoreInitialized() || locations.length <= 1) {
            return 0;
        }
        double[][] doubleLocations = new double[locations.length][2];
        for (int i = 0; i < locations.length; i++) {
            doubleLocations[i] = Stream.of(locations[i]).mapToDouble(Double::doubleValue).toArray();
        }

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

    /**
     * Map Font Size
     */
    public void setLargeFontsSize(boolean value) {
        Config.setLargeFontsSize(value);
    }

    public void isLargeFontsSize() {
        Config.isLargeFontsSize();
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
}

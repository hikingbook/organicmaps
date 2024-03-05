package app.organicmaps.util;

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
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import app.organicmaps.Framework;
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

    public void initApplication(Application application) {
        if (this.application == null) {
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
    }

    public Application getApplication() {
        return this.application;
    }

    public void setActivity(AppCompatActivity activity) {
        this.activity = activity;
        initApplication(activity.getApplication());
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

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public SharedPreferences getSharedPreferences() {
        return this.sharedPreferences;
    }

    public void initCore() {
        try {
            mwmApplication.onCreate();
            mwmApplication.init(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean arePlatformAndCoreInitialized() {
        return mwmApplication.arePlatformAndCoreInitialized();
    }

    @NonNull
    public LocationHelper getLocationHelper()
    {
        return mwmApplication.getLocationHelper();
    }

    @NonNull
    public SensorHelper getSensorHelper()
    {
        return mwmApplication.getSensorHelper();
    }

    @NonNull
    public DisplayManager getDisplayManager()
    {
        return mwmApplication.getDisplayManager();
    }

    @NonNull
    public IsolinesManager getIsolinesManager()
    {
        return mwmApplication.getIsolinesManager();
    }

    @NonNull
    public SubwayManager getSubwayManager()
    {
        return mwmApplication.getSubwayManager();
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void restartLocation() {
        getLocationHelper().restartWithNewMode();
    }

    public void initActivity(AppCompatActivity activity, Fragment fragment) {
        setActivity(activity);
        setFragment(fragment);
    }

    public void onCreateMwmActivity(Bundle savedInstanceState) {
        mwmActivity.mIsTabletLayout = getApplication().getResources().getBoolean(R.bool.tabletLayout);
        if (!mwmActivity.mIsTabletLayout)
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        mwmActivity.initViews(false);
    }

    public void onResumeMwmActivity() {
        if (mwmActivity.mOnmapDownloader != null)
            mwmActivity.mOnmapDownloader.onResume();

        SensorHelper.from(activity).addListener(mwmActivity);
    }

    public void onPauseMwmActivity() {
        if (mwmActivity.mOnmapDownloader != null)
            mwmActivity.mOnmapDownloader.onPause();

        SensorHelper.from(activity).removeListener(mwmActivity);
    }

    public void onStartMwmActivity(Framework.PlacePageActivationListener placePageActivationListener, LocationState.ModeChangeListener modeChangeListener, LocationListener locationListener) {
        Framework.nativePlacePageActivationListener(placePageActivationListener);
        LocationState.nativeSetListener(modeChangeListener);
        getLocationHelper().addListener(locationListener);
    }

    public void onStopMwmActivity(Framework.PlacePageActivationListener placePageActivationListener, LocationListener locationListener) {
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
        Framework.nativeDeactivatePopup();
    }

    public void nativeDeactivateMapSelection() {
        Framework.nativeDeactivateMapSelection();
    }

    public Location getSavedLocation() {
        return getLocationHelper().getSavedLocation();
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void myPositionClick() {
        LocationState.nativeSwitchToNextMode();
        if (!getLocationHelper().isActive())
            getLocationHelper().start();
    }

    public void setViewportCenter(double lat, double lon) {
        Framework.nativeSetViewportCenter(lat, lon, SEARCH_IN_VIEWPORT_ZOOM);
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

    public List<BookmarkCategory> getBookmarkCategories() {
        return Arrays.asList(BookmarkManager.INSTANCE.nativeGetBookmarkCategories());
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
        if (locations.length <= 1) {
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
        if (locations.length <= 1) {
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
        BookmarkManager.INSTANCE.nativeRemoveLine(lineId);
    }

    public void clearLines() {
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
        String geoUrl = Framework.nativeGetGe0Url(location.getLatitude(), location.getLongitude(), 15, "");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(geoUrl));
        Factory.UrlProcessor urlProcessor = new Factory.UrlProcessor();
        return urlProcessor.process(intent, mwmActivity);
    }
}

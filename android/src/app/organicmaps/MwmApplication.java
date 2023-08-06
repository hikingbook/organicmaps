/**
 * Author by robin, Date on 11/14/21.
 * Comment: Comment unused code
 */
package app.organicmaps;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

import app.organicmaps.background.AppBackgroundTracker;
import app.organicmaps.background.NotificationChannelFactory;
import app.organicmaps.background.NotificationChannelProvider;
import app.organicmaps.background.Notifier;
import app.organicmaps.bookmarks.data.BookmarkManager;
import app.organicmaps.downloader.CountryItem;
import app.organicmaps.downloader.MapManager;
import app.organicmaps.location.LocationHelper;
import app.organicmaps.search.SearchEngine;
import app.organicmaps.settings.StoragePathManager;
import app.organicmaps.util.Config;
import app.organicmaps.util.ConnectionState;
import app.organicmaps.util.OrganicmapsFrameworkAdapter;
import app.organicmaps.util.SharedPropertiesUtils;
import app.organicmaps.util.StorageUtils;
import app.organicmaps.util.UiUtils;
import app.organicmaps.util.log.Logger;
import app.organicmaps.util.log.LogsManager;

public class MwmApplication extends Application implements AppBackgroundTracker.OnTransitionListener
{
  @SuppressWarnings("NotNullFieldNotInitialized")
  @NonNull
  private static final String TAG = MwmApplication.class.getSimpleName();

  private AppBackgroundTracker mBackgroundTracker;
//  @SuppressWarnings("NotNullFieldNotInitialized")
//  @NonNull
//  private SubwayManager mSubwayManager;

//  @SuppressWarnings("NotNullFieldNotInitialized")
//  @NonNull
//  private IsolinesManager mIsolinesManager;

  private volatile boolean mFrameworkInitialized;
  private volatile boolean mPlatformInitialized;

  private Handler mMainLoopHandler;
  private final Object mMainQueueToken = new Object();
  @NonNull
  private final MapManager.StorageCallback mStorageCallbacks = new StorageCallbackImpl();
//  private MediaPlayerWrapper mPlayer;

//  @NonNull
//  public SubwayManager getSubwayManager()
//  {
//    return mSubwayManager;
//  }

//  @NonNull
//  public IsolinesManager getIsolinesManager()
//  {
//    return mIsolinesManager;
//  }

  @NonNull
  public static Application from(@NonNull Context context)
  {
    return (Application) OrganicmapsFrameworkAdapter.INSTANCE.getApplication();
//    return (MwmApplication) context.getApplicationContext();
  }

  @NonNull
  public static AppBackgroundTracker backgroundTracker(@NonNull Context context)
  {
    return ((MwmApplication) context.getApplicationContext()).getBackgroundTracker();
  }

  @NonNull
  public static SharedPreferences prefs(@NonNull Context context)
  {
    SharedPreferences mPrefs;
    if (OrganicmapsFrameworkAdapter.INSTANCE.getSharedPreferences() == null) {
      mPrefs = context.getSharedPreferences(context.getString(R.string.pref_file_name), MODE_PRIVATE);
      OrganicmapsFrameworkAdapter.INSTANCE.setSharedPreferences(mPrefs);
    } else {
      mPrefs = OrganicmapsFrameworkAdapter.INSTANCE.getSharedPreferences();
    }

    return mPrefs;
//    return context.getSharedPreferences(context.getString(R.string.pref_file_name), MODE_PRIVATE);
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    Logger.i(TAG, "Initializing application");
    OrganicmapsFrameworkAdapter.INSTANCE.initApplication(this);
    LogsManager.INSTANCE.initFileLogging(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());

    // Set configuration directory as early as possible.
    // Other methods may explicitly use Config, which requires settingsDir to be set.
    final String settingsPath = StorageUtils.getSettingsPath(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
    if (!StorageUtils.createDirectory(settingsPath))
      throw new AssertionError("Can't create settingsDir " + settingsPath);
    Logger.d(TAG, "Settings path = " + settingsPath);
    nativeSetSettingsDir(settingsPath);

    mMainLoopHandler = new Handler(OrganicmapsFrameworkAdapter.INSTANCE.getApplication().getMainLooper());
    ConnectionState.INSTANCE.initialize(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
    
    initNotificationChannels();

    mBackgroundTracker = new AppBackgroundTracker(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
    OrganicmapsFrameworkAdapter.INSTANCE.setBackgroundTracker(mBackgroundTracker);
//    mSubwayManager = new SubwayManager(this);
//    mIsolinesManager = new IsolinesManager(this);
//    mPlayer = new MediaPlayerWrapper(this);
//    WebView.setWebContentsDebuggingEnabled(Utils.isDebugOrBeta());
  }

  private void initNotificationChannels()
  {
    NotificationChannelProvider channelProvider = NotificationChannelFactory.createProvider(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
    channelProvider.setDownloadingChannel();
  }

  /**
   * Initialize native core of application: platform and framework.
   *
   * @throws IOException - if failed to create directories. Caller must handle
   * the exception and do nothing with native code if initialization is failed.
   */
  public void init() throws IOException
  {
    initNativePlatform();
    initNativeFramework();
  }

  private void initNativePlatform() throws IOException
  {
    if (mPlatformInitialized)
      return;

    final String apkPath = StorageUtils.getApkPath(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
    Logger.d(TAG, "Apk path = " + apkPath);
    // Note: StoragePathManager uses Config, which requires SettingsDir to be set.
    final String writablePath = StoragePathManager.findMapsStorage(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
    Logger.d(TAG, "Writable path = " + writablePath);
    final String privatePath = StorageUtils.getPrivatePath(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
    Logger.d(TAG, "Private path = " + privatePath);
    final String tempPath = StorageUtils.getTempPath(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
    Logger.d(TAG, "Temp path = " + tempPath);

    // If platform directories are not created it means that native part of app will not be able
    // to work at all. So, we just ignore native part initialization in this case, e.g. when the
    // external storage is damaged or not available (read-only).
    createPlatformDirectories(writablePath, privatePath, tempPath);

    nativeInitPlatform((Context)OrganicmapsFrameworkAdapter.INSTANCE.getApplication(),
                       apkPath,
                       writablePath,
                       privatePath,
                       tempPath,
                       app.organicmaps.BuildConfig.FLAVOR,
                       app.organicmaps.BuildConfig.BUILD_TYPE, UiUtils.isTablet(this));
    Config.setStoragePath(writablePath);
    Config.setStatisticsEnabled(SharedPropertiesUtils.isStatisticsEnabled(OrganicmapsFrameworkAdapter.INSTANCE.getApplication()));

//    Editor.init(this);
    mPlatformInitialized = true;
    Logger.i(TAG, "Platform initialized");
  }

  private void createPlatformDirectories(@NonNull String writablePath,
                                            @NonNull String privatePath,
                                            @NonNull String tempPath) throws IOException
  {
//    SharedPropertiesUtils.emulateBadExternalStorage(this);

    StorageUtils.requireDirectory(writablePath);
    StorageUtils.requireDirectory(privatePath);
    StorageUtils.requireDirectory(tempPath);
  }

  private void initNativeFramework()
  {
    if (mFrameworkInitialized)
      return;

    nativeInitFramework();

    MapManager.nativeSubscribe(mStorageCallbacks);

    initNativeStrings();
//    ThemeSwitcher.INSTANCE.initialize(this);
    SearchEngine.INSTANCE.initialize(null);
    BookmarkManager.loadBookmarks();
//    TtsPlayer.INSTANCE.initialize(this);
//    ThemeSwitcher.INSTANCE.restart(false);
    LocationHelper.INSTANCE.initialize(OrganicmapsFrameworkAdapter.INSTANCE.getApplication());
//    RoutingController.get().initialize(null);
//    TrafficManager.INSTANCE.initialize(null);
//    SubwayManager.from(this).initialize(null);
//    IsolinesManager.from(this).initialize(null);
    OrganicmapsFrameworkAdapter.INSTANCE.getBackgroundTracker().addListener(this);

    Logger.i(TAG, "Framework initialized");
    mFrameworkInitialized = true;
  }

  private void initNativeStrings()
  {
    nativeAddLocalization("core_entrance", OrganicmapsFrameworkAdapter.INSTANCE.getApplication().getString(R.string.core_entrance));
    nativeAddLocalization("core_exit", OrganicmapsFrameworkAdapter.INSTANCE.getApplication().getString(R.string.core_exit));
    nativeAddLocalization("core_my_places", OrganicmapsFrameworkAdapter.INSTANCE.getApplication().getString(R.string.core_my_places));
    nativeAddLocalization("core_my_position", OrganicmapsFrameworkAdapter.INSTANCE.getApplication().getString(R.string.core_my_position));
    nativeAddLocalization("core_placepage_unknown_place", OrganicmapsFrameworkAdapter.INSTANCE.getApplication().getString(R.string.core_placepage_unknown_place));
    nativeAddLocalization("postal_code",  OrganicmapsFrameworkAdapter.INSTANCE.getApplication().getString(R.string.postal_code));
    nativeAddLocalization("wifi", OrganicmapsFrameworkAdapter.INSTANCE.getApplication().getString(R.string.wifi));
  }

  public boolean arePlatformAndCoreInitialized()
  {
    return mFrameworkInitialized && mPlatformInitialized;
  }

  @NonNull
  public AppBackgroundTracker getBackgroundTracker()
  {
    return OrganicmapsFrameworkAdapter.INSTANCE.getBackgroundTracker();
  }

  static
  {
    System.loadLibrary("organicmaps");
  }

//  public static void onUpgrade(@NonNull Context context)
//  {
//    Counters.resetAppSessionCounters(context);
//  }

  // Called from jni
  @SuppressWarnings("unused")
  void forwardToMainThread(final long taskPointer)
  {
    Message m = Message.obtain(mMainLoopHandler, () -> nativeProcessTask(taskPointer));
    m.obj = mMainQueueToken;
    mMainLoopHandler.sendMessage(m);
  }

//  @NonNull
//  public MediaPlayerWrapper getMediaPlayer()
//  {
//    return mPlayer;
//  }

  private static native void nativeSetSettingsDir(String settingsPath);
  private native void nativeInitPlatform(Context context, String apkPath, String writablePath, String privatePath,
                                         String tmpPath, String flavorName, String buildType,
                                         boolean isTablet);
  private static native void nativeInitFramework();
  private static native void nativeProcessTask(long taskPointer);
  private static native void nativeAddLocalization(String name, String value);
  private static native void nativeOnTransit(boolean foreground);

  @Override
  public void onTransit(boolean foreground)
  {
    nativeOnTransit(foreground);
  }

  private class StorageCallbackImpl implements MapManager.StorageCallback
  {
    @Override
    public void onStatusChanged(List<MapManager.StorageCallbackData> data)
    {
      Notifier notifier = Notifier.from(MwmApplication.this);
      for (MapManager.StorageCallbackData item : data)
        if (item.isLeafNode && (item.newOrganicMapStatus == CountryItem.STATUS_FAILED || item.newHikingbookProMapStatus == CountryItem.STATUS_FAILED))
        {
          if (MapManager.nativeIsAutoretryFailed())
          {
            notifier.notifyDownloadFailed(item.countryId, MapManager.nativeGetName(item.countryId));
          }

          return;
        }
    }

    @Override
    public void onProgress(String countryId, long localSize, long remoteSize) {}
  }
}

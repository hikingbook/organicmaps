// This file is modified by Ke Zheng-Xiang on 2025.
package app.organicmaps.downloader;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import app.organicmaps.sdk.MapSource;
import app.organicmaps.sdk.downloader.CountryItem;
import app.organicmaps.sdk.downloader.MapManager;
import app.organicmaps.sdk.util.log.Logger;

public class DownloaderService extends Service implements MapManager.StorageCallback
{
  private static final String TAG = DownloaderService.class.getSimpleName();
  private static final String ACTION_CANCEL_DOWNLOAD = "ACTION_CANCEL_DOWNLOAD";
  private static final String ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD";
  private static final String ACTION_RETRY_DOWNLOAD = "ACTION_RETRY_DOWNLOAD";
  private static final String ACTION_START_UPDATE = "ACTION_START_UPDATE";
  private static final String EXTRA_COUNTRY_IDS = "EXTRA_COUNTRY_IDS";
    private static final String EXTRA_MAP_SOURCE = "EXTRA_MAP_SOURCE";

  private final DownloaderNotifier mNotifier = new DownloaderNotifier(this);
  private int mSubscriptionSlot;

  @Override
  public void onCreate()
  {
    super.onCreate();

    Logger.i(TAG);

    mSubscriptionSlot = MapManager.nativeSubscribe(this);
  }

  static PendingIntent buildCancelPendingIntent(Context context)
  {
    final int FLAG_IMMUTABLE = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? 0 : PendingIntent.FLAG_IMMUTABLE;
    Intent cancelIntent = new Intent(context, DownloaderService.class);
    cancelIntent.setAction(ACTION_CANCEL_DOWNLOAD);
    return PendingIntent.getService(context, 2, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    final String action = intent != null ? intent.getAction() : null;

    // Cancel is sent via PendingIntent.getService() (not startForegroundService()),
    // so no foreground promotion is needed.
    if (ACTION_CANCEL_DOWNLOAD.equals(action))
    {
      Logger.d(TAG, "Cancel action received, aborting all downloads");
      MapManager.nativeCancel(MapManager.nativeGetRoot());
      stopSelf();
      return START_NOT_STICKY;
    }

    // Download/retry/update actions are sent via startForegroundService().
    // Call startForeground() immediately to satisfy the contract, then enqueue work.
    // The download is deferred to here (instead of being started by MapManagerHelper) to
    // eliminate the race where a fast download completes before onStartCommand() runs.
    if (ACTION_START_DOWNLOAD.equals(action) || ACTION_RETRY_DOWNLOAD.equals(action)
        || ACTION_START_UPDATE.equals(action))
    {
      var notification = mNotifier.buildProgressNotification();
      Logger.i(TAG, "Starting Downloader Foreground Service");
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ServiceCompat.startForeground(this, DownloaderNotifier.NOTIFICATION_ID, notification,
                                      ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
      else
        ServiceCompat.startForeground(this, DownloaderNotifier.NOTIFICATION_ID, notification, 0);

      final String[] countryIds = intent.getStringArrayExtra(EXTRA_COUNTRY_IDS);
      if (countryIds != null)
      {
          MapSource mapSource = MapSource.ORGANIC_MAPS;
          try {
              mapSource = MapSource.valueOf(intent.getStringExtra(EXTRA_MAP_SOURCE));
          } catch (Exception ignored) {}
        if (ACTION_START_DOWNLOAD.equals(action))
        {
          for (String countryId : countryIds)
            MapManager.startDownload(countryId, mapSource);
        }
        else if (ACTION_RETRY_DOWNLOAD.equals(action))
          MapManager.retryDownload(countryIds[0], mapSource);
        else
          MapManager.startUpdate(countryIds[0], mapSource);
      }

      // The enqueue may be a no-op (already downloaded, stale retry, empty batch, etc.).
      // Re-check to avoid leaving a foreground service with no work and no callback to stop it.
      if (!MapManager.nativeIsDownloading())
      {
        Logger.i(TAG, "No active downloads after enqueue, stopping DownloaderService");
        stopSelf(startId);
      }
      return START_NOT_STICKY;
    }

    // No recognized action — stop if nothing to do.
    Logger.i(TAG, "Downloading: " + MapManager.nativeIsDownloading());
    if (!MapManager.nativeIsDownloading())
    {
      Logger.i(TAG, "No active downloads, stopping DownloaderService");
      stopSelf(startId);
    }
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent)
  {
    return null;
  }

  @Override
  public void onStatusChanged(List<MapManager.StorageCallbackData> data)
  {
    final boolean isDownloading = MapManager.nativeIsDownloading();
    final boolean hasFailed = hasDownloadFailed(data);

    Logger.i(TAG, "Downloading: " + isDownloading + " failure: " + hasFailed);

    if (!isDownloading)
    {
      if (hasFailed)
      {
        // Detach service from the notification to keep after the service is stopped.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
          stopForeground(Service.STOP_FOREGROUND_DETACH);
        }
        else
        {
          stopForeground(false);
        }
      }
      stopSelf();
    }
  }

  @Override
  public void onProgress(String countryId, long bytesDownloaded, long bytesTotal)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PERMISSION_GRANTED)
    {
      Logger.w(TAG, "Permission POST_NOTIFICATIONS is not granted, skipping notification");
      return;
    }

    mNotifier.notifyProgress(countryId, (int) bytesTotal, (int) bytesDownloaded);
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();

    Logger.i(TAG, "onDestroy");

    MapManager.nativeUnsubscribe(mSubscriptionSlot);
  }

  @Override
  public void onTimeout(int startId)
  {
    onTimeout(startId, 0);
  }

  @Override
  public void onTimeout(int startId, int fgsType)
  {
    Logger.w(TAG, "Foreground service timed out, cancelling downloads and stopping the service"
                      + " startId: " + startId + " fgsType: " + fgsType);
    MapManager.nativeCancel(MapManager.nativeGetRoot());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
      stopForeground(Service.STOP_FOREGROUND_REMOVE);
    else
      stopForeground(true);
    stopSelf(startId);
  }

  /**
   * Start the foreground service and enqueue downloads for the given countries.
   */
  static void startDownload(Context context, MapSource mapSource, String... countryIds)
  {
    Logger.i(TAG);
    Intent intent = new Intent(context, DownloaderService.class);
    intent.setAction(ACTION_START_DOWNLOAD);
    intent.putExtra(EXTRA_COUNTRY_IDS, countryIds);
    intent.putExtra(EXTRA_MAP_SOURCE, mapSource.name());
    ContextCompat.startForegroundService(context, intent);
  }

  /**
   * Start the foreground service and retry a failed download.
   */
  static void startRetryDownload(Context context, @NonNull String countryId, MapSource mapSource)
  {
    Logger.i(TAG);
    Intent intent = new Intent(context, DownloaderService.class);
    intent.setAction(ACTION_RETRY_DOWNLOAD);
    intent.putExtra(EXTRA_COUNTRY_IDS, new String[] {countryId});
    intent.putExtra(EXTRA_MAP_SOURCE, mapSource.name());
    ContextCompat.startForegroundService(context, intent);
  }

  /**
   * Start the foreground service and enqueue an update for the given root.
   */
  static void startUpdate(Context context, @NonNull String root, MapSource mapSource)
  {
    Logger.i(TAG);
    Intent intent = new Intent(context, DownloaderService.class);
    intent.setAction(ACTION_START_UPDATE);
    intent.putExtra(EXTRA_COUNTRY_IDS, new String[] {root});
    intent.putExtra(EXTRA_MAP_SOURCE, mapSource.name());
    ContextCompat.startForegroundService(context, intent);
  }

  private boolean hasDownloadFailed(List<MapManager.StorageCallbackData> data)
  {
    for (MapManager.StorageCallbackData item : data)
    {
      if (item.isLeafNode && (item.newOrganicMapStatus == CountryItem.STATUS_FAILED || item.newHikingbookProMapStatus == CountryItem.STATUS_FAILED))
      {
        if (MapManager.nativeIsAutoretryFailed())
        {
          mNotifier.notifyDownloadFailed(item.countryId);
          return true;
        }
      }
    }

    return false;
  }
}

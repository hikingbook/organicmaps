// This file is updated for Hikingbook Pro Maps by Zheng-Xiang Ke on 2022.
package app.organicmaps.downloader;

import android.app.Activity;
import android.location.Location;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

import app.organicmaps.MapSource;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;
import app.organicmaps.location.LocationHelper;
import app.organicmaps.routing.RoutingController;
import app.organicmaps.util.Config;
import app.organicmaps.util.ConnectionState;
import app.organicmaps.util.StringUtils;
import app.organicmaps.util.UiUtils;
import app.organicmaps.widget.WheelProgressView;

public class OnmapDownloader implements MwmActivity.LeftAnimationTrackListener
{
  private static boolean sAutodownloadLocked;

  private final Activity mActivity;
  private final View mFrame;
  private final TextView mParent;
  private final TextView mTitle;
  private final TextView mMapSource;
  private final TextView mSize;
  private final WheelProgressView mProgress;
  private final Button mButton;
  private final TextView mNumMapLimit;
  public IDownloaderDelegate downloaderDelegate;

  private int mStorageSubscriptionSlot;

  @Nullable
  private CountryItem mCurrentCountry;

  private final MapManager.StorageCallback mStorageCallback = new MapManager.StorageCallback()
  {
    @Override
    public void onStatusChanged(List<MapManager.StorageCallbackData> data)
    {
      if (mCurrentCountry == null)
        return;

      for (MapManager.StorageCallbackData item : data)
      {
        if (!item.isLeafNode)
          continue;

        if (item.newOrganicMapStatus == CountryItem.STATUS_FAILED || item.newHikingbookProMapStatus == CountryItem.STATUS_FAILED)
          MapManager.showError(mActivity, item, null);

        if (mCurrentCountry.id.equals(item.countryId))
        {
          mCurrentCountry.update();
          updateProgressState(false);

          return;
        }
      }
    }

    @Override
    public void onProgress(String countryId, long localSize, long remoteSize)
    {
      if (mCurrentCountry != null && mCurrentCountry.id.equals(countryId))
      {
        mCurrentCountry.update();
        updateProgressState(false);
      }
    }
  };

  private final MapManager.CurrentCountryChangedListener mCountryChangedListener = new MapManager.CurrentCountryChangedListener()
  {
    @Override
    public void onCurrentCountryChanged(String countryId)
    {
      mCurrentCountry = (TextUtils.isEmpty(countryId) ? null : CountryItem.fill(countryId));
      updateState(false);
      if (downloaderDelegate != null) {
        downloaderDelegate.onCurrentCountryChanged(mCurrentCountry);
      }
    }
  };

  public void updateState(boolean shouldAutoDownload)
  {
    updateStateInternal(shouldAutoDownload);
  }

  private static boolean isMapDownloading(@Nullable CountryItem country)
  {
    if (country == null) return false;

    boolean enqueued = country.status == CountryItem.STATUS_ENQUEUED || country.hikingbookProMapStatus == CountryItem.STATUS_ENQUEUED;
    boolean progress = country.status == CountryItem.STATUS_PROGRESS || country.hikingbookProMapStatus == CountryItem.STATUS_PROGRESS;
    boolean applying = country.status == CountryItem.STATUS_APPLYING || country.hikingbookProMapStatus == CountryItem.STATUS_APPLYING;
    return enqueued || progress || applying;
  }

  private void updateProgressState(boolean shouldAutoDownload)
  {
    updateStateInternal(shouldAutoDownload);
  }

  private void updateStateInternal(boolean shouldAutoDownload)
  {
    boolean showFrame = (mCurrentCountry != null &&
                         !mCurrentCountry.present &&
                         !RoutingController.get().isNavigating());
    if (showFrame)
    {
      int status = countryItemStatus();
      boolean enqueued = (status == CountryItem.STATUS_ENQUEUED);
      boolean progress = (status == CountryItem.STATUS_PROGRESS ||
                          status == CountryItem.STATUS_APPLYING);
      boolean failed = (status == CountryItem.STATUS_FAILED);

      showFrame = (enqueued || progress || failed ||
                  status == CountryItem.STATUS_DOWNLOADABLE);

      if (showFrame)
      {
        boolean hasParent = !CountryItem.isRoot(mCurrentCountry.topmostParentId);

        String mapSourceName = "";
        if (downloaderDelegate != null) {
          mapSourceName = downloaderDelegate.l10nMapSource(getMapSource());
        }

        UiUtils.showIf(!mapSourceName.isBlank() && (progress || enqueued), mMapSource);
        UiUtils.showIf(progress || enqueued, mSize);
        UiUtils.showIf(progress || enqueued, mProgress);
        UiUtils.showIf(!progress && !enqueued, mButton);
        UiUtils.showIf(hasParent, mParent);

        if (hasParent)
          mParent.setText(mCurrentCountry.topmostParentName);

        mTitle.setText(mCurrentCountry.name);

        mMapSource.setText(mapSourceName);

        String sizeText;

        if (progress)
        {
          mProgress.setPending(false);
          mProgress.setProgress(Math.round(mCurrentCountry.progress));
          sizeText = StringUtils.formatUsingSystemLocale("%1$s %2$.2f%%",
              mActivity.getString(R.string.downloader_downloading), mCurrentCountry.progress);
        }
        else
        {
          if (enqueued)
          {
            sizeText = mActivity.getString(R.string.downloader_queued);
            mProgress.setPending(true);
          }
          else
          {
            sizeText = "";
//            sizeText = StringUtils.getFileSizeString(mActivity.getApplicationContext(), mCurrentCountry.totalSize);

            if (shouldAutoDownload &&
                Config.isAutodownloadEnabled() &&
                !sAutodownloadLocked &&
                !failed &&
                ConnectionState.INSTANCE.isWifiConnected())
            {
              Location loc = LocationHelper.from(mActivity).getSavedLocation();
              if (loc != null)
              {
                String country = MapManager.nativeFindCountry(loc.getLatitude(), loc.getLongitude());
                if (TextUtils.equals(mCurrentCountry.id, country) &&
                    MapManager.nativeHasSpaceToDownloadCountry(country))
                {
                  MapManager.nativeDownload(mCurrentCountry.id, getMapSource().getValue());
                }
              }
            }

            mButton.setText(failed ? R.string.downloader_retry
                                   : R.string.download);
          }
        }

        mSize.setText(sizeText);
      }
    }

    UiUtils.showIf(showFrame, mFrame);
  }

  private MapSource getMapSource() {
    MapSource mapSource = MapSource.ORGANIC_MAPS;
    if (downloaderDelegate != null) {
      mapSource = downloaderDelegate.getMainDownloadMapSource(mCurrentCountry);
    }
    return mapSource;
  }

  private int countryItemStatus() {
    if (mCurrentCountry == null) {
      return CountryItem.STATUS_UNKNOWN;
    }
    if (getMapSource() == MapSource.HIKINGBOOK_PRO_MAPS) {
      return mCurrentCountry.hikingbookProMapStatus;
    }
    return mCurrentCountry.status;
  }

  public OnmapDownloader(Fragment fragment)
  {
    mActivity = fragment.getActivity();
    mFrame = fragment.getView().findViewById(R.id.onmap_downloader);
    mParent = mFrame.findViewById(R.id.downloader_parent);
    mTitle = mFrame.findViewById(R.id.downloader_title);
    mMapSource = mFrame.findViewById(R.id.downloader_map_source);
    mSize = mFrame.findViewById(R.id.downloader_size);
    mNumMapLimit = (TextView)mFrame.findViewById(R.id.text_view_num_maps_limit);

    View controls = mFrame.findViewById(R.id.downloader_controls_frame);
    mProgress = controls.findViewById(R.id.wheel_downloader_progress);
    mButton = controls.findViewById(R.id.downloader_button);

    mProgress.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (mCurrentCountry == null)
          return;

        MapManager.nativeCancel(mCurrentCountry.id);
        setAutodownloadLocked(true);
      }
    });
      mButton.setOnClickListener(v -> MapManager.warnOn3g(mActivity, mCurrentCountry == null ? null :
      mCurrentCountry.id, () -> {
      if (downloaderDelegate != null) {
        downloaderDelegate.downloadButtonDidClick(mCurrentCountry);
        return;
      }

      if (mCurrentCountry == null)
        return;

      boolean retry = (countryItemStatus() == CountryItem.STATUS_FAILED);
      if (retry)
      {
        DownloaderNotifier.cancelNotification(mActivity.getApplicationContext());
        MapManager.nativeRetry(mCurrentCountry.id, getMapSource().getValue());
      }
      else
      {
        MapManager.nativeDownload(mCurrentCountry.id, getMapSource().getValue());
//        mActivity.requestPostNotificationsPermission();
      }
    }));

    ViewCompat.setOnApplyWindowInsetsListener(mFrame, (view, windowInsets) -> {
      UiUtils.setViewInsetsPadding(view, windowInsets);
      return windowInsets;
    });
  }

  @Override
  public void onTrackStarted(boolean collapsed) {}

  @Override
  public void onTrackFinished(boolean collapsed) {}

  @Override
  public void onTrackLeftAnimation(float offset)
  {
    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)mFrame.getLayoutParams();
    lp.leftMargin = (int)offset;
    mFrame.setLayoutParams(lp);
  }

  public void onPause()
  {
    if (mStorageSubscriptionSlot > 0)
    {
      MapManager.nativeUnsubscribe(mStorageSubscriptionSlot);
      mStorageSubscriptionSlot = 0;

      MapManager.nativeUnsubscribeOnCountryChanged();
    }
  }

  public void onResume()
  {
    if (mStorageSubscriptionSlot == 0)
    {
      mStorageSubscriptionSlot = MapManager.nativeSubscribe(mStorageCallback);

      MapManager.nativeSubscribeOnCountryChanged(mCountryChangedListener);
    }
  }

  public static void setAutodownloadLocked(boolean locked)
  {
    sAutodownloadLocked = locked;
  }

  public void updateNumMapLimit(boolean isVisible, String text, int color, int backgroundColor) {
    mNumMapLimit.setText(text);
    mNumMapLimit.setTextColor(color);
    mNumMapLimit.setBackgroundColor(backgroundColor);
    if (isVisible) {
      mNumMapLimit.setVisibility(View.VISIBLE);
    }
    else {
      mNumMapLimit.setVisibility(View.GONE);
    }
  }

  public interface IDownloaderDelegate {
    MapSource getMainDownloadMapSource(CountryItem countryItem);

    void downloadButtonDidClick(CountryItem countryItem);
    void onCurrentCountryChanged(CountryItem countryItem);

    String l10nMapSource(MapSource mapSource);
  }
}

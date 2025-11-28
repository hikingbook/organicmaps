// This file is updated for Hikingbook Pro Maps by Zheng-Xiang Ke on 2022.
package app.organicmaps.downloader;

import android.app.Activity;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

import app.organicmaps.MwmActivity;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.sdk.MapSource;
import app.organicmaps.sdk.downloader.CountryItem;
import app.organicmaps.sdk.downloader.MapManager;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.sdk.util.Config;
import app.organicmaps.sdk.util.ConnectionState;
import app.organicmaps.sdk.util.StringUtils;
import app.organicmaps.util.UiUtils;
import app.organicmaps.util.WindowInsetUtils.PaddingInsetsListener;
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
  public CountryItem mCurrentCountry;

  private final MapManager.StorageCallback mStorageCallback = new MapManager.StorageCallback() {
    @Override
    public void onStatusChanged(List<MapManager.StorageCallbackData> data)
    {
      if (mCurrentCountry == null)
        return;

      for (MapManager.StorageCallbackData item : data)
      {
        if (!item.isLeafNode)
          continue;

        if (item.newOrganicMapStatus == CountryItem.STATUS_FAILED || item.newHikingbookProMapStatus == CountryItem.STATUS_FAILED) {
          if (downloaderDelegate != null) {
            downloaderDelegate.handleDownloadError(item, getMapSource());
          }
          else {
            MapManagerHelper.showError(mActivity, item, null);
          }
        }

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

  private final MapManager.CurrentCountryChangedListener mCountryChangedListener =
      new MapManager.CurrentCountryChangedListener() {
        @Override
        public void onCurrentCountryChanged(String countryId)
        {
          mCurrentCountry = (TextUtils.isEmpty(countryId) ? null : CountryItem.fill(countryId));
          updateState(true);
		  if (downloaderDelegate != null) {
        	downloaderDelegate.onCurrentCountryChanged(mCurrentCountry);
      	  }
        }
      };

  public void updateState(boolean shouldAutoDownload)
  {
    updateStateInternal(shouldAutoDownload);
  }

  public Button getDownloadMapButton() {
    return mButton;
  }

  private static boolean isMapDownloading(@Nullable CountryItem country)
  {
    if (country == null)
      return false;

    boolean enqueued = country.status == CountryItem.STATUS_ENQUEUED;
    boolean progress = country.status == CountryItem.STATUS_PROGRESS;
    boolean applying = country.status == CountryItem.STATUS_APPLYING;
    return enqueued || progress || applying;
  }

  public WheelProgressView getProgressView() {
    return mProgress;
  }

  private void updateProgressState(boolean shouldAutoDownload)
  {
    updateStateInternal(shouldAutoDownload);
  }

  private void updateStateInternal(boolean shouldAutoDownload)
  {
    boolean showFrame =
        (mCurrentCountry != null && !mCurrentCountry.present && !RoutingController.get().isNavigating());
    if (showFrame)
    {
      int status = countryItemStatus();
      boolean enqueued = (status == CountryItem.STATUS_ENQUEUED);
      boolean progress = (status == CountryItem.STATUS_PROGRESS
                          || status == CountryItem.STATUS_APPLYING);
      boolean failed = (status == CountryItem.STATUS_FAILED);

      showFrame = (enqueued || progress || failed || status == CountryItem.STATUS_DOWNLOADABLE);

      if (showFrame)
      {
        boolean hasParent = !CountryItem.isRoot(mCurrentCountry.topmostParentId);
        MapSource mapSource = getMapSource();

        String mapSourceName = "";
        if (downloaderDelegate != null) {
          mapSourceName = downloaderDelegate.l10nMapSource(mapSource);
        }

        boolean isDownloading = progress || enqueued;
        UiUtils.showIf(!mapSourceName.isBlank() && isDownloading, mMapSource);
        UiUtils.showIf(isDownloading, mSize);
        UiUtils.showIf(isDownloading, mProgress);
        UiUtils.showIf(!isDownloading, mButton);
        UiUtils.showIf(hasParent, mParent);

        if (hasParent)
          mParent.setText(mCurrentCountry.topmostParentName);

        mTitle.setText(mCurrentCountry.name);

        mMapSource.setText(mapSourceName);
        if (mapSource == MapSource.HIKINGBOOK_PRO_MAPS) {
            mMapSource.setTextColor(ContextCompat.getColor(mActivity, R.color.pro_blue));
        }
        else {
            mMapSource.setTextColor(ContextCompat.getColor(mActivity, R.color.text_body));
        }

        String sizeText;

        if (progress)
        {
          mProgress.setPending(false);
          mProgress.setProgress(Math.round(mCurrentCountry.progress));
          sizeText = mActivity.getString(R.string.downloader_downloading) + " "
                   + StringUtils.formatPercent(mCurrentCountry.progress / 100, true);
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

            if (shouldAutoDownload && Config.isAutodownloadEnabled() && !sAutodownloadLocked && !failed
                && ConnectionState.INSTANCE.isWifiConnected())
            {
              Location loc = MwmApplication.from(mActivity).getLocationHelper().getSavedLocation();
              if (loc != null)
              {
                String country = MapManager.nativeFindCountry(loc.getLatitude(), loc.getLongitude());
                if (TextUtils.equals(mCurrentCountry.id, country)
                    && MapManager.nativeHasSpaceToDownloadCountry(country))
                {
                  MapManagerHelper.startDownload(mCurrentCountry.id, mapSource);
                }
              }
            }

            mButton.setText(failed ? R.string.downloader_retry : R.string.download);
          }
        }

        mSize.setText(sizeText);
      }
    }

    UiUtils.showIf(showFrame, mFrame);
    if (downloaderDelegate != null) {
      downloaderDelegate.onCountryStateChanged(this, showFrame, mCurrentCountry);
    }
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

    mProgress.setOnClickListener(v -> {
      if (mCurrentCountry == null)
        return;

      if (downloaderDelegate != null) {
        downloaderDelegate.cancelDownloadButtonDidClick(mCurrentCountry, getMapSource());
      }
      else {
        MapManager.nativeCancel(mCurrentCountry.id);
      }
      setAutodownloadLocked(true);
    });
    mButton.setOnClickListener(
        v -> {
          if (downloaderDelegate != null) {
            downloaderDelegate.downloadButtonDidClick(mCurrentCountry);
            return;
          }
          MapManagerHelper.warnOn3g(mActivity, mCurrentCountry == null ? null : mCurrentCountry.id, () -> {
            if (mCurrentCountry == null)
              return;

            boolean retry = (countryItemStatus() == CountryItem.STATUS_FAILED);
            if (retry)
            {
              MapManagerHelper.retryDownload(mCurrentCountry.id, getMapSource());
            }
            else
            {
              MapManagerHelper.startDownload(mCurrentCountry.id, getMapSource());
//            mActivity.requestPostNotificationsPermission();
            }
          });
        });

    ViewCompat.setOnApplyWindowInsetsListener(mFrame, PaddingInsetsListener.allSides());
  }

  @Override
  public void onTrackStarted(boolean collapsed)
  {}

  @Override
  public void onTrackFinished(boolean collapsed)
  {}

  @Override
  public void onTrackLeftAnimation(float offset)
  {
    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mFrame.getLayoutParams();
    lp.leftMargin = (int) offset;
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

  public void updateNumMapLimit(boolean isVisible, String text, int color) {
    mNumMapLimit.setText(text);
    mNumMapLimit.setTextColor(color);
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

    void cancelDownloadButtonDidClick(CountryItem countryItem, MapSource mapSource);

    void onCountryStateChanged(OnmapDownloader downloader, Boolean isDownloaderVisible, @Nullable CountryItem countryItem);

    void handleDownloadError(MapManager.StorageCallbackData countryItem, MapSource mapSource);
  }
}

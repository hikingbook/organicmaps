/**
 * Author by robin, Date on 11/30/21.
 * Comment: Instead application by OrganicmapsFrameworkAdapter.INSTANCE.getApplication()
 */
package com.mapswithme.maps.background;

import android.app.Application;

import androidx.annotation.NonNull;

import com.mapswithme.util.OrganicmapsFrameworkAdapter;

public class StubNotificationChannelProvider implements NotificationChannelProvider
{
  private static final String DEFAULT_NOTIFICATION_CHANNEL = "default_notification_channel";

  @NonNull
  private final Application mApplication;

  @NonNull
  private final String mDownloadingChannel;


  StubNotificationChannelProvider(@NonNull Application context, @NonNull String downloadingChannel)
  {
    mApplication = context;
    mDownloadingChannel = downloadingChannel;
  }

  StubNotificationChannelProvider(@NonNull Application context)
  {
    this(context, DEFAULT_NOTIFICATION_CHANNEL);
  }

  @NonNull
  @Override
  public String getDownloadingChannel()
  {
    return mDownloadingChannel;
  }

  @Override
  public void setDownloadingChannel()
  {
    /*Do nothing */
  }

  @NonNull
  protected Application getApplication()
  {
    return OrganicmapsFrameworkAdapter.INSTANCE.getApplication();
  }
}

// This file is created by Zheng-Xiang Ke on 2025.
package app.organicmaps.sdk;

import androidx.annotation.Keep;

public interface ViewportListener
{
  // Called from JNI
  @Keep
  @SuppressWarnings("unused")
  void onViewportChanged();
}

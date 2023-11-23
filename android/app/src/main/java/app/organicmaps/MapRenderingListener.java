/**
 * Author by robin, Date on 11/30/21.
 * Comment: Public interface
 */
package app.organicmaps;

import androidx.annotation.Keep;

public interface MapRenderingListener
{
  default void onRenderingCreated() {}

  default void onRenderingRestored() {}

  // Called from JNI.
  @Keep
  @SuppressWarnings("unused")
  default void onRenderingInitializationFinished() {}
}

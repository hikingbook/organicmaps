/**
 * Author by robin, Date on 11/30/21.
 * Comment: Public interface
 */
package app.organicmaps;

public interface MapRenderingListener
{
  void onRenderingCreated();
  void onRenderingRestored();
  void onRenderingInitializationFinished();
}

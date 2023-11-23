/**
 * Author by robin, Date on 11/30/21.
 * Comment: Comment unused code
 */

package app.organicmaps.base;

import android.content.Context;

import androidx.fragment.app.Fragment;

import app.organicmaps.util.OrganicmapsFrameworkAdapter;
import app.organicmaps.util.Utils;

public class BaseMwmFragment extends Fragment implements OnBackPressListener
{
  @Override
  public void onAttach(Context context)
  {
    super.onAttach(context);
    if (OrganicmapsFrameworkAdapter.INSTANCE.getFragment() == null) OrganicmapsFrameworkAdapter.INSTANCE.setFragment(this);
    Utils.detachFragmentIfCoreNotInitialized(context, OrganicmapsFrameworkAdapter.INSTANCE.getFragment());
//    Utils.detachFragmentIfCoreNotInitialized(context, this);
  }

  @Override
  public boolean onBackPressed()
  {
    return false;
  }

}

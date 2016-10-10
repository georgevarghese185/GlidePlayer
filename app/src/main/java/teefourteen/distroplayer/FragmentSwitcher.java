package teefourteen.distroplayer;

import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * Created by george on 2/10/16.
 */
public class FragmentSwitcher {
    private Fragment currentlyVisible;
    private FragmentManager fragmentManager;
    private @IdRes int containerViewId;

    public FragmentSwitcher(FragmentManager fragmentManager, @IdRes int containerViewId) {
        this.fragmentManager = fragmentManager;
        this.containerViewId = containerViewId;
    }

    public void switchTo(Fragment fragment, String tag) {
        if(fragment == currentlyVisible)
            return;

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if(currentlyVisible != null)
            transaction.detach(currentlyVisible);

        if(fragmentManager.findFragmentByTag(tag) == null)
            transaction.add(containerViewId,fragment,tag);
        else
            transaction.attach(fragment);
        currentlyVisible = fragment;
        transaction.commit();
    }
}
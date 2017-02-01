package teefourteen.glideplayer.fragments.connectivity;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.fragments.FragmentSwitcher;
import teefourteen.glideplayer.fragments.connectivity.listeners.ConnectionCloseListener;
import teefourteen.glideplayer.fragments.connectivity.listeners.ConnectivitySelectionListener;


public class ConnectivityFragment extends Fragment implements ConnectivitySelectionListener,
        ConnectionCloseListener{
    private FragmentSwitcher connectivityFragmentSwitcher;
    private ConnectivityHomeFragment homeFragment;
    private JoinGroupFragment joinFragment;
    private CreateGroupFragment createFragment;
    private ShareGroup group;
    private static final String JOIN_FRAGMENT_TAG="join_fragment";
    private static final String CREATE_FRAGMENT_TAG="create_fragment";
    private static final String HOME_FRAGMENT_TAG="home_fragment";

    public ConnectivityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_connectivity, container, false);
        if(homeFragment == null) {
            homeFragment = new ConnectivityHomeFragment();
            homeFragment.setConnectivitySelectionListener(this);
        }
        if(connectivityFragmentSwitcher == null) {
            connectivityFragmentSwitcher = new FragmentSwitcher(getFragmentManager(),
                    R.id.fragment_connectivity_main_container);
            connectivityFragmentSwitcher.switchTo(homeFragment, HOME_FRAGMENT_TAG);
        }
        else {
            connectivityFragmentSwitcher.reattach();
        }
        return rootView;
    }

    @Override
    public void OnJoinGroupSelected(String username) {
        joinFragment = JoinGroupFragment.newInstance(new ShareGroup(getActivity(), username,
                        new ShareGroup.ShareGroupInitListener() {
                            @Override
                            public void onShareGroupReady() {
                                connectivityFragmentSwitcher.switchTo(joinFragment, JOIN_FRAGMENT_TAG);
                            }
                        }),
                this);
    }

    @Override
    public void OnCreateGroupSelected(String username) {
        createFragment = CreateGroupFragment.newInstance(new ShareGroup(getActivity(), username,
                new ShareGroup.ShareGroupInitListener() {
                    @Override
                    public void onShareGroupReady() {
                        connectivityFragmentSwitcher.switchTo(createFragment,CREATE_FRAGMENT_TAG);
                    }
                }),
                this);
    }

    @Override
    public void onConnectionClose() {
        connectivityFragmentSwitcher.switchTo(homeFragment, HOME_FRAGMENT_TAG, true);
    }
}

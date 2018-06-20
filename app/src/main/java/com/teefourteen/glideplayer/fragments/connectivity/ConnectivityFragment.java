/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.fragments.connectivity;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.Group;
import com.teefourteen.glideplayer.fragments.FragmentSwitcher;
import com.teefourteen.glideplayer.fragments.connectivity.listeners.ConnectionCloseListener;
import com.teefourteen.glideplayer.fragments.connectivity.listeners.ConnectivitySelectionListener;


public class ConnectivityFragment extends Fragment implements ConnectivitySelectionListener,
        ConnectionCloseListener{
    public static final String LAST_USED_USERNAME_KEY = "last_used_username";
    public static final String LAST_USED_GROUP_NAME_KEY = "last_used_group_name";

    private FragmentSwitcher connectivityFragmentSwitcher;
    private ConnectivityHomeFragment homeFragment;
    private JoinGroupFragment joinFragment;
    private CreateGroupFragment createFragment;
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
            homeFragment = ConnectivityHomeFragment.newInstance(this);
        }
        if(connectivityFragmentSwitcher == null) {
            connectivityFragmentSwitcher = new FragmentSwitcher(getFragmentManager(),
                    R.id.fragment_connectivity_main_container);

            Group group;
            if((group = Group.getInstance()) != null) {
                if(group.getMode() == Group.Mode.CREATE_GROUP) {
                    createFragment = CreateGroupFragment.newInstance(group, this);
                    connectivityFragmentSwitcher.switchTo(createFragment, CREATE_FRAGMENT_TAG);
                } else {
                    joinFragment = JoinGroupFragment.newInstance(group, this);
                    connectivityFragmentSwitcher.switchTo(joinFragment, JOIN_FRAGMENT_TAG);
                }
            } else {
                connectivityFragmentSwitcher.switchTo(homeFragment, HOME_FRAGMENT_TAG);
            }
        }
        else {
            connectivityFragmentSwitcher.reattach();
        }

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.connectivity_toolbar);
        AppCompatActivity mainActivity = (AppCompatActivity)getActivity();

        mainActivity.setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) mainActivity.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(mainActivity,
                drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        return rootView;
    }

    @Override
    public void OnJoinGroupSelected(String username) {
        joinFragment = JoinGroupFragment.newInstance(new Group(getActivity(), username,
                Group.Mode.JOIN_GROUP), this);

        connectivityFragmentSwitcher.switchTo(joinFragment, JOIN_FRAGMENT_TAG);
    }

    @Override
    public void OnCreateGroupSelected(String username) {
        createFragment = CreateGroupFragment.newInstance(new Group(getActivity(), username,
                Group.Mode.CREATE_GROUP), this);

        connectivityFragmentSwitcher.switchTo(createFragment,CREATE_FRAGMENT_TAG);
    }

    @Override
    public void onConnectionClose() {
        connectivityFragmentSwitcher.switchTo(homeFragment, HOME_FRAGMENT_TAG, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        connectivityFragmentSwitcher.getCurrentFragment().onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        connectivityFragmentSwitcher.getCurrentFragment().onResume();
    }
}

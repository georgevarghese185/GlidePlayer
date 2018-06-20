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

package com.teefourteen.glideplayer.fragments.library;


import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.Group;
import com.teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import com.teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;

public abstract class LibraryFragment extends Fragment implements GroupMemberListener,
        GroupConnectionListener {
    protected boolean fragmentInitialized = false;
    protected View rootView;
    private ArrayAdapter<String> memberListAdapter;
    private Spinner librarySpinner;

    public LibraryFragment() {
    }

    public interface LibraryChangedListener {
        void onLibraryChanged(Cursor newCursor);
    }

    public interface CloseCursorsListener {
        void closeCursors();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflateRootView(inflater, container);

        librarySpinner = (Spinner) rootView.findViewById(R.id.library_spinner);

        initSpinner();

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.library_toolbar);
        AppCompatActivity mainActivity = (AppCompatActivity)getActivity();

        mainActivity.setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        if(drawerLayout != null) {
            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(mainActivity,
                    drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);

            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();
        }

        initializeContent();

        fragmentInitialized = true;

        return rootView;
    }

    private void initSpinner() {

        final Group group;
        if((group = Group.getInstance()) != null) {


            group.registerGroupMemberListener(this);
            group.registerGroupConnectionListener(this);

            if (rootView.findViewById(R.id.library_spinner) == null) {
                ((ViewGroup) rootView.findViewById(R.id.library_app_bar))
                        .addView(librarySpinner, 1);
            }

            memberListAdapter = new ArrayAdapter<>(getContext(),
                    R.layout.library_spinner_item, group.getMemberList());

            librarySpinner.setAdapter(memberListAdapter);

            librarySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private int lastPosition = 0;

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                                           long id) {
                    if (position == lastPosition) {
                        return;
                    } else {
                        lastPosition = position;
                    }
                    String name = group.getMemberList().get(position);

                    if (name.equals(Group.userName)) {
                        name = null;
                    }

                    if (fragmentInitialized) {
                        libraryChanged(name);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            ((ViewGroup)rootView.findViewById(R.id.library_app_bar)).removeView(librarySpinner);
            if(fragmentInitialized) {
                libraryChanged(null);
            }
        }
    }

    abstract View inflateRootView(LayoutInflater inflater, ViewGroup container);

    abstract void initializeContent();

    abstract public void libraryChanged(String userName);

    @Override
    public void onPause() {
        super.onPause();

        Group group;
        if((group = Group.getInstance()) != null) {
            group.unregisterGroupMemberListener(this);
            group.unregisterGroupConnectionListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Group group;
        if((group = Group.getInstance()) != null) {
            group.registerGroupMemberListener(this);
            group.registerGroupConnectionListener(this);

            memberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConnectionSuccess(String connectedGroup) {
        initSpinner();
    }

    @Override
    public void onNewMemberJoined(String memberId, String memberName) {
        if(memberListAdapter != null) {
            memberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMemberLeft(String member) {
        if(memberListAdapter != null) {
            memberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onOwnerDisconnected() {
        if(memberListAdapter != null) {
            memberListAdapter.notifyDataSetChanged();
        }
    }


    //not needed
    @Override
    public void onConnectionFailed(String failureMessage) {}
    @Override
    public void onExchangingInfo() {}
}

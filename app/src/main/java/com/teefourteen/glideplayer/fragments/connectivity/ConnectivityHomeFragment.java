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


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.fragments.connectivity.listeners.ConnectivitySelectionListener;


public class ConnectivityHomeFragment extends Fragment {
    private View parentView;
    ConnectivitySelectionListener connectivitySelectionListener;

    public ConnectivityHomeFragment() {
        // Required empty public constructor
    }

    public static ConnectivityHomeFragment newInstance(ConnectivitySelectionListener listener) {
        ConnectivityHomeFragment fragment = new ConnectivityHomeFragment();
        fragment.connectivitySelectionListener = listener;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        parentView = inflater.inflate(R.layout.fragment_connectivity_home, container, false);
        Button button =(Button) parentView.findViewById(R.id.join_group_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinGroup(v);
            }
        });
        button = (Button) parentView.findViewById(R.id.create_group_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createGroup(v);
            }
        });

        SharedPreferences preferences =
                getContext().getSharedPreferences(Global.SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        String lastUsername = preferences.getString(ConnectivityFragment.LAST_USED_USERNAME_KEY, null);

        if(lastUsername != null) {
            ((EditText) parentView.findViewById(R.id.username)).setText(lastUsername);
        }

        return parentView;
    }

    private void joinGroup(View view) {
        String username = checkUsername();
        if(username!=null){
            connectivitySelectionListener.OnJoinGroupSelected(username);
        }
    }

    private void createGroup(View view) {
        String username = checkUsername();
        if(username != null) {
            connectivitySelectionListener.OnCreateGroupSelected(username);
        }
    }

    String checkUsername() {
        String username =((EditText) parentView.findViewById(R.id.username)).getText().toString();
        if(username.isEmpty()) {
            TextView caption = (TextView) parentView.findViewById(R.id.username_caption);
            caption.setText("Cannot be empty!");
            caption.setTextColor(Color.RED);
            return null;
        } else {
            hideKeyboard();
            SharedPreferences.Editor editor = getActivity()
                    .getSharedPreferences(Global.SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();

            editor.putString(ConnectivityFragment.LAST_USED_USERNAME_KEY, username);
            editor.apply();

            return username;
        }
    }

    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}

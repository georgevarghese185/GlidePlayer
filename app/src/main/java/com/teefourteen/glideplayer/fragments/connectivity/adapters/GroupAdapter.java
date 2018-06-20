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

package com.teefourteen.glideplayer.fragments.connectivity.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.Group;


public class GroupAdapter extends ArrayAdapter<Group.GlidePlayerGroup> {
    ArrayList<Group.GlidePlayerGroup> groups;


    public GroupAdapter(Context context, ArrayList<Group.GlidePlayerGroup> groups){
        super(context, R.layout.available_group, groups);
        this.groups = groups;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if(v == null) {
            LayoutInflater inflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.available_group, parent, false);
        }

        Group.GlidePlayerGroup group = groups.get(position);
        if(group != null) {
            TextView textView = (TextView)v.findViewById(R.id.group_name);
            textView.setText(group.groupName);
            textView = (TextView)v.findViewById(R.id.owner);
            textView.setText(group.owner.name);
            textView = (TextView)v.findViewById(R.id.device_name);
            textView.setText(group.owner.deviceName);
            textView = (TextView)v.findViewById(R.id.no_of_members);
            textView.setText(String.valueOf(group.getMemberCount()));
        }
        return v;
    }
}

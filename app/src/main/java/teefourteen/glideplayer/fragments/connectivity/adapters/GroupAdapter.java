package teefourteen.glideplayer.fragments.connectivity.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.zip.Inflater;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.connectivity.ShareGroup;


public class GroupAdapter extends ArrayAdapter<ShareGroup.AvailableGroup> {
    ArrayList<ShareGroup.AvailableGroup> groups;


    public GroupAdapter(Context context, ArrayList<ShareGroup.AvailableGroup> groups){
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

        ShareGroup.AvailableGroup group = groups.get(position);
        if(group != null) {
            TextView textView = (TextView)v.findViewById(R.id.group_name);
            textView.setText(group.groupName);
            textView = (TextView)v.findViewById(R.id.owner);
            textView.setText(group.ownerName);
            textView = (TextView)v.findViewById(R.id.device_name);
            textView.setText(group.deviceName);
            textView = (TextView)v.findViewById(R.id.no_of_members);
            textView.setText(String.valueOf(group.memberCount));
        }
        return v;
    }
}

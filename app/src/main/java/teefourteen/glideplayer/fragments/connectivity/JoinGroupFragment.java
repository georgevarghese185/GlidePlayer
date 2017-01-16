package teefourteen.glideplayer.fragments.connectivity;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.fragments.connectivity.adapters.GroupAdapter;
import teefourteen.glideplayer.fragments.connectivity.listeners.ConnectionCloseListener;


public class JoinGroupFragment extends Fragment {
    private View rootView;
    private ShareGroup group;
    private ConnectionCloseListener closeListener;

    public JoinGroupFragment() {
        // Required empty public constructor
    }

    public static JoinGroupFragment newInstance(ShareGroup group,
                                                ConnectionCloseListener closeListener) {
        JoinGroupFragment fragment = new JoinGroupFragment();
        fragment.group = group;
        fragment.closeListener = closeListener;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_join_group, container, false);
        setupGroupList();

        rootView.findViewById(R.id.close_connection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeConnection();
            }
        });

        return rootView;
    }

    private void setupGroupList() {
        GroupAdapter adapter = new GroupAdapter(getContext(), group.getGroupList());
        ListView groupList =(ListView) rootView.findViewById(R.id.group_list);
        groupList.setAdapter(adapter);
        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               //TODO: Connect to group
            }
        });
        group.findGroups(adapter);
    }

    private void closeConnection() {
        group.stopFindingGroups();
        group = null;
        closeListener.onConnectionClose();
    }
}

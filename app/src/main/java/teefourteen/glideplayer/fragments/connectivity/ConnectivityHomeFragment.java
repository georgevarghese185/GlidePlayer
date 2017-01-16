package teefourteen.glideplayer.fragments.connectivity;


import android.content.Context;
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

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.fragments.connectivity.listeners.ConnectivitySelectionListener;


public class ConnectivityHomeFragment extends Fragment {
    private View parentView;
    ConnectivitySelectionListener connectivitySelectionListener;

    public ConnectivityHomeFragment() {
        // Required empty public constructor
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
        return parentView;
    }

    void setConnectivitySelectionListener(ConnectivitySelectionListener listener) {
        connectivitySelectionListener = listener;
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

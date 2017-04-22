package com.teefourteen.glideplayer.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

/**
 * Created by George on 4/17/2017.
 */

public class QuitSyncSessionDialog extends DialogFragment {
    private UserOptionListener listener;

    public interface UserOptionListener {
        void okay();
    }

    public static QuitSyncSessionDialog newInstance(UserOptionListener listener) {
        QuitSyncSessionDialog dialog = new QuitSyncSessionDialog();
        dialog.listener = listener;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Are you sure you want to exit this session?");
        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.okay();
            }
        });
        builder.setNegativeButton("Cancel", null);
        return builder.create();
    }
}

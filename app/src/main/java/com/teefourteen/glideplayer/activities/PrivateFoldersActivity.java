package com.teefourteen.glideplayer.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PrivateFoldersActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    public static final String PRIVATE_FOLDERS_KEY = "private_folders";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_folders);

        Toolbar toolbar = (Toolbar) findViewById(R.id.private_folders_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar =  getSupportActionBar();

        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle("Set Private Folders");
            toolbar.setTitleTextColor(Color.WHITE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.private_folders_menu, menu);
        return true;
    }

    private ArrayList<String> getList(SharedPreferences sharedPreferences) {
        try {

            JSONArray folders = new JSONArray(sharedPreferences.getString(PRIVATE_FOLDERS_KEY, null));
            ArrayList<String> list = new ArrayList<>(folders.length());
            for (int i = 0; i < folders.length(); i++) {
                list.add(folders.getString(i));
            }
            return list;
        } catch (JSONException e) {
            return new ArrayList<String>();
        }
    }

    private JSONArray listToJson(ArrayList<String> list) {
        JSONArray jsonArray = new JSONArray();
        for(String string : list) {
            jsonArray.put(string);
        }
        return jsonArray;
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences sharedPreferences = getSharedPreferences(Global.SHARED_PREFS_NAME, MODE_PRIVATE);
        if (sharedPreferences.contains(PRIVATE_FOLDERS_KEY)) {
            final ArrayList<String> list = getList(sharedPreferences);

            ListView privateFoldersList = (ListView) findViewById(R.id.private_folders_list);

            final PrivateFoldersAdapter adapter = new PrivateFoldersAdapter(this, list);
            privateFoldersList.setAdapter(adapter);

            privateFoldersList.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                            DeleteDirectoryDialog.newInstance(new Runnable() {
                                @Override
                                public void run() {
                                    list.remove(position);
                                    sharedPreferences.edit().putString(PRIVATE_FOLDERS_KEY, listToJson(list).toString()).commit();
                                    adapter.notifyDataSetChanged();
                                }
                            }).show(PrivateFoldersActivity.this.getFragmentManager(), "delete_directory_dialog");
                        }
                    }
            );
        }
    }

    private class PrivateFoldersAdapter extends ArrayAdapter<String> {
        ArrayList<String> list;

        PrivateFoldersAdapter(Context context, ArrayList<String> list) {
            super(context, R.layout.directory, list);
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.directory, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(R.id.dir_path);
            textView.setText(list.get(position));

            textView = (TextView) convertView.findViewById(R.id.dir_name);
            String s[] = list.get(position).split("/");
            textView.setText(s[s.length-1]);

            return convertView;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.add_private_folder) {
            final Intent i = new Intent(this, FilePickerActivity.class);

            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);

            InternalExternalDialog dialog = InternalExternalDialog.newInstance(
                    new InternalExternalDialog.ChoiceListener() {
                @Override
                public void internal() {
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory());
                    startActivityForResult(i, REQUEST_CODE);
                }

                @Override
                public void external() {
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, "/storage/");
                    startActivityForResult(i, REQUEST_CODE);
                }
            });

            dialog.show(getFragmentManager(), "private_folders_picker");

            return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<Uri> files = Utils.getSelectedFilesFromResult(data);

            SharedPreferences sharedPreferences = getSharedPreferences(Global.SHARED_PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            JSONArray folders;
            try {
                if (sharedPreferences.contains(PRIVATE_FOLDERS_KEY)) {
                    folders = new JSONArray(sharedPreferences.getString(PRIVATE_FOLDERS_KEY, null));
                } else {
                    folders = new JSONArray();
                }

                for (Uri uri : files) {
                    File file = Utils.getFileForUri(uri);
                    folders.put(file.getAbsolutePath());
                }

                editor.putString(PRIVATE_FOLDERS_KEY, folders.toString());
                editor.apply();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static class InternalExternalDialog extends DialogFragment {
        private ChoiceListener listener;

        public interface ChoiceListener {
            void internal();
            void external();
        }

        public static InternalExternalDialog newInstance(ChoiceListener listener) {
            InternalExternalDialog dialog = new InternalExternalDialog();
            dialog.listener = listener;
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Pick directories from Internal or External Storage?");
            builder.setPositiveButton("Internal", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.internal();
                }
            });
            builder.setNegativeButton("External", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.external();
                }
            });
            return builder.create();
        }
    }

    public static class DeleteDirectoryDialog extends DialogFragment {
        Runnable confirm;

        public static DeleteDirectoryDialog newInstance(Runnable confirm) {
            DeleteDirectoryDialog dialog = new DeleteDirectoryDialog();
            dialog.confirm = confirm;
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Make this directory public to anyone in a group?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    confirm.run();
                }
            });
            builder.setNegativeButton("Cancel", null);
            return builder.create();
        }
    }
}

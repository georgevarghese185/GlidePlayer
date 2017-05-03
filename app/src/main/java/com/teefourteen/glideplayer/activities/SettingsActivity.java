package com.teefourteen.glideplayer.activities;

import android.content.Intent;
import android.graphics.Color;
import android.preference.Preference;
import com.teefourteen.glideplayer.fragments.SupportPreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.fragments.FragmentSwitcher;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFERENCES_FRAGMENT_TAG = "preferences_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar =  getSupportActionBar();

        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle("Settings");
            toolbar.setTitleTextColor(Color.WHITE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportFragmentManager().beginTransaction().add(R.id.settings_main_container,
                new Preferences(), PREFERENCES_FRAGMENT_TAG).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .remove(fragmentManager.findFragmentByTag(PREFERENCES_FRAGMENT_TAG)).commit();
    }

    public static class Preferences extends SupportPreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            findPreference("private_folders").setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), PrivateFoldersActivity.class));
                    return true;
                }
            });
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.private_folders_menu,menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if(item.getItemId() == R.id.add_private_folder) {
                return true;
            } else {
                return false;
            }
        }
    }
}

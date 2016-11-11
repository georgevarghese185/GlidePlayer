package teefourteen.glideplayer.activities;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import teefourteen.glideplayer.CustomToolbarOptions;
import teefourteen.glideplayer.fragments.FragmentSwitcher;
import teefourteen.glideplayer.fragments.LibraryFragment;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.music.PlayQueue;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_PLAY_QUEUE = "play_queue";
    private LibraryFragment libraryFragment;
    private String LIBRARY_FRAGMENT_TAG ="SONGS";
    private FragmentSwitcher mainFragmentSwitcher;
    public static PlayQueue globalPlayQueue = null;
    public CustomToolbarOptions toolbarOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        showDrawerToggle(true);
        toolbarOptions = new CustomToolbarOptions(this) {
            @Override
            public void reInflateMenu() {
                invalidateOptionsMenu();
            }

            @Override
            public void resetToolbar() {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                showDrawerToggle(true);
            }
        };

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mainFragmentSwitcher = new FragmentSwitcher(getSupportFragmentManager(), R.id.main_container);
        libraryFragment = new LibraryFragment();

        mainFragmentSwitcher.switchTo(libraryFragment, LIBRARY_FRAGMENT_TAG);
    }

    private void showDrawerToggle(boolean showToggle) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if(showToggle) {

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();
        }
        else {
            toolbar.setNavigationIcon(null);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu (getMenuInflater().inflate()); this adds items to the action bar if it is present.
        int result = toolbarOptions.getCurrentMenu();
        if (result == -1) {
            return false;
        }
        else {
            getMenuInflater().inflate(toolbarOptions.getCurrentMenu(), menu);
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else toolbarOptions.handleOption(id);
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_library) {

        } else if (id == R.id.nav_settings) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

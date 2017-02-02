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

import teefourteen.glideplayer.ToolbarEditor;
import teefourteen.glideplayer.fragments.FragmentSwitcher;
import teefourteen.glideplayer.fragments.connectivity.ConnectivityFragment;
import teefourteen.glideplayer.fragments.library.LibraryFragment;
import teefourteen.glideplayer.R;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ToolbarEditor.ToolbarEditable {

    private LibraryFragment libraryFragment;
    private ConnectivityFragment connectivityFragment;
    private static final String LIBRARY_FRAGMENT_TAG ="songs";
    private static final String CONNECTIVITY_FRAGMENT_TAG = "connectivity";
    private FragmentSwitcher mainFragmentSwitcher;
    private ToolbarEditor toolbarEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        showDrawerToggle(true);
        toolbarEditor = new ToolbarEditor(this) {
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
        connectivityFragment = new ConnectivityFragment();

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
        int result = toolbarEditor.getCurrentMenu();
        if (result == -1) {
            return false;
        }
        else {
            getMenuInflater().inflate(toolbarEditor.getCurrentMenu(), menu);
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else toolbarEditor.handleOption(id);
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_library) {
            mainFragmentSwitcher.switchTo(libraryFragment, LIBRARY_FRAGMENT_TAG);
        } else if (id == R.id.connectivity){
            mainFragmentSwitcher.switchTo(connectivityFragment, CONNECTIVITY_FRAGMENT_TAG);
        } else if (id == R.id.nav_settings) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public ToolbarEditor getEditor() {
        return toolbarEditor;
    }
}

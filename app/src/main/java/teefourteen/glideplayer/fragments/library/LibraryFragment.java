package teefourteen.glideplayer.fragments.library;


import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.File;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.activities.PlayerActivity;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import teefourteen.glideplayer.fragments.library.adapters.AlbumAdapter;
import teefourteen.glideplayer.music.PlayQueue;
import teefourteen.glideplayer.fragments.library.adapters.SongAdapter;
import teefourteen.glideplayer.music.database.Library;

import static teefourteen.glideplayer.Global.playQueue;
import static teefourteen.glideplayer.connectivity.ShareGroup.shareGroupWeakReference;

public class LibraryFragment extends Fragment implements GroupMemberListener,
        GroupConnectionListener, SongAdapter.SongClickListener, AlbumAdapter.AlbumClickListener {
    private boolean fragmentInitialized = false;
    private View rootView;
    private ArrayAdapter<String> memberListAdapter;
    private Spinner librarySpinner;
    private SongsFragment songsFragment;
    private AlbumsFragment albumsFragment;
    private static final int NUMBER_OF_TABS = 2;
    private static final int SONGS_TAB_INDEX = 0;
    private static final int ALBUMS_TAB_INDEX = 1;
    private static final String SONGS_TAB_TITLE = "Songs";
    private static final String ALBUMS_TAB_TITLE = "Albums";

    public LibraryFragment() {
    }

    public interface LibraryChangedListener {
        void onLibraryChanged(Cursor newCursor);
    }


    private class LibraryPagerAdapter extends FragmentPagerAdapter {
        LibraryPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == SONGS_TAB_INDEX) {
                return songsFragment;
            } else if(position == ALBUMS_TAB_INDEX){
                return albumsFragment;
            } else {
                return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(position == SONGS_TAB_INDEX) {
                return SONGS_TAB_TITLE;
            } else if(position == ALBUMS_TAB_INDEX){
                return ALBUMS_TAB_TITLE;
            } else {
                return null;
            }
        }

        @Override
        public int getCount() {
            return NUMBER_OF_TABS;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_library, container, false);

        librarySpinner = (Spinner) rootView.findViewById(R.id.library_spinner);

         initSpinner();

        if(!fragmentInitialized) {
            songsFragment = SongsFragment.newInstance(Library.getSongs(null),this);

            albumsFragment = AlbumsFragment.newInstance(Library.getAlbums(null), this);
        }

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.library_toolbar);
        AppCompatActivity mainActivity = (AppCompatActivity)getActivity();

        mainActivity.setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) mainActivity.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(mainActivity,
                drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.library_view_pager);

        viewPager.setAdapter(new LibraryPagerAdapter(getChildFragmentManager()));

        ((TabLayout)rootView.findViewById(R.id.library_tab_layout)).setupWithViewPager(viewPager);

        fragmentInitialized = true;
        return rootView;
    }

    private void initSpinner() {

        if(shareGroupWeakReference != null
                && shareGroupWeakReference.get() != null) {
            final ShareGroup group = shareGroupWeakReference.get();

            group.registerGroupMemberListener(this);
            group.registerGroupConnectionListener(this);

            if (rootView.findViewById(R.id.library_spinner) == null) {
                ((ViewGroup) rootView.findViewById(R.id.library_app_bar))
                        .addView(librarySpinner, 1);
            }

            memberListAdapter = new ArrayAdapter<>(getContext(),
                    R.layout.library_spinner_item, group.getMemberList());

            librarySpinner.setAdapter(memberListAdapter);

            librarySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private int lastPosition = 0;

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                                           long id) {
                    if (position == lastPosition) {
                        return;
                    } else {
                        lastPosition = position;
                    }
                    String name = group.getMemberList().get(position);

                    if (name.equals(ShareGroup.userName)) {
                        name = null;
                    }

                    if (fragmentInitialized) {
                        libraryChanged(name);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            ((ViewGroup)rootView.findViewById(R.id.library_app_bar)).removeView(librarySpinner);
            if(fragmentInitialized) {
                libraryChanged(null);
            }
        }
    }

    private void libraryChanged(String userName) {
        songsFragment.onLibraryChanged(Library.getSongs(userName));
        albumsFragment.onLibraryChanged(Library.getAlbums(userName));
    }

    @Override
    public void onSongClicked(Cursor songCursor, int position) {
        songCursor.moveToPosition(position);

        playQueue = new PlayQueue(songCursor);

        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_PLAY_QUEUE, playQueue);
        getActivity().startActivity(intent);
    }

    @Override
    public void onAlbumClicked(Cursor albumCursor, int position) {

    }

    @Override
    public void onPause() {
        super.onPause();

        if(shareGroupWeakReference != null
                &&shareGroupWeakReference.get() != null) {
            ShareGroup group = shareGroupWeakReference.get();
            group.unregisterGroupMemberListener(this);
            group.unregisterGroupConnectionListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(shareGroupWeakReference != null
                &&shareGroupWeakReference.get() != null) {
            ShareGroup group = shareGroupWeakReference.get();
            group.registerGroupMemberListener(this);
            group.registerGroupConnectionListener(this);

            memberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConnectionSuccess(String connectedGroup) {
        initSpinner();
    }

    @Override
    public void onNewMemberJoined(String memberId, String memberName) {
        if(memberListAdapter != null) {
            memberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMemberLeft(String member) {
        if(memberListAdapter != null) {
            memberListAdapter.notifyDataSetChanged();
        }

        librarySpinner.performClick();
    }

    @Override
    public void onOwnerDisconnected() {
        if(memberListAdapter != null) {
            memberListAdapter.notifyDataSetChanged();
        }
    }


    //not needed
    @Override
    public void onConnectionFailed(String failureMessage) {}
    @Override
    public void onExchangingInfo() {}
}

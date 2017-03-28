package com.teefourteen.glideplayer.fragments.library;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.activities.MainActivity;
import com.teefourteen.glideplayer.activities.PlayerActivity;
import com.teefourteen.glideplayer.connectivity.ShareGroup;
import com.teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import com.teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import com.teefourteen.glideplayer.fragments.library.adapters.AlbumAdapter;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.fragments.library.adapters.SongAdapter;
import com.teefourteen.glideplayer.music.database.AlbumTable;
import com.teefourteen.glideplayer.music.database.Library;

import static com.teefourteen.glideplayer.Global.playQueue;
import static com.teefourteen.glideplayer.connectivity.ShareGroup.shareGroupWeakReference;

public class LibraryFragment extends Fragment implements GroupMemberListener,
        GroupConnectionListener, SongAdapter.SongClickListener, AlbumAdapter.AlbumClickListener {
    private boolean fragmentInitialized = false;
    private View rootView;
    private ArrayAdapter<String> memberListAdapter;
    private Spinner librarySpinner;
    private SongsFragment songsFragment;
    private AlbumsFragment albumsFragment;
    private LibraryPagerAdapter pagerAdapter;
    private Runnable backAction = null;
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

    public interface CloseCursorsListener {
        void closeCursors();
    }


    private class LibraryPagerAdapter extends FragmentStatePagerAdapter {
        FragmentManager fm;
        Fragment songsFragment;
        Fragment albumsFragment;
        Fragment toReplace;

        LibraryPagerAdapter(FragmentManager fm, Fragment songsFragment, Fragment albumsFragment) {
            super(fm);
            this.fm = fm;
            this.songsFragment = songsFragment;
            this.albumsFragment = albumsFragment;
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
        public int getItemPosition(Object object) {
            if(object == toReplace) {
                toReplace = null;
                return POSITION_NONE;
            } else {
                return POSITION_UNCHANGED;
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

        public void changeAlbumsFragment(Fragment fragment) {
            toReplace = albumsFragment;
            fm.beginTransaction().remove(albumsFragment).commit();
            albumsFragment = fragment;
            notifyDataSetChanged();
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
        pagerAdapter = new LibraryPagerAdapter(getChildFragmentManager(), songsFragment, albumsFragment);
        viewPager.setAdapter(pagerAdapter);

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
        if(backAction != null) {
            backAction.run();
        }
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
        String username = null;
        albumCursor.moveToPosition(position);
        if(Library.getInt(albumCursor, AlbumTable.Columns.IS_REMOTE) == 1) {
            username = Library.getString(albumCursor, AlbumTable.Columns.REMOTE_USERNAME);
        }
        Cursor albumSongsCursor = Library.getAlbumSongs(username,
                Library.getLong(albumCursor, AlbumTable.Columns.ALBUM_ID));

        final SongsFragment albumSongsFragment = SongsFragment.newInstance(albumSongsCursor, this);
        pagerAdapter.changeAlbumsFragment(albumSongsFragment);

        final MainActivity mainActivity = (MainActivity) getActivity();

        backAction = new Runnable() {
            @Override
            public void run() {
                backAction = null;
                pagerAdapter.changeAlbumsFragment(albumsFragment);
                albumSongsFragment.closeCursors();
                mainActivity.overrideBackButton(null);
            }
        };

        mainActivity.overrideBackButton(backAction);
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
        songsFragment.closeCursors();
        albumsFragment.closeCursors();
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

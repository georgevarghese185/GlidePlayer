package com.teefourteen.glideplayer.fragments.library;


import android.content.Intent;
import android.database.Cursor;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.activities.MainActivity;
import com.teefourteen.glideplayer.activities.PlayerActivity;
import com.teefourteen.glideplayer.fragments.library.adapters.AlbumAdapter;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.fragments.library.adapters.SongAdapter;
import com.teefourteen.glideplayer.database.AlbumTable;
import com.teefourteen.glideplayer.database.Library;

import static com.teefourteen.glideplayer.Global.playQueue;

public class MusicLibraryFragment extends LibraryFragment implements SongAdapter.SongClickListener,
        AlbumAdapter.AlbumClickListener {
    private SongsFragment songsFragment;
    private AlbumsFragment albumsFragment;
    private LibraryPagerAdapter pagerAdapter;
    private Runnable backAction = null;
    private static final int NUMBER_OF_TABS = 2;
    private static final int SONGS_TAB_INDEX = 0;
    private static final int ALBUMS_TAB_INDEX = 1;
    private static final String SONGS_TAB_TITLE = "Songs";
    private static final String ALBUMS_TAB_TITLE = "Albums";

    private SongAdapter.SongClickListener customSongClickListener = null;
    private AlbumAdapter.AlbumClickListener customAlbumClickListener = null;

    public MusicLibraryFragment() {
    }

    public static MusicLibraryFragment newInstance(SongAdapter.SongClickListener songClickListener,
                                                   AlbumAdapter.AlbumClickListener albumClickListener) {
        MusicLibraryFragment fragment = new MusicLibraryFragment();
        fragment.customAlbumClickListener = albumClickListener;
        fragment.customSongClickListener = songClickListener;
        return fragment;
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
            if (position == SONGS_TAB_INDEX) {
                return songsFragment;
            } else if (position == ALBUMS_TAB_INDEX) {
                return albumsFragment;
            } else {
                return null;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            if (object == toReplace) {
                toReplace = null;
                return POSITION_NONE;
            } else {
                return POSITION_UNCHANGED;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == SONGS_TAB_INDEX) {
                return SONGS_TAB_TITLE;
            } else if (position == ALBUMS_TAB_INDEX) {
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
    View inflateRootView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.fragment_music_library, container, false);
    }

    @Override
    void initializeContent() {
        if (!fragmentInitialized) {
            songsFragment = SongsFragment.newInstance(Library.getSongs(null), this);

            albumsFragment = AlbumsFragment.newInstance(Library.getAlbums(null), this);
        }

        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.library_view_pager);
        pagerAdapter = new LibraryPagerAdapter(getChildFragmentManager(), songsFragment, albumsFragment);
        viewPager.setAdapter(pagerAdapter);

        ((TabLayout) rootView.findViewById(R.id.library_tab_layout)).setupWithViewPager(viewPager);
    }

    @Override
    public void libraryChanged(String userName) {
        songsFragment.onLibraryChanged(Library.getSongs(userName));
        if (backAction != null) {
            backAction.run();
        }
        albumsFragment.onLibraryChanged(Library.getAlbums(userName));
    }

    @Override
    public void onSongClicked(Cursor songCursor, int position) {
        if (customSongClickListener != null) {
            customSongClickListener.onSongClicked(songCursor, position);
        } else {

            songCursor.moveToPosition(position);

            playQueue = new PlayQueue(songCursor);

            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_PLAY_QUEUE, playQueue);
            getActivity().startActivity(intent);
        }
    }

    @Override
    public void onSongLongClicked(Cursor songCursor, int position) {
        if (customSongClickListener != null) {
            customSongClickListener.onSongLongClicked(songCursor, position);
        }
    }

    @Override
    public void onAlbumClicked(Cursor albumCursor, int position) {
        if (customAlbumClickListener != null) {

        } else {

            String username = null;
            albumCursor.moveToPosition(position);
            if (Library.getInt(albumCursor, AlbumTable.Columns.IS_REMOTE) == 1) {
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        songsFragment.closeCursors();
        albumsFragment.closeCursors();
    }
}

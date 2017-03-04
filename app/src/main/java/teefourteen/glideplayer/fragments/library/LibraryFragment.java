package teefourteen.glideplayer.fragments.library;


import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TabHost;

import java.io.File;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.activities.PlayerActivity;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import teefourteen.glideplayer.fragments.FragmentSwitcher;
import teefourteen.glideplayer.music.PlayQueue;
import teefourteen.glideplayer.fragments.library.adapters.SongAdapter;
import teefourteen.glideplayer.music.database.Library;

import static teefourteen.glideplayer.Global.playQueue;
import static teefourteen.glideplayer.connectivity.ShareGroup.shareGroupWeakReference;

public class LibraryFragment extends Fragment implements GroupMemberListener,
        GroupConnectionListener, SongAdapter.SongClickListener {
    private TabHost tabHost;
    private static Fragment instance;
    private boolean fragmentInitialized = false;
    private View rootView;
    private ArrayAdapter<String> memberListAdapter;
    private Spinner librarySpinner;
    private FragmentSwitcher songFragmentSwitcher;
    private SongsFragment songsFragment;
    private FragmentSwitcher albumFragmentSwitcher;
    private AlbumsFragment albumsFragment;
    private static final String SONGS_FRAGMENT_TAG = "songs_fragment";
    private static final String ALBUMS_FRAGMENT_TAG = "albums_fragment";

    public LibraryFragment() {
        instance = this;
    }

    public static Fragment getInstance() {
        return instance;
    }

    public interface LibraryChangedListener {
        void onLibraryChanged(Cursor newCursor);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_library, container, false);

        librarySpinner = (Spinner) rootView.findViewById(R.id.library_spinner);

        tabHost = (TabHost) rootView.findViewById(R.id.libraryTabHost);
        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec("songsTab");
        spec.setContent(R.id.songsTab);
        spec.setIndicator("Songs");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("albumsTab");
        spec.setContent(R.id.albumsTab);
        spec.setIndicator("Albums");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("artistsTab");
        spec.setContent(R.id.artistsTab);
        spec.setIndicator("Artists");
        tabHost.addTab(spec);

        initSpinner();

        if(!fragmentInitialized) {
            SQLiteDatabase libraryDb = new Library(getContext(),
                    new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME))
                    .getReadableDatabase();

            songsFragment = SongsFragment.getInstance(Library.getSongs(libraryDb),this);
            songFragmentSwitcher = new FragmentSwitcher(getFragmentManager(), R.id.songsTab);
            songFragmentSwitcher.switchTo(songsFragment,SONGS_FRAGMENT_TAG);

            albumsFragment = new AlbumsFragment();
            albumFragmentSwitcher = new FragmentSwitcher(getFragmentManager(), R.id.albumsTab);
            albumFragmentSwitcher.switchTo(albumsFragment, ALBUMS_FRAGMENT_TAG);
        } else {
            songFragmentSwitcher.reattach();
            albumFragmentSwitcher.reattach();
        }

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
                ((ViewGroup) rootView.findViewById(R.id.tab_host_root)).addView(librarySpinner);
            }

            memberListAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_dropdown_item, group.getMemberList());

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
                    File dbFile;
                    if (name.equals(ShareGroup.userName)) {
                        dbFile = new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME);
                    } else {
                        dbFile = ShareGroup.getLibraryFile(name);
                    }

                    if (dbFile != null && dbFile.exists() && fragmentInitialized) {
                        libraryChanged(dbFile);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            ((ViewGroup)rootView.findViewById(R.id.tab_host_root)).removeView(librarySpinner);
            if(fragmentInitialized) {
                libraryChanged(new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME));
            }
        }
    }

    private void libraryChanged(File dbFile) {
        SQLiteDatabase libraryDb = new Library(getContext(), dbFile).getReadableDatabase();
        songsFragment.onLibraryChanged(Library.getSongs(libraryDb));
        albumsFragment.onLibraryChanged(Library.getAlbums(libraryDb));
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
        librarySpinner.performClick();
        libraryChanged(new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME));
    }


    //not needed
    @Override
    public void onConnectionFailed(String failureMessage) {}
    @Override
    public void onExchangingInfo() {}
}

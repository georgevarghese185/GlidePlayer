package teefourteen.glideplayer.fragments.library;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;

import java.util.ArrayList;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.activities.PlayerActivity;
import teefourteen.glideplayer.fragments.FragmentSwitcher;
import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.music.PlayQueue;
import teefourteen.glideplayer.fragments.library.adapters.SongAdapter;

public class LibraryFragment extends Fragment {
    private TabHost tabHost;
    private static Fragment instance;
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_library, container, false);


        tabHost = (TabHost) view.findViewById(R.id.libraryTabHost);
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

        if(songsFragment == null) {
            songsFragment = new SongsFragment();
            songsFragment.setupList(new SongAdapter(getActivity(), Global.songCursor,null), true,
                    new SongsFragment.SelectionHandler() {
                        @Override
                        public void handleSelection(PlayQueue playQueue, int position) {
                            Intent intent = new Intent(getContext(), PlayerActivity.class);
                            intent.putExtra(PlayerActivity.EXTRA_PLAY_QUEUE, playQueue);
                            getActivity().startActivity(intent);
                        }
                    });
            songFragmentSwitcher = new FragmentSwitcher(getFragmentManager(), R.id.songsTab);
            songFragmentSwitcher.switchTo(songsFragment,SONGS_FRAGMENT_TAG);
        } else {
            songFragmentSwitcher.reattach();
        }

        if(albumsFragment == null) {
            albumsFragment = new AlbumsFragment();
            albumFragmentSwitcher = new FragmentSwitcher(getFragmentManager(), R.id.albumsTab);
            albumFragmentSwitcher.switchTo(albumsFragment, ALBUMS_FRAGMENT_TAG);
        } else {
            albumFragmentSwitcher.reattach();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        songsFragment.onDestroyView();
        albumsFragment.onDestroyView();
    }
}

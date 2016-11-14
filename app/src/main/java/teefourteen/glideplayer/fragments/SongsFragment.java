package teefourteen.glideplayer.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;

import teefourteen.glideplayer.ToolbarEditor;
import teefourteen.glideplayer.ToolbarEditor.ToolbarEditable;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.music.*;
import teefourteen.glideplayer.music.adapters.SongAdapter;

public class SongsFragment extends Fragment {
    private ListAdapter songAdapter = null;
    private ListView songList = null;
    private boolean allowMultiSelection;
    public interface SelectionHandler{
        public void handleSelection(PlayQueue playQueue, int position);
    }
    SelectionHandler selectionHandler;

    public SongsFragment() {
    }

    public void setupList(ListAdapter adapter, boolean allowMultiSelection,
                          SelectionHandler handler){
        songAdapter = adapter;
        this.allowMultiSelection = allowMultiSelection;
        selectionHandler = handler;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        songList = (ListView) view.findViewById(R.id.songList);
        initSongList();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void initSongList() {
        songList.setAdapter(songAdapter);
        SingleSelect single = new SingleSelect();
        songList.setOnItemClickListener(single);
        if(allowMultiSelection)
            songList.setOnItemLongClickListener(single);
    }

    class SingleSelect implements AdapterView.OnItemClickListener,
            AdapterView.OnItemLongClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Global.songCursor.moveToPosition(position);
            selectionHandler.handleSelection(new PlayQueue(Global.songCursor),position);
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            MultiSelect multi = new MultiSelect(position, parent.getAdapter().getCount());
            parent.setOnItemClickListener(multi);
            parent.setOnItemLongClickListener(null);
            ((SongAdapter)songAdapter).setChecker(multi);
            ((SongAdapter)songAdapter).colorBackground(view, getActivity(), position);
            ToolbarEditor editor = ((ToolbarEditable)getActivity()).getEditor();
            editor.registerMenu(R.menu.multi_select, MultiSelect.MENU_NAME, multi);
            editor.changeToolbar(MultiSelect.MENU_NAME);
            return true;
        }
    }

    public class MultiSelect implements AdapterView.OnItemClickListener,
            SongAdapter.SelectionChecker, ToolbarEditor.MenuHandler {
        public static final String MENU_NAME ="multi_select_menu";
        private BitSet selected;
        private ArrayList<Integer> selectionList;
        private final static String SELECTION_LIST_HANDLER_THREAD = "selection_list_handler_thread";
        private Handler selectionListHandler;
        private HandlerThread selectionHandlerThread;

        MultiSelect(int position, int listSize) {
            selectionList = new ArrayList<>();
            selectionList.add(position);
            selected = new BitSet(listSize);
            selected.set(position);
            selectionHandlerThread = new HandlerThread(SELECTION_LIST_HANDLER_THREAD);
            selectionHandlerThread.start();
            selectionListHandler = new Handler(selectionHandlerThread.getLooper());
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            selected.flip(position);

            if(selected.get(position)) {
                selectionListHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        selectionList.add(position);
                        Collections.sort(selectionList, new SelectionListComparator());
                    }
                });
                ((SongAdapter)songAdapter).colorBackground(view, getActivity(), position);
            }
            else {
                selectionListHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        selectionList.remove(Integer.valueOf(position));
                        Collections.sort(selectionList, new SelectionListComparator());
                    }
                });
                ((SongAdapter)songAdapter).colorBackground(view, getActivity(), position);
            }

        }

        public void cancelMultiSelect() {
            SingleSelect single = new SingleSelect();
            songList.setOnItemClickListener(single);
            songList.setOnItemLongClickListener(single);
            selected = null;
            selectionList = null;
            selectionListHandler = null;
            selectionHandlerThread.quit();
            selectionHandlerThread = null;
            ((SongAdapter)songAdapter).setChecker(null);
            ((SongAdapter)songAdapter).notifyDataSetChanged();
        }

        @Override
        public boolean isSelected(int position) {
            return (selected.get(position));
        }

        @Override
        public void setupToolbar(AppCompatActivity activity) {
            activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            toolbar.setNavigationIcon(null);
        }

        @Override
        public void handleOption(int optionItemId) {
            if(optionItemId == R.id.cancel_multi_select){
                ToolbarEditor editor = ((ToolbarEditable) getActivity()).getEditor();
                editor.returnToNormal();
                editor.unregisterMenu(MENU_NAME);
                cancelMultiSelect();
            }
            else if(optionItemId == R.id.queue_and_play){
                selectionHandler.handleSelection(new PlayQueue(Global.songCursor, selectionList),0);
            }
        }

        private class SelectionListComparator implements Comparator<Integer> {
            @Override
            public int compare(Integer o1, Integer o2) {
                if(o1<o2) return -1;
                else if(o1.equals(o2)) return 0;
                else return 1;
            }
        }
    }
}

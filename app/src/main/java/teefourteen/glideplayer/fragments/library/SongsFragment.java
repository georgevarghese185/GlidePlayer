package teefourteen.glideplayer.fragments.library;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;

import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.ToolbarEditor;
import teefourteen.glideplayer.ToolbarEditor.ToolbarEditable;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import teefourteen.glideplayer.music.*;
import teefourteen.glideplayer.fragments.library.adapters.SongAdapter;
import teefourteen.glideplayer.music.database.Library;

import static teefourteen.glideplayer.connectivity.ShareGroup.shareGroupWeakReference;

public class SongsFragment extends Fragment implements GroupMemberListener,
        GroupConnectionListener {
    private ListAdapter songAdapter = null;
    private View rootView;
    private ArrayAdapter<String> memberListAdapter;
    private Spinner librarySpinner;
    private ListView songListView = null;
    private boolean allowMultiSelection;

    public interface SelectionHandler{
        void handleSelection(PlayQueue playQueue, int position);
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
        rootView = inflater.inflate(R.layout.fragment_songs, container, false);

        songListView = (ListView) rootView.findViewById(R.id.songList);
        initSongList();

        librarySpinner = (Spinner) rootView.findViewById(R.id.library_spinner);

        if(shareGroupWeakReference != null
            && shareGroupWeakReference.get() != null) {
            ShareGroup group = shareGroupWeakReference.get();

                group.registerGroupMemberListener(this);
                group.registerGroupConnectionListener(this);
                group.registerGroupConnectionListener(onlyDisconnectionListener);

            if(group.isConnected()) {
                initSpinner(librarySpinner, group.getMemberList());
            } else {
                ((ViewGroup) rootView).removeView(librarySpinner);
            }
        } else {
            ((ViewGroup) rootView).removeView(librarySpinner);
            changeLibraryDb(new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME));
        }

        return rootView;
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
        if(songAdapter instanceof SongAdapter) {
            ((SongAdapter) songAdapter).setForRecycling(songListView);
        }
        songListView.setAdapter(songAdapter);
        SingleSelect single = new SingleSelect();
        songListView.setOnItemClickListener(single);
        if(allowMultiSelection)
            songListView.setOnItemLongClickListener(single);
    }

    private void initSpinner(Spinner librarySpinner, final ArrayList<String> memberList) {
        if(((ViewGroup) rootView).findViewById(R.id.library_spinner) == null) {
            ((ViewGroup) rootView).addView(librarySpinner);
        }

        memberListAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, memberList);

        librarySpinner.setAdapter(memberListAdapter);

        librarySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int lastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == lastPosition) {
                    return;
                } else {
                    lastPosition = position;
                }
                String name = memberList.get(position);
                File dbFile;
                if (name.equals(ShareGroup.userName)) {
                    dbFile = new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME);
                } else {
                    dbFile = ShareGroup.getLibraryFile(name);
                }

                if (dbFile != null && dbFile.exists()) {
                    changeLibraryDb(dbFile);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void changeLibraryDb(File dbFile) {
        Library library = new Library(getContext(), dbFile);
        SQLiteDatabase db = library.getReadableDatabase();

        Global.songCursor = Library.getSongs(db);

        if(songAdapter instanceof SongAdapter) {
            ((SongAdapter) songAdapter).changeCursor(Global.songCursor);
            ((SongAdapter) songAdapter).notifyDataSetChanged();
        }
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
            songListView.setOnItemClickListener(single);
            songListView.setOnItemLongClickListener(single);
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

    @Override
    public void onMemberLeft(String member) {
        librarySpinner.setSelection(0);
        librarySpinner.performClick();
        if(memberListAdapter != null) {
            memberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onNewMemberJoined(String memberId, String member) {
        if(memberListAdapter != null) {
            memberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConnectionSuccess(String connectedGroup) {
        initSpinner(librarySpinner, shareGroupWeakReference.get().getMemberList());
    }

    //blank because onlyDisconnectionListener will handle disconnections.
    @Override
    public void onOwnerDisconnected() {}

    //this listener stays registered at all times. Even when fragment detached.
    GroupConnectionListener onlyDisconnectionListener = new GroupConnectionListener() {
        @Override
        public void onExchangingInfo() {}
        @Override
        public void onConnectionSuccess(String connectedGroup) {}
        @Override
        public void onConnectionFailed(String failureMessage) {}
        @Override
        public void onOwnerDisconnected() {
            changeLibraryDb(new File(Library.DATABASE_LOCATION,Library.LOCAL_DATABASE_NAME));
            librarySpinner.setSelection(0);
        }
    };

    public void onDestroyView() {
        super.onDestroyView();
        if (shareGroupWeakReference != null) {
            ShareGroup group = shareGroupWeakReference.get();
            if (group != null) {
                group.unregisterGroupMemberListener(this);
                group.unregisterGroupConnectionListener(this);
            }
        }
    }

    //not required to handle
    @Override
    public void onConnectionFailed(String failureMessage) {}
    @Override
    public void onExchangingInfo() {}
}

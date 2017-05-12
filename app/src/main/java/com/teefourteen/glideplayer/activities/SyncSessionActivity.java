package com.teefourteen.glideplayer.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.Synchronization;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.services.PlayerService;

import java.util.ArrayList;

public class SyncSessionActivity extends AppCompatActivity {
    private static final String SESSION_FETCH_THREAD_NAME = "session_fetch_thread";
    public static final String EXTRA_SESSION_TYPE = "session_type";
    private EasyHandler handler = new EasyHandler();

    private Synchronization.SessionType type;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_sessions);
        Toolbar toolbar = (Toolbar) findViewById(R.id.sync_sessions_toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.sync_create_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createSession();
            }
        });

        if(getIntent().hasExtra(EXTRA_SESSION_TYPE)) {
            type = (Synchronization.SessionType) getIntent().getSerializableExtra(EXTRA_SESSION_TYPE);
        }

        handler.createHandler(SESSION_FETCH_THREAD_NAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchSessions();
    }

    private void createSession() {
        if(type == Synchronization.SessionType.MUSIC) {
            Synchronization.MusicSession session =
                    (Synchronization.MusicSession) Synchronization.getInstance().createMusicSession();
            clearQueue(session.getPlayQueue());
            startActivity(new Intent(this, SyncMusicPlayerActivity.class));
        } else {
            Synchronization.getInstance().createVideoSession();
            clearQueue(null);
            startActivity(new Intent(this, SyncVideoPlayerActivity.class));
        }
        Toast.makeText(SyncSessionActivity.this, "Created new session",
                Toast.LENGTH_LONG).show();
    }

    private void fetchSessions() {
        final ListView syncListView = (ListView) findViewById(R.id.sync_list_view);
        final TextView statusTextView = (TextView) findViewById(R.id.fetch_status);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.sync_create_fab);

        if(Synchronization.getInstance() == null) {
            statusTextView.setText("Connect to a group first");
            statusTextView.setVisibility(View.VISIBLE);
            syncListView.setVisibility(View.INVISIBLE);
            fab.setVisibility(View.INVISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
            Synchronization.FetchSessionsListener fetchListener =
                    new Synchronization.FetchSessionsListener() {
                @Override
                public void sessionsFetched(final ArrayList<Synchronization.Session> sessionList) {
                    if(sessionList.size() == 0) {
                        statusTextView.setText("No sessions available");
                        statusTextView.setVisibility(View.VISIBLE);
                        syncListView.setVisibility(View.INVISIBLE);
                    } else {
                        SyncListAdapter adapter = new SyncListAdapter(SyncSessionActivity.this,
                                sessionList);
                        statusTextView.setVisibility(View.INVISIBLE);
                        syncListView.setVisibility(View.VISIBLE);
                        syncListView.setAdapter(adapter);
                        syncListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Synchronization.Session syncSession = sessionList.get(position);
                                Synchronization.getInstance().joinSession(syncSession,
                                        new Synchronization.JoinSessionListener() {
                                            @Override
                                            public void sessionJoined(Synchronization.Session s) {
                                                if(type == Synchronization.SessionType.MUSIC) {
                                                    clearQueue(((Synchronization.MusicSession) s)
                                                            .getPlayQueue());

                                                    startActivity(new Intent(SyncSessionActivity.this,
                                                            SyncMusicPlayerActivity.class));
                                                } else {
                                                    clearQueue(null);
                                                    startActivity(new Intent(SyncSessionActivity.this,
                                                            SyncVideoPlayerActivity.class));
                                                }
                                                Toast.makeText(SyncSessionActivity.this, "Joined session",
                                                        Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void sessionJoinFailed() {
                                                Toast.makeText(SyncSessionActivity.this,
                                                        "Failed to join session", Toast.LENGTH_LONG)
                                                        .show();
                                            }
                                        });
                            }
                        });
                    }
                }

                @Override
                public void fetchFailed() {
                    statusTextView.setText("Failed to fetch sessions");
                    statusTextView.setVisibility(View.VISIBLE);
                }
            };
            if(type == Synchronization.SessionType.MUSIC) {
                Synchronization.getInstance().fetchMusicSessions(fetchListener);
            } else {
                Synchronization.getInstance().fetchVideoSessions(fetchListener);
            }

        }


    }

    private void clearQueue(PlayQueue syncPlayQueue) {
        Intent intent = new Intent(SyncSessionActivity.this, PlayerService.class);
        intent.putExtra(PlayerService.EXTRA_CLEAR_PLAYER, 0);
        startService(new Intent(intent));

        Global.playQueue = syncPlayQueue;
    }

    class SyncListAdapter extends ArrayAdapter<Synchronization.Session> {
        Context context;
        ArrayList<Synchronization.Session> sessionList;

        SyncListAdapter(Context context, ArrayList<Synchronization.Session> sessionList) {
            super(context, 0, sessionList);
            this.context = context;
            this.sessionList = sessionList;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.sync_session, parent, false);

            Synchronization.Session session = sessionList.get(position);

            TextView textView = (TextView) convertView.findViewById(R.id.session_user_name);
            textView.setText(session.getOwnerName() + "'s Session");

            textView = (TextView) convertView.findViewById(R.id.session_participants);
            textView.setText(textView.getText() + " " + session.getNumberOfMembers());

            textView = (TextView) convertView.findViewById(R.id.session_current);
            textView.setText(textView.getText() + " " + session.getCurrentPlaying());

            return convertView;
        }

        @Override
        public int getCount() {
            return sessionList.size();
        }
    }
}

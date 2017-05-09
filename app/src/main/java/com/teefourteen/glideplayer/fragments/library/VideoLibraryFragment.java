package com.teefourteen.glideplayer.fragments.library;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.activities.VideoPlayerActivity;
import com.teefourteen.glideplayer.database.Library;
import com.teefourteen.glideplayer.video.Video;
import com.teefourteen.glideplayer.video.VideoPlayer;

/**
 * A simple {@link Fragment} subclass.
 */
public class VideoLibraryFragment extends LibraryFragment implements VideoAdapter.VideoClickListener {
    private VideoAdapter videoAdapter;

    @Override
    View inflateRootView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    void initializeContent() {
        videoAdapter = new VideoAdapter(this, Library.getVideos(null));
        RecyclerView videoRecyclerView =
                (RecyclerView) rootView.findViewById(R.id.video_recycler_view);
        videoRecyclerView.setHasFixedSize(true);
        videoRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        videoRecyclerView.setAdapter(videoAdapter);
    }

    @Override
    public void libraryChanged(String userName) {
        videoAdapter.changeCursor(Library.getVideos(userName));
        videoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onVideoClick(Cursor videoCursor, int position) {
        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
        videoCursor.moveToPosition(position);
        Video video = Video.toVideo(videoCursor);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_USERNAME, video.libraryUsername);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_ID, video.videoId);
        startActivity(intent);
    }

    @Override
    public void onVideoLongClick(Cursor videoCursor, int position) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        videoAdapter.closeCursor();
    }
}

/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.fragments.library.adapters;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.video.Video;

import java.util.concurrent.TimeUnit;


public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoHolder> {
    private Cursor videoCursor;
    private VideoClickListener clickListener;

    public interface VideoClickListener {
        void onVideoClick(Cursor videoCursor, int position);
        void onVideoLongClick(Cursor videoCursor, int position);
    }
    
    public VideoAdapter(VideoClickListener listener, Cursor videoCursor) {
        this.videoCursor = videoCursor;
        this.clickListener = listener;
    }

    @Override
    public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.video, parent, false);
        return new VideoHolder(view);
    }

    @Override
    public void onBindViewHolder(VideoHolder holder, int position) {
        videoCursor.moveToPosition(position);
        holder.bindView(Video.toVideo(videoCursor), position);
    }

    @Override
    public int getItemCount() {
        return videoCursor.getCount();
    }

    class VideoHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {
        private int position;
        private TextView videoTitle;
        private TextView path;
        private TextView length;

        VideoHolder(View itemView) {
            super(itemView);
            this.videoTitle = (TextView) itemView.findViewById(R.id.vid_name);
            this.path = (TextView) itemView.findViewById(R.id.vid_path);
            this.length = (TextView) itemView.findViewById(R.id.vid_length);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @SuppressLint("DefaultLocale")
        void bindView(Video video, int position) {
            this.position = position;

            videoTitle.setText(video.title);
            if(video.isRemote) {
                path.setText("From " + video.libraryUsername + "'s Device");
            } else {
                path.setText(video.filePath);
            }
            length.setText(
                    String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(video.duration),
                    TimeUnit.MILLISECONDS.toMinutes(video.duration) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(video.duration)),
                    TimeUnit.MILLISECONDS.toSeconds(video.duration) -
                            TimeUnit.MINUTES.toSeconds(
                                    TimeUnit.MILLISECONDS.toMinutes(video.duration)))
            );
        }

        @Override
        public void onClick(View v) {
            clickListener.onVideoClick(videoCursor, position);
        }

        @Override
        public boolean onLongClick(View v) {
            clickListener.onVideoLongClick(videoCursor, position);
            return true;
        }
    }

    public void changeCursor(Cursor cursor) {
        videoCursor.close();
        videoCursor = cursor;
    }

    public void closeCursor() {
        if(videoCursor != null) {
            videoCursor.close();
        }
    }
}

package com.teefourteen.glideplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

import java.lang.ref.WeakReference;

import com.teefourteen.glideplayer.activities.MainActivity;
import com.teefourteen.glideplayer.activities.PlayerActivity;
import com.teefourteen.glideplayer.music.Song;
import com.teefourteen.glideplayer.services.PlayerService;


public class AppNotification extends NotificationCompat {
    private static WeakReference<AppNotification> weakReference = null;
    private Song currentSong = null;
    private Context context = null;
    private static int playerNotificationId = 1;
    private static int networkNotificationId = 2;
    private static final int PLAY_REQUEST_CODE = 0;
    private static final int PAUSE_REQUEST_CODE = 1;
    private static final int NEXT_REQUEST_CODE = 2;
    private static final int PREV_REQUEST_CODE = 3;

    public static AppNotification getInstance(Context context) {
        if(weakReference == null || weakReference.get() == null) {
            AppNotification appNotification = new AppNotification(context);
            weakReference = new WeakReference<AppNotification>(appNotification);
        }

        return weakReference.get();
    }

    private AppNotification(Context context) {
        this.context = context;
    }

    public void displayPlayerNotification(Song currentSong, boolean isPlaying,
                                                  boolean ongoing) {
        this.currentSong = currentSong;

        NotificationCompat.Builder notificationBuilder = new Builder(context);

        //create custom views
        RemoteViews notificationView = new RemoteViews(context.getPackageName(),
                R.layout.notification_player);

        RemoteViews notificationExpandedView = new RemoteViews(context.getPackageName(),
                R.layout.notification_expanded_player);

        preparePlayerNotification(notificationView, isPlaying);
        prepareExpandedPlayerNotification(notificationExpandedView, isPlaying);

        notificationBuilder.setContent(notificationView);
        notificationBuilder.setCustomBigContentView(notificationExpandedView);

        notificationBuilder.setSmallIcon(R.drawable.glideplayer_notification);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addNextIntent(new Intent(context, MainActivity.class));
        stackBuilder.addNextIntent(new Intent(context, PlayerActivity.class));

        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(pendingIntent);

        notificationBuilder.setOngoing(ongoing);

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(playerNotificationId, notificationBuilder.build());
    }

    private void preparePlayerNotification(RemoteViews notificationView, boolean isPlaying) {
        //set drawables for small view
        String album_art = currentSong.getAlbumArt();

        if(album_art!=null && !album_art.equals("")) {
            notificationView.setImageViewUri(R.id.notification_album_art,
                    Uri.parse(album_art));
        } else {
            notificationView.setImageViewResource(R.id.notification_album_art,
                    R.mipmap.ic_album_white);
        }

        notificationView.setImageViewResource(R.id.notification_prev,
                R.mipmap.glideplayer_skip_prev_white);

        if(isPlaying) {
            notificationView.setImageViewResource(R.id.notification_play_pause,
                    R.mipmap.glideplayer_pause_white);
        } else {
            notificationView.setImageViewResource(R.id.notification_play_pause,
                    R.mipmap.glideplayer_play_white);
        }
        notificationView.setImageViewResource(R.id.notification_next,
                R.mipmap.glideplayer_skip_next_white);

        //set text
        notificationView.setTextViewText(R.id.notification_track_title,
                currentSong.getTitle());
        notificationView.setTextViewText(R.id.notification_track_artist,
                currentSong.getArtist());

        //set click listeners
        Intent intent;
        PendingIntent pendingIntent;

        intent = new Intent(context, PlayerService.class);
        if(isPlaying) {
            intent.putExtra(PlayerService.EXTRA_PLAY_CONTROL, PlayerService.PAUSE);
            pendingIntent = PendingIntent.getService(context, PAUSE_REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            intent.putExtra(PlayerService.EXTRA_PLAY_CONTROL, PlayerService.PLAY);
            pendingIntent = PendingIntent.getService(context, PLAY_REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        notificationView.setOnClickPendingIntent(R.id.notification_play_pause, pendingIntent);

        intent = new Intent(context, PlayerService.class);
        intent.putExtra(PlayerService.EXTRA_PLAY_CONTROL, PlayerService.PREV);
        pendingIntent = PendingIntent.getService(context, PREV_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationView.setOnClickPendingIntent(R.id.notification_prev, pendingIntent);

        intent = new Intent(context, PlayerService.class);
        intent.putExtra(PlayerService.EXTRA_PLAY_CONTROL, PlayerService.NEXT);
        pendingIntent = PendingIntent.getService(context, NEXT_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationView.setOnClickPendingIntent(R.id.notification_next, pendingIntent);
    }

    private void prepareExpandedPlayerNotification(RemoteViews notificationExpandedView,
                                                   boolean isPlaying) {
        //set drawables for expanded view
        String album_art = currentSong.getAlbumArt();

        if(album_art!=null && !album_art.equals("")) {
            notificationExpandedView.setImageViewUri(R.id.notification_expanded_album_art,
                    Uri.parse(album_art));
        } else {
            notificationExpandedView.setImageViewResource(R.id.notification_expanded_album_art,
                    R.mipmap.ic_album_white);
        }

        notificationExpandedView.setImageViewResource(R.id.notification_expanded_prev,
                R.mipmap.glideplayer_skip_prev_white);
        if(isPlaying) {
            notificationExpandedView.setImageViewResource(R.id.notification_expanded_play_pause,
                    R.mipmap.glideplayer_pause_white);
        } else {
            notificationExpandedView.setImageViewResource(R.id.notification_expanded_play_pause,
                    R.mipmap.glideplayer_play_white);
        }
        notificationExpandedView.setImageViewResource(R.id.notification_expanded_next,
                R.mipmap.glideplayer_skip_next_white);

        //set text
        notificationExpandedView.setTextViewText(R.id.notification_expanded_track_title,
                currentSong.getTitle());
        notificationExpandedView.setTextViewText(R.id.notification_expanded_track_artist,
                currentSong.getArtist());
        notificationExpandedView.setTextViewText(R.id.notification_expanded_track_album,
                currentSong.getAlbum());

        //set click listeners
        Intent intent;
        PendingIntent pendingIntent;

        intent = new Intent(context, PlayerService.class);
        if(isPlaying) {
            intent.putExtra(PlayerService.EXTRA_PLAY_CONTROL, PlayerService.PAUSE);
            pendingIntent = PendingIntent.getService(context, PAUSE_REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            intent.putExtra(PlayerService.EXTRA_PLAY_CONTROL, PlayerService.PLAY);
            pendingIntent = PendingIntent.getService(context, PLAY_REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        notificationExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play_pause,
                pendingIntent);

        intent = new Intent(context, PlayerService.class);
        intent.putExtra(PlayerService.EXTRA_PLAY_CONTROL, PlayerService.PREV);
        pendingIntent = PendingIntent.getService(context, PREV_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationExpandedView.setOnClickPendingIntent(R.id.notification_expanded_prev,
                pendingIntent);

        intent = new Intent(context, PlayerService.class);
        intent.putExtra(PlayerService.EXTRA_PLAY_CONTROL, PlayerService.NEXT);
        pendingIntent = PendingIntent.getService(context, NEXT_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationExpandedView.setOnClickPendingIntent(R.id.notification_expanded_next,
                pendingIntent);
    }

    public void dismissPlayerNotification() {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(playerNotificationId);
    }

    public void displayNetworkNotification(String groupName, boolean isGroupOwner) {
        NotificationCompat.Builder notificationBuilder = new Builder(context);

        notificationBuilder.setContentTitle("GlidePlayer Network");
        if (groupName == null) {
            notificationBuilder.setContentText("Not connected to any group");
            notificationBuilder.setContentInfo("");
        } else {
            notificationBuilder.setContentText("Connected to " + groupName);
            if (isGroupOwner) {
                notificationBuilder.setContentInfo("(Owner)");
            } else {
                notificationBuilder.setContentInfo("");
            }
        }
        notificationBuilder.setWhen(0);

        notificationBuilder.setSmallIcon(R.mipmap.glideplayer_play_white);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_JUMP_TO_CONNECTIVITY_FRAGMENT, 0);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder.setOngoing(true);
        notificationBuilder.setPriority(PRIORITY_MIN);

        notificationManager.notify(networkNotificationId, notificationBuilder.build());
    }

    public void dismissNetworkNotification() {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(networkNotificationId);
    }
}
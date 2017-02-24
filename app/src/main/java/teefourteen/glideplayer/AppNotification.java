package teefourteen.glideplayer;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

import teefourteen.glideplayer.music.Song;

/**
 * Created by George on 2/24/2017.
 */

public class AppNotification extends NotificationCompat {
    private NotificationCompat notification = null;
    private Song currentSong = null;
    private Context context = null;
    private RemoteViews playerView;
    private static final int NOTIFICATION_ID = 1;

    public AppNotification(Context context) {
        this.context = context;
    }

    public void createPlayerNotification(Song currentSong) {
        this.currentSong = currentSong;

        NotificationCompat.Builder notificationBuilder = new Builder(context);

        //create custom views
        RemoteViews notificationView = new RemoteViews(context.getPackageName(),
                R.layout.notification_player);

        RemoteViews notificationExpandedView = new RemoteViews(context.getPackageName(),
                R.layout.notification_expanded_player);

        preparePlayerNotification(notificationView);
        prepareExpandedPlayerNotification(notificationExpandedView);

        notificationBuilder.setContent(notificationView);
        notificationBuilder.setCustomBigContentView(notificationExpandedView);

        notificationBuilder.setSmallIcon(R.drawable.glideplayer_notification);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void preparePlayerNotification(RemoteViews notificationView) {
        //set drawables for small view
        String album_art = currentSong.getAlbumArt();

        if(album_art!=null && !album_art.equals("")) {
            notificationView.setImageViewUri(R.id.notification_album_art,
                    Uri.parse(album_art));
        } else {
            notificationView.setImageViewResource(R.id.notification_album_art,
                    R.drawable.ic_album_white_24dp);
        }

        notificationView.setImageViewResource(R.id.notification_prev,
                R.drawable.glideplayer_skip_prev_white);
        notificationView.setImageViewResource(R.id.notification_play_pause,
                R.drawable.glideplayer_pause_white);
        notificationView.setImageViewResource(R.id.notification_next,
                R.drawable.glideplayer_skip_next_white);

        //set text
        notificationView.setTextViewText(R.id.notification_track_title,
                currentSong.getTitle());
        notificationView.setTextViewText(R.id.notification_track_artist,
                currentSong.getArtist());
    }

    private void prepareExpandedPlayerNotification(RemoteViews notificationExpandedView) {
        //set drawables for expanded view
        String album_art = currentSong.getAlbumArt();

        if(album_art!=null && !album_art.equals("")) {
            notificationExpandedView.setImageViewUri(R.id.notification_expanded_album_art,
                    Uri.parse(album_art));
        } else {
            notificationExpandedView.setImageViewResource(R.id.notification_expanded_album_art,
                    R.drawable.ic_album_white_24dp);
        }

        notificationExpandedView.setImageViewResource(R.id.notification_expanded_prev,
                R.drawable.glideplayer_skip_prev_white);
        notificationExpandedView.setImageViewResource(R.id.notification_expanded_play_pause,
                R.drawable.glideplayer_pause_white);
        notificationExpandedView.setImageViewResource(R.id.notification_expanded_next,
                R.drawable.glideplayer_skip_next_white);

        //set text
        notificationExpandedView.setTextViewText(R.id.notification_expanded_track_title,
                currentSong.getTitle());
        notificationExpandedView.setTextViewText(R.id.notification_expanded_track_artist,
                currentSong.getArtist());
        notificationExpandedView.setTextViewText(R.id.notification_expanded_track_album,
                currentSong.getAlbum());
    }
}
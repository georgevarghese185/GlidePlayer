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

package com.teefourteen.glideplayer.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;

import com.teefourteen.glideplayer.activities.SplashActivity;
import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.database.Library;


public class LibraryService extends IntentService {
    public LibraryService() {
        super(SplashActivity.LIBRARY_INIT_THREAD_NAME);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        File file = new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME);
        if(file.exists()) {
            file.delete();
        }

        Library.initialize(this);

        File lastQueue = new File(PlayerService.PLAY_QUEUE_FILE_PATH);

        if(lastQueue.exists()) {
            try {
                Global.playQueue = new PlayQueue(lastQueue);
            } catch (IOException e) {
                Global.playQueue = null;
                lastQueue.delete();
            }
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                new Intent(SplashActivity.LIBRARY_INITIALIZED_ACTION));
    }
}

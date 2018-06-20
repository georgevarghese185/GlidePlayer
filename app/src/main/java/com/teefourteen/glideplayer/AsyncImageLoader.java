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

package com.teefourteen.glideplayer;


import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.io.File;

public class AsyncImageLoader extends CancellableAsyncTaskHandler {

    public static class ImageLoadTask implements Task {
        private ImageView imageView;
        private String imagePath;
        private Drawable artDrawable;

        private ImageLoadTask(ImageView imageView, String imagePath) {
            this.imageView = imageView;
            this.imagePath = imagePath;
        }

        @Override
        public void doBackground() {
            if(new File(imagePath).exists()) {
                artDrawable = Drawable.createFromPath(imagePath);
            } else {
                artDrawable = null;
            }
        }

        @Override
        public void doOnMainThread() {
            if(artDrawable != null) {
                imageView.setImageDrawable(artDrawable);
            }
        }
    }

    public AsyncImageLoader(int maxParallelLoads) {
        super(maxParallelLoads);
    }

    public ImageLoadTask loadImageAsync(ImageView imageView, String imagePath) {
        ImageLoadTask imageLoadTask = new ImageLoadTask(imageView, imagePath);
        loadAsync(imageLoadTask);
        return imageLoadTask;
    }

    public synchronized void cancelTask(Task task) {
        super.cancelTask(task, true);
    }
}

package com.teefourteen.glideplayer;


import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

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

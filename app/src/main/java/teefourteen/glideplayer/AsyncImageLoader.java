package teefourteen.glideplayer;


import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.LinkedList;

public class AsyncImageLoader {
    private int maxThreads;
    private static final String ON_COMPLETE_THREAD = "Async_image_complete_thread";
    private EasyHandler handler = new EasyHandler();
    private HashMap<LoadTask, ImageAsyncTask> activeTasks;
    private LinkedList<LoadTask> taskList = new LinkedList<>();


    private class ImageAsyncTask extends AsyncTask<String, Object, Drawable> {
        private ImageView imageView;
        private LoadTask loadTask;

        ImageAsyncTask(ImageView imageView, LoadTask loadTask) {
            this.imageView = imageView;
            this.loadTask = loadTask;
        }

        @Override
        protected Drawable doInBackground(String... params) {
            return Drawable.createFromPath(params[0]);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            imageView.setImageDrawable(drawable);
            handler.executeAsync(new Runnable() {
                @Override
                public void run() {
                    onTaskComplete(loadTask);
                }
            }, ON_COMPLETE_THREAD, true);
        }
    }

    public static class LoadTask {
        private ImageView imageView;
        private String imagePath;

        public LoadTask(ImageView imageView, String imagePath) {
            this.imageView = imageView;
            this.imagePath = imagePath;
        }
    }

    public AsyncImageLoader(int maxParallelLoads) {
        this.maxThreads = maxParallelLoads;
        activeTasks = new HashMap<>(maxParallelLoads);
    }

    synchronized public void loadAsync(LoadTask loadTask) {
        if(activeTasks.size() < maxThreads) {
            ImageAsyncTask asyncTask = new ImageAsyncTask(loadTask.imageView, loadTask);
            activeTasks.put(loadTask, asyncTask);
            asyncTask.execute(loadTask.imagePath);
        } else {
            taskList.addLast(loadTask);
        }
    }

    synchronized public void cancelTask(LoadTask loadTask) {
        if(activeTasks.containsKey(loadTask)) {
            activeTasks.get(loadTask).cancel(true);
            activeTasks.remove(loadTask);
        } else if(taskList.contains(loadTask)) {
            taskList.remove(loadTask);
        }
    }

    synchronized private void onTaskComplete(LoadTask loadTask) {
        activeTasks.remove(loadTask);
        if(taskList.size() > 0) {
            LoadTask task = taskList.getFirst();
            taskList.remove(task);
            loadAsync(task);
        }
    }
}

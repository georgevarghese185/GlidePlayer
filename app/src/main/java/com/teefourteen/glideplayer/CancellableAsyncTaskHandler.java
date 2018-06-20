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

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.LinkedList;


public class CancellableAsyncTaskHandler {
    protected int maxThreads;
    protected static final String ON_COMPLETE_THREAD = "task_complete_thread";
    protected EasyHandler handler = new EasyHandler();
    protected HashMap<Task, AsyncTask> activeTasks;
    protected LinkedList<Task> taskList = new LinkedList<>();

    public CancellableAsyncTaskHandler(int maxParallelTasks) {
        maxThreads = maxParallelTasks;
        activeTasks = new HashMap<>(maxParallelTasks);
    }

    public interface Task {
        void doBackground();
        void doOnMainThread();
    }

    protected class Executor extends AsyncTask<Task, Object, Task> {
        @Override
        protected Task doInBackground(Task... params) {
            params[0].doBackground();
            return params[0];
        }

        @Override
        protected void onPostExecute(final Task task) {
            task.doOnMainThread();
            handler.executeAsync(new Runnable() {
                @Override
                public void run() {onTaskComplete(task);
                }
            }, ON_COMPLETE_THREAD);
        }
    }

    synchronized public void loadAsync(Task task) {
        if(activeTasks.size() < maxThreads) {
            Executor executor = new Executor();
            activeTasks.put(task, executor);
            executor.execute(task);
        } else {
            taskList.addLast(task);
        }
    }

    synchronized public void cancelTask(Task task, boolean interrupt) {
        if(activeTasks.containsKey(task)) {
            activeTasks.get(task).cancel(interrupt);
            activeTasks.remove(task);
        } else if(taskList.contains(task)) {
            taskList.remove(task);
        }
    }

    synchronized protected void onTaskComplete(Task loadTask) {
        activeTasks.remove(loadTask);
        if(taskList.size() > 0) {
            Task task = taskList.getFirst();
            taskList.remove(task);
            loadAsync(task);
        }
    }
}
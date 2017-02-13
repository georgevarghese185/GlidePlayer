package teefourteen.glideplayer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue;

import java.util.HashMap;

/**
 * A class to make it easier to quickly execute asynchronous tasks on a new or previously created
 * separate thread, or execute a task on the main thread (useful for posting tasks on the main
 * thread from within another thread).
 */
public class EasyHandler {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private HashMap<String, Handler> handlerMap;


    public EasyHandler() {
        handlerMap = new HashMap<>(1);
    }

    /**Executes a task on the main thread.*/
    public static void executeOnMainThread(Runnable r) {
        mainHandler.post(r);
    }

    /**
     * Creates a new HandlerThread, starts it and assigns it to a new handler (if not already
     * created).
     */
    synchronized public void createHandler(String handlerName){
        if(!handlerMap.containsKey(handlerName)) {
            HandlerThread handlerThread = new HandlerThread(handlerName);
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());
            handlerMap.put(handlerName, handler);
        }
    }

    /**
     * Takes a runnable and handler name, calls create Handler to create the handler and executes
     * the task on that thread.
     *
     * Note: Remember to close all handlers before destroying this object!
     */
    public void executeAsync(Runnable r, final String handlerName) {
        createHandler(handlerName);
        handlerMap.get(handlerName).post(r);
    }

    /**
     * If quitHandlerIfIdle is true, a temporary HandlerThread is created that stays alive only as
     * long as there are more runnables to be executed.
     *
     * Note: Once a runnable is posted to a handler using this overload, it becomes a temporary
     * handler and cannot be changed back to non temporary. Either post new runnables to it while
     * it is still alive to keep it from quitting, or if the thread quit already, just call
     * executeAsync() with the same thread name and a new thread will be created again with the same
     * name. Either way, it probably won't make a difference except for the overhead of having to
     * create a new handler and thread once again.
     */
    public void executeAsync(Runnable r, final String handlerName, boolean quitHandlerIfIdle) {
        executeAsync(r, handlerName);

        if(quitHandlerIfIdle) {
            final Handler handler = handlerMap.get(handlerName);

            final MessageQueue.IdleHandler idleHandler = new MessageQueue.IdleHandler() {
                @Override
                public boolean queueIdle() {
                    handlerMap.remove(handlerName);
                    handler.getLooper().quit();
                    return false;
                }
            };

            Runnable quitter = new Runnable() {
                @Override
                public void run() {
                    Looper.myQueue().addIdleHandler(idleHandler);
                }
            };

            handler.post(quitter);
        }
    }

    /**
     * Safely close all handlersQuits a specific handler's looper. The task of quitting the thread
     * is posted onto that thread's queue itself so that any remaining tasks finish first (since
     * quitSafely() is unavailable below sdk 18).
     */
    synchronized public void closeHandler(final String handlerName) {
        final Handler handler = handlerMap.get(handlerName);
        if(handler != null) {
            handlerMap.remove(handlerName);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    handler.getLooper().quit();
                }
            });
        }
    }

    synchronized public void killHandler(final String handlerName) {
        Handler handler = handlerMap.get(handlerName);
        if(handler != null) {
            handlerMap.remove(handlerName);
            handler.getLooper().quit();
        }
    }

    /**Closes all running handlers and their threads*/
    public void closeAllHandlers() {
        HashMap<String, Handler> handlerMap = this.handlerMap;
        this.handlerMap = new HashMap<>();
        for(String handlerName : handlerMap.keySet()) {
            final Handler handler = handlerMap.get(handlerName);
            if(handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handler.getLooper().quit();
                    }
                });
            }
        }
    }

    public void executeWhenIdle(String handlerName, final Runnable r, final boolean executeOnce) {
        final MessageQueue.IdleHandler idleHandler = new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                r.run();
                return !executeOnce;
            }
        };

        Handler handler = handlerMap.get(handlerName);
        if(handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Looper.myQueue().addIdleHandler(idleHandler);
                }
            });
        }
    }
}

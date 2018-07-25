package com.flexdecision.ak_lex.threadpoolexec;

import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomThreadPoolManager {
    public static final String TAG = CustomThreadPoolManager.class.getSimpleName();
    private static CustomThreadPoolManager sInstance = null;
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private final ExecutorService mExecutorService;
    private final BlockingQueue<Runnable> mTaskQueue;
    private List<Future> mRunningTaskList;

    private WeakReference<UiThreadCallback> uiThreadCallbackWeakReference;

    private Monitor monitor;

    public ExecutorService getmExecutorService() {
        return mExecutorService;
    }

    private CustomThreadPoolManager(){
        mTaskQueue = new LinkedBlockingQueue<Runnable>();

        mRunningTaskList = new ArrayList<>();

        Log.e(TAG,"Available cores: " + NUMBER_OF_CORES);

        //mExecutorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE, new BackgroundThreadFactory());
        mExecutorService = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES*2,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mTaskQueue, new BackgroundThreadFactory());
    }

    public static CustomThreadPoolManager getsInstance() {
        if (sInstance == null){
            synchronized (CustomThreadPoolManager.class) {
                sInstance = new CustomThreadPoolManager();
            }
        }
        return sInstance;
    }
    // Add a callable to the queue, which will be executed by the next available thread in the pool
    public void addCallable(Callable callable){
        if (monitor == null || !monitor.getStatus()){
            Log.d(TAG, "Activate Thread monitor");
            monitor = new Monitor(this, 1);
            Thread monitorThread = new Thread(monitor);
            monitorThread.start();
            sendMessageToUiThread(Util.createMessage(Util.MESSAGE_JOB_STARTED_ID));
        }

        Future future = mExecutorService.submit(callable);
        mRunningTaskList.add(future);
    }

    /* Remove all tasks in the queue and stop all running threads
     * Notify UI thread about the cancellation
     */
    public void cancelAllTasks() {
        synchronized (this) {
            mTaskQueue.clear();
            for (Future task : mRunningTaskList) {
                if (!task.isDone()) {
                    task.cancel(true);
                }
            }
            mRunningTaskList.clear();
        }
        sendMessageToUiThread(Util.createMessage(Util.MESSAGE_ID, "All tasks in the thread pool are cancelled"));
        monitor.shutdown();
        sendMessageToUiThread(Util.createMessage(Util.MESSAGE_JOB_FINISHED_ID));
    }

    // Keep a weak reference to the UI thread, so we can send messages to the UI thread
    public void setUiThreadCallback(UiThreadCallback uiThreadCallback) {
        this.uiThreadCallbackWeakReference = new WeakReference<UiThreadCallback>(uiThreadCallback);
    }

    // Pass the message to the UI thread
    public void sendMessageToUiThread(Message message){
        if(uiThreadCallbackWeakReference != null && uiThreadCallbackWeakReference.get() != null) {
            uiThreadCallbackWeakReference.get().publishToUiThread(message);
        }
    }

    public void finished(){
        sendMessageToUiThread(Util.createMessage(Util.MESSAGE_JOB_FINISHED_ID));
    }

    /* A ThreadFactory implementation which create new threads for the thread pool.
      The threads created is set to background priority, so it does not compete with the UI thread.
    */
    private static class BackgroundThreadFactory implements ThreadFactory {
        private static int sTag = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("CustomThread" + sTag);
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);

            // A exception handler is created to log the exception from threads
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Log.e(TAG, thread.getName() + " encountered an error: " + ex.getMessage());
                }
            });
            return thread;
        }
    }


}

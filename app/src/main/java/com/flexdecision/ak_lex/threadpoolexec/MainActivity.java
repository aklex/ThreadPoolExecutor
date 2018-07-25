package com.flexdecision.ak_lex.threadpoolexec;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity implements UiThreadCallback {
    // The handler for the UI thread. Used for handling messages from worker threads.
    private UiHandler mUiHandler;
    public static final String TAG = MainActivity.class.getSimpleName();

    // A text view to show messages sent from work threads
    private TextView mDisplayTextView;
    private ImageView imageView;

    // A thread pool manager
    // It is a static singleton instance by design and will survive activity lifecycle
    private CustomThreadPoolManager mCustomThreadPoolManager;
    private RecyclerView recyclerView;
    private RVAdapter adapter;

    private ImageManager imageManager;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);
        imageManager = ImageManager.getInstance();

        recyclerView = findViewById(R.id.imageList);
        adapter = new RVAdapter(imageManager.getImages());
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        //adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize the handler for UI thread to handle message from worker threads
        mUiHandler = new UiHandler(Looper.getMainLooper(), this);


        // get the thread pool manager instance
        mCustomThreadPoolManager = CustomThreadPoolManager.getsInstance();
        // CustomThreadPoolManager stores activity as a weak reference. No need to unregister.
        mCustomThreadPoolManager.setUiThreadCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void downloadImage(View view){
        initializeData();
        adapter.notifyDataSetChanged();
    }


    public void cancelAllTasksInThreadPool(View view) {
        Log.d(TAG, "Canceling all tasks");
        mCustomThreadPoolManager.cancelAllTasks();
    }


    @Override
    public void publishToUiThread(Message message) {
        // add the message from worker thread to UI thread's message queue
        if(mUiHandler != null){
            mUiHandler.sendMessage(message);
        }
    }

    private static class UiHandler extends Handler {
        private WeakReference<MainActivity> mWeakRefDisplay;

        public UiHandler(Looper looper, MainActivity display) {
            super(looper);
            this.mWeakRefDisplay = new WeakReference<>(display);
        }

        // This method will run on UI thread
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                // Our communication protocol for passing a string to the UI thread
                case Util.MESSAGE_JOB_STARTED_ID:
                    if(mWeakRefDisplay != null && mWeakRefDisplay.get() != null) {
                        mWeakRefDisplay.get().progressBar.setVisibility(View.VISIBLE);
                    }
                    break;
                case Util.MESSAGE_JOB_FINISHED_ID:
                    if(mWeakRefDisplay != null && mWeakRefDisplay.get() != null) {
                        mWeakRefDisplay.get().progressBar.setVisibility(View.INVISIBLE);
                    }
                    break;
                case Util.MESSAGE_BITMAP_ID:
                    Bundle bundle1 = msg.getData();
                    String url = bundle1.getString(Util.URL_MESSAGE);
                    Bitmap bitmap = bundle1.getParcelable(Util.BITMAP_MESSAGE);

                    ImageManager imageManager = ImageManager.getInstance();
                    List<Image> images = imageManager.getImages();
                    images.add(new Image(url, bitmap));
                    int lastElement = images.size() - 1;

                    if(mWeakRefDisplay != null && mWeakRefDisplay.get() != null) {
                        mWeakRefDisplay.get().adapter.notifyItemChanged(lastElement);
                        mWeakRefDisplay.get().recyclerView.scrollToPosition(lastElement);

                    }
                    break;

                default:
                    break;
            }
        }
    }

    public void initializeData(){
        String[] urlsArray = getResources().getStringArray(R.array.urls);

        for(int i=0; i< urlsArray.length; i++) {
            Log.d(TAG, "url: " + urlsArray[i]);
            CustomCallable callable = new CustomCallable(urlsArray[i]);
            callable.setCustomThreadPoolManager(mCustomThreadPoolManager);
            mCustomThreadPoolManager.addCallable(callable);
        }

    }
}

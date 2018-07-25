package com.flexdecision.ak_lex.threadpoolexec;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

public class CustomCallable implements Callable {
    private WeakReference<CustomThreadPoolManager> mCustomThreadPoolManagerWeakReference;
    public static final String TAG = CustomCallable.class.getSimpleName();
    InputStream in = null;
    Bitmap bitmap;

    String strUrl;

    public CustomCallable(String strUrl) {
        this.strUrl = strUrl;
    }

    @Override
    public Object call() throws Exception {
        try {
            if (Thread.interrupted()) throw new InterruptedException();
            loadBitmap(strUrl);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setCustomThreadPoolManager(CustomThreadPoolManager customThreadPoolManager) {
        this.mCustomThreadPoolManagerWeakReference = new WeakReference<CustomThreadPoolManager>(customThreadPoolManager);
    }

    private void loadBitmap(String strUrl){
        try {
            Log.d(TAG, "Load is starting");
            URL url = new URL(strUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK){
                Log.d(TAG, "OK_STATUS");
                in = httpURLConnection.getInputStream();
                bitmap = BitmapFactory.decodeStream(in);
                sendResult(strUrl, bitmap);
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendResult(String url, Bitmap bitmap) {
        Log.d(TAG, "Send result");
        Message message = Util.createBitmapMessage(Util.MESSAGE_BITMAP_ID, url, bitmap);

        if(mCustomThreadPoolManagerWeakReference != null
                && mCustomThreadPoolManagerWeakReference.get() != null) {

            mCustomThreadPoolManagerWeakReference.get().sendMessageToUiThread(message);
        }
    }
}

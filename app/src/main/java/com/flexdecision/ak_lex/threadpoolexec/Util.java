package com.flexdecision.ak_lex.threadpoolexec;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;

public class Util {
    public static final int MESSAGE_ID = 1;
    public static final int MESSAGE_BITMAP_ID = 2;
    public static final int MESSAGE_JOB_STARTED_ID =3;
    public static final int MESSAGE_JOB_FINISHED_ID =4;
    public static final String MESSAGE_BODY = "MESSAGE_BODY";
    public static final String BITMAP_MESSAGE = "BITMAP_MESSAGE";
    public static final String URL_MESSAGE = "URL_MESSAGE";

    public static Message createMessage(int id){
        Message message = new Message();
        message.what = id;
        return  message;
    }

    public static Message createMessage(int id, String dataString) {
        Bundle bundle = new Bundle();
        bundle.putString(Util.MESSAGE_BODY, dataString);
        Message message = new Message();
        message.what = id;
        message.setData(bundle);
        return message;
    }

    public static Message createBitmapMessage(int id, String url, Bitmap bitmap){
        Bundle bundle = new Bundle();
        bundle.putString(URL_MESSAGE, url);
        bundle.putParcelable(Util.BITMAP_MESSAGE, bitmap);
        Message message = new Message();
        message.what = id;
        message.setData(bundle);
        return message;
    }
}

package com.flexdecision.ak_lex.threadpoolexec;

import android.os.Message;

public interface UiThreadCallback {
    void publishToUiThread(Message message);
}

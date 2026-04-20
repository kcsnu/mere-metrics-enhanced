package com.example.cs360_charlton_molloy_keir.util;

import android.content.Context;
import android.widget.Toast;

/** Small helper for short toast messages */
public final class ToastUtil {

    private ToastUtil() {
    }

    public static void show(Context context, int messageResId) {
        Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_SHORT).show();
    }
}
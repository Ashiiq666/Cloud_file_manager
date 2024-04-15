package com.sn.snfilemanager.providers.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;

import java.util.Objects;

public class ContextUtils {

    @SuppressLint("StaticFieldLeak")
    public static Context context;

    @SuppressLint({"PrivateApi", "RestrictedApi"})
    public static Context getContext() {
        if (context == null) {
            try {
                Context c = (Context) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null);
                context = getContextImpl(Objects.requireNonNull(c));
            } catch (Exception e) {
                // Shall never happen
                throw new RuntimeException(e);
            }
        }
        return context;
    }

    public static Context getContextImpl(Context context) {
        while (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        return context;
    }
}

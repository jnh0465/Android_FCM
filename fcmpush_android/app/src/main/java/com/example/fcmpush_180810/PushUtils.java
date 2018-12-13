package com.example.fcmpush_180810;

import android.content.Context;
import android.os.PowerManager;

//푸시올때 화면깨우기

public class PushUtils {
    private static final String TAG = PushUtils.class.getSimpleName();

    private static PowerManager.WakeLock mWakeLock;

    public static void acquireWakeLock(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "WAKEUP"
        );

        mWakeLock.acquire();
    }

    public static void releaseWakeLock(){
        if(mWakeLock != null){
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}

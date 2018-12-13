package com.example.fcmpush_180810;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    Bitmap bigPicture;                                                                                   //비트맵

    @Override
    public void onNewToken(String mToken) {     //토큰
        super.onNewToken(mToken);
    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        PushUtils.acquireWakeLock(this); //푸시알림시 화면깨우기

        if(remoteMessage.getData().get("imageurl")==null){                                             //node 서버에서 온 imageurl값이 null이면 알림으로 인식, sendNotification_alert로
            sendNotification_alert(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"));
        }else{                                                                                         //imageurl값이 있으면 광고로 인식, sendNotification_ad로
            sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("imageurl"));
        }

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String deviceToken = instanceIdResult.getToken();
            }
        });
    }

    private void sendNotification_alert(String title, String body) {
        PushUtils.releaseWakeLock(); //푸시알림시 화면깨우기
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);                                                                   //이제 Builder사용할 때 channelId필요, strings.xml에 저장되어있음
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("오투미_"+title)                                               //타이틀
                        .setContentText(body)                                                           //텍스트
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setVibrate(new long[]{100, 0, 100, 0})
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {                                            //오레오부터 채널필요
            String channelName = getString(R.string.default_notification_channel_name);                  //strings.xml에 저장되어있음
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);                                                               //진동
            channel.setVibrationPattern(new long[]{100, 200, 100, 200});                                 //진동간격설정
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private void sendNotification(String title, String myimg) {
        PushUtils.releaseWakeLock(); //화면깨우기
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        try {
            URL url = new URL(myimg);
            bigPicture = BitmapFactory.decodeStream(url.openConnection().getInputStream());              //이미지 비트맵으로 변환
        } catch (Exception e) {
            e.printStackTrace();
        }

        String channelId = getString(R.string.default_notification_channel_id);                          //이제 Builder사용할 때 channelId필요, strings.xml에 저장되어있음
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("오투미_"+title)                                               //타이틀
                        .setContentText("두 손가락을 이용해 아래로 당겨주세요")                         //텍스트
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setVibrate(new long[]{100, 0, 100, 0})

                        .setStyle(new NotificationCompat.BigPictureStyle()                              //손가락으로 내렸을 때 화면
                                .bigPicture(bigPicture)                                                 //비트맵이미지
                                .setBigContentTitle("오투미_"+title)                                    //타이틀
                                .setSummaryText("* 수신거부 : 설정>알림거부"))                          //텍스트
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {                                           //오레오부터 채널필요
            String channelName = getString(R.string.default_notification_channel_name);                 //strings.xml에 저장되어있음
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);                                                              //진동
            channel.setVibrationPattern(new long[]{100, 200, 100, 200});                                //진동간격설정
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
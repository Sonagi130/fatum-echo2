package io.github.sonagi130.anima;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/** Fatum Echo 保活服务：常驻通知 + 后台存活，小工能力的宿主 */
public class HearthService extends Service {
    public static final String CHANNEL_ID = "fatum_keep";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Fatum Echo", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        startForeground(1, buildNotif());
    }

    private Notification buildNotif() {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("Fatum Echo · 小工在线")
                .setContentText("通知/截屏/读屏待命中")
                .setOngoing(true)
                .setContentIntent(pi);
        return b.build();
    }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
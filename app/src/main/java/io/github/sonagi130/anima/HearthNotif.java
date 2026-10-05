package io.github.sonagi130.anima;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/** 通知监听：Fatum Echo 网页可随时读取最近通知 */
public class HearthNotif extends NotificationListenerService {
    public static final List<String[]> LAST = new ArrayList<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        String text = "";
        try {
            Notification n = sbn.getNotification();
            if (n != null && n.extras != null) {
                CharSequence t = n.extras.getCharSequence(Notification.EXTRA_TEXT);
                if (TextUtils.isEmpty(t)) t = n.extras.getCharSequence(Notification.EXTRA_TITLE);
                if (t != null) text = t.toString();
            }
        } catch (Exception ignored) {}
        if (text.isEmpty()) return;
        synchronized (LAST) {
            LAST.add(0, new String[]{pkg, text, String.valueOf(System.currentTimeMillis())});
            while (LAST.size() > 30) LAST.remove(LAST.size() - 1);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    public static String snapshot() {
        synchronized (LAST) {
            if (LAST.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < LAST.size(); i++) {
                String[] e = LAST.get(i);
                sb.append("{\"pkg\":\"").append(e[0].replace("\"", "'"))
                        .append("\",\"text\":\"").append(e[1].replace("\"", "'").replace("\n", " "))
                        .append("\"},");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]");
            return sb.toString();
        }
    }
}
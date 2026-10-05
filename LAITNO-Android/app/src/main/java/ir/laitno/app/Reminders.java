package ir.laitno.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** یادآوری روزانه حتی وقتی برنامه بسته است (بر اساس تنظیمات یادآوری داخل خود برنامه). */
public final class Reminders {
    static final String CH = "laitno";
    private Reminders() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("ltn", Context.MODE_PRIVATE);
    }

    static void channel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "یادآوری LAITNO", NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("یادآوری مرور لغت‌ها");
                nm.createNotificationChannel(ch);
            }
        }
    }

    static void notify(Context c, String title, String body, int id) {
        try {
            channel(c);
            Intent open = new Intent(c, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(c, 1, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(c, CH) : new Notification.Builder(c);
            b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new Notification.BigTextStyle().bigText(body))
                    .setContentIntent(pi)
                    .setAutoCancel(true);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(id, b.build());
        } catch (Exception ignored) {}
    }

    private static PendingIntent alarmIntent(Context c) {
        return PendingIntent.getBroadcast(c, 7, new Intent(c, ReminderReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void save(Context c, String json) {
        prefs(c).edit().putString("rem", json).apply();
        schedule(c);
    }

    static void schedule(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = alarmIntent(c);
            am.cancel(pi);
            String j = prefs(c).getString("rem", null);
            if (j == null) return;
            JSONObject o = new JSONObject(j);
            if (!o.optBoolean("on", false)) return;
            String[] t = o.optString("time", "20:00").split(":");
            int h = Integer.parseInt(t[0].trim());
            int m = t.length > 1 ? Integer.parseInt(t[1].trim()) : 0;
            Calendar now = Calendar.getInstance();
            Calendar at = Calendar.getInstance();
            at.set(Calendar.HOUR_OF_DAY, h);
            at.set(Calendar.MINUTE, m);
            at.set(Calendar.SECOND, 0);
            at.set(Calendar.MILLISECOND, 0);
            if (at.getTimeInMillis() <= now.getTimeInMillis() + 30_000) at.add(Calendar.DAY_OF_MONTH, 1);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.getTimeInMillis(), pi);
        } catch (Exception ignored) {}
    }

    static void fire(Context c) {
        try {
            String j = prefs(c).getString("rem", null);
            if (j != null && !MainActivity.foreground) {
                JSONObject o = new JSONObject(j);
                boolean dayOk = true;
                JSONArray days = o.optJSONArray("days");
                if (days != null && days.length() > 0) {
                    int js = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1; // 0 = یکشنبه مثل JS
                    dayOk = false;
                    for (int i = 0; i < days.length(); i++) {
                        if (String.valueOf(js).equals(String.valueOf(days.opt(i)))) { dayOk = true; break; }
                    }
                }
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                if (o.optBoolean("on", false) && dayOk && !today.equals(o.optString("last", ""))) {
                    notify(c, "یادآوری هوشمند · LAITNO", "وقت مرور لایتنره؛ چند دقیقه برای لغت‌های امروز وقت بذار 📚", 4242);
                }
            }
        } catch (Exception ignored) {}
        schedule(c);
    }
}

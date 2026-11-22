package ir.masir.bandegi

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

object Notify {
    const val CH = "masir_channel"

    class Rem(val id: Int, val title: String, val text: String, val h: Int, val m: Int)

    val all = listOf(
        Rem(1, "نماز صبح و برنامه امروز", "صبح بخیر. نماز اول وقت و برنامه‌ی امروزت را ببین.", 5, 30),
        Rem(2, "نماز ظهر و عصر", "وقت نماز ظهر و عصر است.", 12, 30),
        Rem(3, "نماز مغرب و عشا", "نماز مغرب و عشا و نماز استغفار را فراموش نکن.", 18, 30),
        Rem(4, "قرآن، استغفار و صلوات", "یک صفحه قرآن، ۱۰۰ استغفار و صلوات‌ها.", 20, 0),
        Rem(5, "برنامه‌ریزی فردا", "فردا چه کارهای اضافه‌ای داری؟ همین حالا ثبتش کن تا فردا یادآوری شود.", 21, 30),
        Rem(6, "محاسبه نفس", "وقت محاسبه‌ی نفس و خلوت شبانه است.", 22, 30)
    )

    fun createChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CH, "یادآوری‌ها", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    private fun pending(c: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(c, id, Intent(c, ReminderReceiver::class.java).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun scheduleAll(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val st = Store(c)
        for (r in all) {
            val pi = pending(c, r.id)
            am.cancel(pi)
            if (!st.getB("rem_${r.id}_on", true)) continue
            val h = st.getI("rem_${r.id}_h", r.h)
            val m = st.getI("rem_${r.id}_m", r.m)
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
        }
    }

    fun show(c: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", 0)
        val r = Notify.all.firstOrNull { it.id == id } ?: return
        var text = r.text
        if (id == 1) {
            val ex = Store(c).extras(Plan.key(Calendar.getInstance()))
            if (ex.isNotEmpty()) text += "\nکارهای اضافه‌ی امروز: " + ex.joinToString("، ")
        }
        Notify.show(c, id, r.title, text)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) Notify.scheduleAll(c)
    }
}

package ir.masir.bandegi

import java.util.Calendar
import java.util.Locale

data class Task(val id: String, val title: String, val group: String)

object Plan {
    fun key(c: Calendar): String = String.format(Locale.US, "%04d-%02d-%02d",
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))

    fun jal(c: Calendar) = Jalali.from(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))

    fun title(c: Calendar): String {
        val (y, m, d) = jal(c)
        return "${Jalali.DAYS[c.get(Calendar.DAY_OF_WEEK) - 1]} ${fa(d)} ${Jalali.MONTHS[m - 1]} ${fa(y)}"
    }

    fun epochDay(c: Calendar): Long = (c.timeInMillis + c.timeZone.getOffset(c.timeInMillis)) / 86400000L

    fun tasksFor(c: Calendar, st: Store, dateKey: String): List<Task> {
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val jd = jal(c).third
        val ep = epochDay(c)
        val l = mutableListOf<Task>()

        l.add(Task("p_fajr", "نماز صبح — اول وقت و با توجه", "نماز"))
        l.add(Task("p_zohr", "نماز ظهر — اول وقت و با توجه", "نماز"))
        l.add(Task("p_asr", "نماز عصر — اول وقت و با توجه", "نماز"))
        l.add(Task("p_maghrib", "نماز مغرب — اول وقت و با توجه", "نماز"))
        l.add(Task("p_isha", "نماز عشا — اول وقت و با توجه", "نماز"))
        l.add(Task("p_imam", "یاد امام زمان (عج) پس از هر نماز", "نماز"))
        l.add(Task("p_istighfar_prayer", "نماز استغفار پس از نماز مغرب و عشا", "نماز"))

        l.add(Task("d_istighfar", "۱۰۰ مرتبه استغفار", "ذکر"))
        l.add(Task("d_salavat_imam", "۱۰۰ صلوات هدیه به امام زمان (عج)", "ذکر"))
        l.add(Task("d_salavat_zahra", "۱۰۰ صلوات هدیه به حضرت زهرا (س)", "ذکر"))

        val surahs = listOf("واقعه", "نور", "ذاریات")
        l.add(Task("q_page", "یک صفحه قرآن همراه تفسیر (سوره‌ی این نوبت: ${surahs[(ep % 3).toInt()]})", "قرآن"))
        l.add(Task("q_read", "مطالعه‌ی کوتاه روزانه", "قرآن"))
        l.add(Task("q_khalvat", "خلوت شبانه با خدا", "دعا"))
        l.add(Task("q_ashura", "زیارت عاشورا", "دعا"))
        l.add(Task("q_dua", "یکی از دعاهای آل‌یاسین، کمیل یا علقمه", "دعا"))
        l.add(Task("q_care", "مراقبت از چشم، فکر و زبان", "اخلاق"))

        if (jd % 2 == 0) {
            l.add(Task("e_night", "نماز شب", "روز زوج"))
            l.add(Task("e_mosque", "نماز صبح در مسجد", "روز زوج"))
            l.add(Task("e_fast", "روزه (با رعایت سلامت و احکام مربوط)", "روز زوج"))
        }
        if (ep % 2L == 0L) l.add(Task("alt_sadaqa", "صدقه (یک روز در میان)", "یک روز در میان"))

        when (dow) {
            Calendar.TUESDAY -> l.add(Task("w_base", "حضور در پایگاه (شب)", "هفتگی"))
            Calendar.WEDNESDAY -> l.add(Task("w_shahr", "هیئت شهرری (شب)", "هفتگی"))
            Calendar.THURSDAY -> l.add(Task("w_masjed", "هیئت مسجد (شب)", "هفتگی"))
            Calendar.FRIDAY -> {
                l.add(Task("w_behesht", "زیارت بهشت زهرا (س) — صبح", "هفتگی"))
                l.add(Task("w_abdol", "زیارت حرم حضرت عبدالعظیم حسنی (ع) — صبح", "هفتگی"))
                l.add(Task("w_media", "برگزاری کلاس رسانه — عصر", "هفتگی"))
            }
        }
        if (dow == Calendar.SUNDAY || dow == Calendar.WEDNESDAY) l.add(Task("w_greenhouse", "سر زدن به شبکه گلخانه", "هفتگی"))

        when (dow) {
            Calendar.SATURDAY -> l.add(Task("s_sec", "یادگیری: امنیت شبکه", "مهارت"))
            Calendar.SUNDAY -> l.add(Task("s_heb", "یادگیری: زبان عبری", "مهارت"))
            Calendar.MONDAY -> {
                l.add(Task("s_sec", "یادگیری: امنیت شبکه", "مهارت"))
                l.add(Task("s_ai", "یادگیری: هوش مصنوعی", "مهارت"))
            }
            Calendar.TUESDAY -> l.add(Task("s_heb", "یادگیری: زبان عبری", "مهارت"))
            Calendar.WEDNESDAY -> l.add(Task("s_osint", "یادگیری: OSINT", "مهارت"))
            Calendar.THURSDAY -> {
                l.add(Task("s_sec", "یادگیری: امنیت شبکه", "مهارت"))
                l.add(Task("s_photo", "یادگیری: عکاسی", "مهارت"))
            }
        }

        for (t in st.custom()) l.add(Task("c_$t", t, "شخصی"))
        for (t in st.extras(dateKey)) l.add(Task("x_$t", t, "اضافه"))
        return l
    }

    fun dayScore(st: Store, c: Calendar): Float? {
        val ts = tasksFor(c, st, key(c))
        if (ts.isEmpty()) return null
        val lg = st.log(key(c))
        var sum = 0f
        for (t in ts) {
            sum += when (lg[t.id]?.status) { 1 -> 1f; 2 -> 0.5f; else -> 0f }
        }
        return sum / ts.size
    }

    val MESSAGES = listOf(
        "کوچک و پیوسته بودن، از بزرگ و ناپایدار بودن بهتر است.",
        "امروز یک کار را کامل و با توجه انجام بده، نه ده کار را شتاب‌زده.",
        "اگر دیروز کم شد، امروز فرصتی تازه است؛ سرزنش لازم نیست.",
        "نیت‌ات را قبل از هر کار یک بار تازه کن.",
        "شکرگزاری برای یک نعمت کوچک، دل را آرام می‌کند.",
        "به پدر و مادرت یک تماس یا یک لبخند هدیه بده.",
        "مراقب زبان و چشمت باش؛ همین یک مراقبت، نیمی از مسیر است.",
        "خستگی بخشی از مسیر است؛ استراحتِ به‌اندازه هم عبادتِ هوشمندانه است."
    )
}

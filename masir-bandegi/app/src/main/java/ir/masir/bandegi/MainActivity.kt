package ir.masir.bandegi

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Th(val name: String, val primary: Color, val secondary: Color, val bg: Color)

val Themes = listOf(
    Th("حرم امام رضا (ع) — مشهد", Color(0xFFB8860B), Color(0xFF0F766E), Color(0xFFFFF8E7)),
    Th("حرم امام حسین (ع) — کربلا", Color(0xFF166534), Color(0xFF14532D), Color(0xFFF0FDF4)),
    Th("حرم امام علی (ع) — نجف", Color(0xFF1E40AF), Color(0xFFB8860B), Color(0xFFEFF6FF)),
    Th("کاظمین", Color(0xFF0E7490), Color(0xFF0F766E), Color(0xFFECFEFF)),
    Th("سامرا", Color(0xFF115E59), Color(0xFF854D0E), Color(0xFFF0FDFA)),
    Th("مسجدالحرام — مکه", Color(0xFF1F2937), Color(0xFFB8860B), Color(0xFFF9FAFB)),
    Th("مسجدالنبی — مدینه", Color(0xFF15803D), Color(0xFF065F46), Color(0xFFF7FEE7))
)

val Questions = listOf(
    "امروز چه کار خوبی انجام دادم؟",
    "کجا کوتاهی کردم؟",
    "آیا حق کسی را ضایع کردم؟",
    "آیا به پدر و مادرم احترام گذاشتم؟",
    "آیا از زمانم درست استفاده کردم؟",
    "بابت چه چیزی شکرگزارم؟",
    "فردا چه چیزی را اصلاح می‌کنم؟"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notify.createChannel(this)
        Notify.scheduleAll(this)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val store = Store(this)
        setContent { App(store) }
    }
}

@Composable
fun App(st: Store) {
    var themeIdx by remember { mutableStateOf(st.getI("theme", 0)) }
    val th = Themes[themeIdx.coerceIn(0, Themes.size - 1)]
    var tab by remember { mutableStateOf(0) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = th.primary, secondary = th.secondary,
            background = th.bg, surface = Color.White)) {
            Scaffold(bottomBar = {
                NavigationBar {
                    listOf("🏠" to "امروز", "🌙" to "فردا", "🤲" to "نفس", "📊" to "گزارش", "⚙️" to "بیشتر")
                        .forEachIndexed { i, p ->
                            NavigationBarItem(selected = tab == i, onClick = { tab = i },
                                icon = { Text(p.first) }, label = { Text(p.second, fontSize = 11.sp) })
                        }
                }
            }) { pad ->
                Box(Modifier.padding(pad).fillMaxSize().background(th.bg)) {
                    when (tab) {
                        0 -> TodayScreen(st, th)
                        1 -> TomorrowScreen(st, th)
                        2 -> NafsScreen(st, th)
                        3 -> ReportScreen(st, th)
                        else -> MoreScreen(st, th, themeIdx) { themeIdx = it; st.putI("theme", it) }
                    }
                }
            }
        }
    }
}

@Composable
fun Section(title: String, th: Th) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = th.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
fun Pill(text: String, on: Boolean, color: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50))
        .background(if (on) color else color.copy(alpha = 0.12f))
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(text, color = if (on) Color.White else color, fontSize = 12.sp)
    }
}

@Composable
fun Bar(f: Float, color: Color, h: Dp = 10.dp) {
    Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f))) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

@Composable
fun Header(th: Th, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(Brush.verticalGradient(listOf(th.primary, th.secondary))).padding(16.dp)) {
        Column(content = content)
    }
}

@Composable
fun TaskCard(t: Task, e: Store.Entry?, th: Th, onSet: (Int) -> Unit, onNote: (String) -> Unit, onTomorrow: () -> Unit) {
    val s = e?.status ?: 0
    var note by remember(t.id) { mutableStateOf(e?.note ?: "") }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(t.group, fontSize = 11.sp, color = th.secondary)
            Text(t.title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("✅ انجام شد", s == 1, Color(0xFF15803D)) { onSet(1) }
                Pill("🟡 ناقص", s == 2, Color(0xFFB45309)) { onSet(2) }
                Pill("❌ نشد", s == 3, Color(0xFFB91C1C)) { onSet(3) }
            }
            if (s != 0 && e != null && e.time.isNotEmpty()) {
                Text("ساعت ثبت: ${fa(e.time)}", fontSize = 11.sp, color = Color.Gray)
            }
            if (s == 2 || s == 3) {
                OutlinedTextField(value = note, onValueChange = { note = it; onNote(it) },
                    label = { Text("علت / توضیح") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onTomorrow) { Text("انتقال به فردا") }
            }
        }
    }
}

@Composable
fun TodayScreen(st: Store, th: Th) {
    val ctx = LocalContext.current
    var v by remember { mutableStateOf(0) }
    val cal = remember { Calendar.getInstance() }
    val key = Plan.key(cal)
    val tasks = remember(v) { Plan.tasksFor(cal, st, key) }
    val log = remember(v) { st.log(key) }
    val done = tasks.count { log[it.id]?.status == 1 }
    val partial = tasks.count { log[it.id]?.status == 2 }
    val frac = if (tasks.isEmpty()) 0f else (done + 0.5f * partial) / tasks.size
    val yc = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) } }
    val yLog = remember(v) { st.log(Plan.key(yc)) }
    val overdue = remember(v) {
        Plan.tasksFor(yc, st, Plan.key(yc)).filter { val s = yLog[it.id]?.status; s == 2 || s == 3 }
    }
    val extrasToday = remember(v) { st.extras(key) }
    val msg = Plan.MESSAGES[(Plan.epochDay(cal) % Plan.MESSAGES.size).toInt()]
    val tomorrowKey = remember { Plan.key(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Header(th) {
                Text("مسیر بندگی", color = Color.White, fontSize = 13.sp)
                Text(Plan.title(cal), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Bar(frac, Color.White, 12.dp)
                Spacer(Modifier.height(6.dp))
                Text("پیشرفت امروز: ${fa((frac * 100).toInt())}٪ — انجام‌شده ${fa(done)}، باقی‌مانده ${fa(tasks.size - done)}",
                    color = Color.White, fontSize = 13.sp)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Text("💡 $msg", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
            }
        }
        if (extrasToday.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3C4)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("📌 کارهای اضافه‌ی امروز (از قبل ثبت کرده بودی)", fontWeight = FontWeight.Bold)
                        extrasToday.forEach { Text("• $it") }
                    }
                }
            }
        }
        if (overdue.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⏳ دیروز ${fa(overdue.size)} کار ناقص یا انجام‌نشده داشتی", fontWeight = FontWeight.Bold)
                        overdue.forEach { Text("• ${it.title}", fontSize = 13.sp) }
                        TextButton(onClick = {
                            overdue.forEach { st.addExtra(key, it.title) }
                            v++
                        }) { Text("انتقال به امروز") }
                    }
                }
            }
        }
        items(tasks) { t ->
            TaskCard(t, log[t.id], th,
                onSet = { s ->
                    val cur = st.log(key)[t.id]
                    if (cur?.status == s) st.setEntry(key, t.id, null)
                    else st.setEntry(key, t.id, Store.Entry(s, SimpleDateFormat("HH:mm", Locale.US).format(Date()), cur?.note ?: ""))
                    v++
                },
                onNote = { n ->
                    val cur = st.log(key)[t.id]
                    if (cur != null) st.setEntry(key, t.id, Store.Entry(cur.status, cur.time, n))
                },
                onTomorrow = {
                    st.addExtra(tomorrowKey, t.title)
                    Toast.makeText(ctx, "برای فردا ثبت شد", Toast.LENGTH_SHORT).show()
                })
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun TomorrowScreen(st: Store, th: Th) {
    val c = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) } }
    val key = Plan.key(c)
    var list by remember { mutableStateOf(st.extras(key)) }
    var text by remember { mutableStateOf("") }
    val base = remember { Plan.tasksFor(c, st, key).filter { !it.id.startsWith("x_") } }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Header(th) {
                Text("برنامه‌ریزی فردا", color = Color.White, fontSize = 13.sp)
                Text(Plan.title(c), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column {
                Text("امشب بنویس فردا چه کارهای اضافه‌ای داری. صبح فردا در اعلان و بالای صفحه‌ی «امروز» یادآوری می‌شود.", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("کار اضافه") },
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (text.isNotBlank()) { st.addExtra(key, text.trim()); list = st.extras(key); text = "" }
                    }) { Text("افزودن") }
                }
            }
        }
        items(list) { t ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t, modifier = Modifier.weight(1f))
                    TextButton(onClick = { st.removeExtra(key, t); list = st.extras(key) }) { Text("حذف") }
                }
            }
        }
        item {
            Column {
                Section("برنامه‌ی ثابت فردا", th)
                base.forEach { Text("• ${it.title}", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
fun NafsScreen(st: Store, th: Th) {
    val cal = remember { Calendar.getInstance() }
    val key = Plan.key(cal)
    val answers = remember { mutableStateListOf<String>().apply { for (i in Questions.indices) add(st.getS("nafs_${key}_$i", "")) } }
    val history = remember {
        (1..7).mapNotNull { d ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }
            val k = Plan.key(c)
            val a = Questions.indices.map { st.getS("nafs_${k}_$it", "") }
            if (a.any { it.isNotBlank() }) Pair(Plan.title(c), a) else null
        }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Header(th) {
                Text("محاسبه نفس", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(Plan.title(cal), color = Color.White, fontSize = 13.sp)
                Text("آرام بنشین، نفس بکش و صادقانه بنویس. اینجا جای سرزنش نیست؛ جای دیدن و اصلاح است.",
                    color = Color.White, fontSize = 13.sp)
            }
        }
        items(Questions.size) { i ->
            Column {
                Text(Questions[i], fontWeight = FontWeight.Medium)
                OutlinedTextField(value = answers[i], onValueChange = { answers[i] = it; st.putS("nafs_${key}_$i", it) },
                    modifier = Modifier.fillMaxWidth())
            }
        }
        if (history.isNotEmpty()) {
            item { Section("سوابق ۷ روز گذشته", th) }
            items(history) { h ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(h.first, fontWeight = FontWeight.Bold, color = th.secondary)
                        h.second.forEachIndexed { i, a ->
                            if (a.isNotBlank()) Text("${Questions[i]}\n$a", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

fun insights(avg: Float, weakest: String?): List<String> {
    val l = mutableListOf<String>()
    when {
        avg < 0.4f -> {
            l.add("این هفته حجم برنامه از توان فعلی‌ات بیشتر بوده؛ این طبیعی است و جای نگرانی نیست.")
            l.add("پیشنهاد: چند روز فقط روی نمازها، استغفار و محاسبه نفس تمرکز کن و بقیه را آرام‌آرام برگردان.")
        }
        avg < 0.75f -> {
            l.add("روندت قابل‌قبول است و پایه‌ی خوبی ساخته‌ای.")
            l.add("پیشنهاد: یک یا دو کاری را که بیشتر جا می‌افتد، به ساعت ثابتی در روز گره بزن.")
        }
        else -> {
            l.add("عملکرد این هفته‌ات عالی بوده؛ خدا قبول کند.")
            l.add("پیشنهاد: اگر توان داری، فقط یک کار کوچک و ثابت به برنامه اضافه کن و بیش از آن نه.")
        }
    }
    if (weakest != null) l.add("بخشی که این هفته بیشترین فضای رشد را دارد: «$weakest». یک قدم کوچک و مشخص برای فردا انتخاب کن.")
    return l
}

@Composable
fun ReportScreen(st: Store, th: Th) {
    val days = remember { (6 downTo 0).map { d -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) } } }
    val scores = remember { days.map { Plan.dayScore(st, it) ?: 0f } }
    val avg7 = scores.average().toFloat()
    val avg30 = remember {
        val s = (0..29).map { d -> Plan.dayScore(st, Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }) ?: 0f }
        s.average().toFloat()
    }
    val cats = remember {
        val pts = mutableMapOf<String, Float>()
        val tot = mutableMapOf<String, Int>()
        for (c in days) {
            val k = Plan.key(c)
            val lg = st.log(k)
            for (t in Plan.tasksFor(c, st, k)) {
                tot[t.group] = (tot[t.group] ?: 0) + 1
                val p = when (lg[t.id]?.status) { 1 -> 1f; 2 -> 0.5f; else -> 0f }
                pts[t.group] = (pts[t.group] ?: 0f) + p
            }
        }
        tot.filter { it.value >= 3 }.map { Pair(it.key, (pts[it.key] ?: 0f) / it.value) }.sortedBy { it.second }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Header(th) {
                Text("گزارش", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("پایبندی ۷ روز اخیر: ${fa((avg7 * 100).toInt())}٪", color = Color.White)
                Text("پایبندی ۳۰ روز اخیر: ${fa((avg30 * 100).toInt())}٪", color = Color.White)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom) {
                days.forEachIndexed { i, c ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(24.dp).height((100f * scores[i]).coerceAtLeast(3f).dp)
                            .clip(RoundedCornerShape(6.dp)).background(th.primary))
                        Text(fa(Plan.jal(c).third), fontSize = 11.sp)
                    }
                }
            }
        }
        item { Section("به تفکیک بخش (۷ روز اخیر)", th) }
        items(cats) { p ->
            Column {
                Text("${p.first} — ${fa((p.second * 100).toInt())}٪", fontSize = 13.sp)
                Bar(p.second, th.secondary)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("🧭 تحلیل و پیشنهاد (آفلاین، بر پایه‌ی قانون)", fontWeight = FontWeight.Bold)
                    insights(avg7, cats.firstOrNull()?.takeIf { it.second < 0.6f }?.first).forEach {
                        Text("• $it", fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MoreScreen(st: Store, th: Th, themeIdx: Int, onTheme: (Int) -> Unit) {
    val ctx = LocalContext.current
    var v by remember { mutableStateOf(0) }
    var cnt by remember { mutableStateOf(st.getI("counter", 0)) }
    var custom by remember { mutableStateOf(st.custom()) }
    var newTask by remember { mutableStateOf("") }
    var restore by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Section("ذکرشمار", th) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fa(cnt), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = th.primary)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { cnt++; st.putI("counter", cnt) }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                        Text("+۱", fontSize = 22.sp)
                    }
                    TextButton(onClick = { cnt = 0; st.putI("counter", 0) }) { Text("صفر کردن") }
                }
            }
        }
        item { Section("تم", th) }
        items(Themes.size) { i ->
            Row(Modifier.fillMaxWidth().clickable { onTheme(i) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(Themes[i].primary, Themes[i].secondary))))
                Spacer(Modifier.width(10.dp))
                Text(Themes[i].name, fontWeight = if (i == themeIdx) FontWeight.Bold else FontWeight.Normal)
            }
        }
        item { Section("یادآوری‌ها", th) }
        items(Notify.all) { r ->
            val on = remember(v) { st.getB("rem_${r.id}_on", true) }
            val h = remember(v) { st.getI("rem_${r.id}_h", r.h) }
            val m = remember(v) { st.getI("rem_${r.id}_m", r.m) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(r.title, modifier = Modifier.weight(1f), fontSize = 14.sp)
                TextButton(onClick = {
                    TimePickerDialog(ctx, { _, nh, nm ->
                        st.putI("rem_${r.id}_h", nh); st.putI("rem_${r.id}_m", nm)
                        Notify.scheduleAll(ctx); v++
                    }, h, m, true).show()
                }) { Text(fa(String.format(Locale.US, "%02d:%02d", h, m))) }
                Switch(checked = on, onCheckedChange = {
                    st.putB("rem_${r.id}_on", it); Notify.scheduleAll(ctx); v++
                })
            }
        }
        item { Text("ساعت نمازها را متناسب با فصل و شهر خودت تنظیم کن؛ اپ اوقات شرعی را خودکار محاسبه نمی‌کند.", fontSize = 12.sp, color = Color.Gray) }
        item { Section("فعالیت شخصی روزانه", th) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newTask, onValueChange = { newTask = it }, label = { Text("فعالیت جدید") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { st.addCustom(newTask.trim()); custom = st.custom(); newTask = "" }) { Text("افزودن") }
            }
        }
        items(custom) { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t, modifier = Modifier.weight(1f))
                TextButton(onClick = { st.removeCustom(t); custom = st.custom() }) { Text("حذف") }
            }
        }
        item { Section("پشتیبان‌گیری و بازیابی", th) }
        item {
            Button(onClick = {
                val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, st.exportAll()) }
                ctx.startActivity(Intent.createChooser(i, "ارسال پشتیبان"))
            }) { Text("گرفتن پشتیبان (JSON)") }
        }
        item {
            OutlinedTextField(value = restore, onValueChange = { restore = it }, label = { Text("متن پشتیبان را اینجا بچسبان") },
                modifier = Modifier.fillMaxWidth())
        }
        item {
            Button(onClick = {
                val ok = st.importAll(restore)
                Toast.makeText(ctx, if (ok) "بازیابی شد؛ برنامه را ببند و دوباره باز کن" else "متن پشتیبان معتبر نیست", Toast.LENGTH_LONG).show()
            }) { Text("بازیابی") }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

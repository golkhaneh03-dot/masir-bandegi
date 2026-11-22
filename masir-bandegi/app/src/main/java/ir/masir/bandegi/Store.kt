package ir.masir.bandegi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Store(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("masir", Context.MODE_PRIVATE)

    data class Entry(val status: Int, val time: String, val note: String)

    fun getS(k: String, d: String) = sp.getString(k, d) ?: d
    fun putS(k: String, v: String) { sp.edit().putString(k, v).apply() }
    fun getI(k: String, d: Int) = sp.getInt(k, d)
    fun putI(k: String, v: Int) { sp.edit().putInt(k, v).apply() }
    fun getB(k: String, d: Boolean) = sp.getBoolean(k, d)
    fun putB(k: String, v: Boolean) { sp.edit().putBoolean(k, v).apply() }

    fun log(date: String): MutableMap<String, Entry> {
        val m = mutableMapOf<String, Entry>()
        val js = sp.getString("log_$date", null) ?: return m
        try {
            val o = JSONObject(js)
            for (k in o.keys()) {
                val e = o.getJSONObject(k)
                m[k] = Entry(e.optInt("s"), e.optString("t"), e.optString("n"))
            }
        } catch (_: Exception) {}
        return m
    }

    fun setEntry(date: String, id: String, e: Entry?) {
        val m = log(date)
        if (e == null || e.status == 0) m.remove(id) else m[id] = e
        val o = JSONObject()
        for ((k, v) in m) o.put(k, JSONObject().put("s", v.status).put("t", v.time).put("n", v.note))
        sp.edit().putString("log_$date", o.toString()).apply()
    }

    private fun list(key: String): MutableList<String> {
        val r = mutableListOf<String>()
        try {
            val a = JSONArray(sp.getString(key, "[]"))
            for (i in 0 until a.length()) r.add(a.getString(i))
        } catch (_: Exception) {}
        return r
    }
    private fun saveList(key: String, l: List<String>) {
        sp.edit().putString(key, JSONArray(l).toString()).apply()
    }

    fun extras(date: String): MutableList<String> = list("extra_$date")
    fun addExtra(date: String, t: String) {
        val l = extras(date)
        if (t.isNotBlank() && !l.contains(t)) { l.add(t); saveList("extra_$date", l) }
    }
    fun removeExtra(date: String, t: String) {
        val l = extras(date); l.remove(t); saveList("extra_$date", l)
    }

    fun custom(): MutableList<String> = list("custom")
    fun addCustom(t: String) {
        val l = custom()
        if (t.isNotBlank() && !l.contains(t)) { l.add(t); saveList("custom", l) }
    }
    fun removeCustom(t: String) { val l = custom(); l.remove(t); saveList("custom", l) }

    fun exportAll(): String {
        val o = JSONObject()
        for ((k, v) in sp.all) o.put(k, v)
        return o.toString(2)
    }

    fun importAll(s: String): Boolean = try {
        val o = JSONObject(s)
        val e = sp.edit()
        for (k in o.keys()) {
            when (val v = o.get(k)) {
                is String -> e.putString(k, v)
                is Int -> e.putInt(k, v)
                is Boolean -> e.putBoolean(k, v)
                else -> {}
            }
        }
        e.apply()
        true
    } catch (_: Exception) { false }
}

package app.laitno

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class Card(val box: Int, val due: Long)

/** جعبه‌ی ۱ تا ۵ = در حال یادگیری، جعبه‌ی ۶ = یادگرفته‌شده */
class Progress {
    val cards = HashMap<String, Card>()
    val testBest = HashMap<Int, Int>()
    var ok = 0
    var bad = 0
    var updated = 0L

    companion object {
        const val DAY = 86_400_000L
        fun interval(box: Int) = when (box) { 1 -> 1; 2 -> 2; 3 -> 4; 4 -> 8; 5 -> 16; else -> 0 }
        fun today(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun fromJson(s: String?): Progress {
            val p = Progress()
            if (s.isNullOrBlank()) return p
            runCatching {
                val o = JSONObject(s)
                val c = o.optJSONObject("c") ?: JSONObject()
                c.keys().forEach { k -> val a = c.getJSONArray(k); p.cards[k] = Card(a.getInt(0), a.getLong(1)) }
                val tb = o.optJSONObject("tb") ?: JSONObject()
                tb.keys().forEach { k -> p.testBest[k.toInt()] = tb.getInt(k) }
                p.ok = o.optInt("ok"); p.bad = o.optInt("bad"); p.updated = o.optLong("t")
            }
            return p
        }
    }

    fun toJson(): String {
        val c = JSONObject()
        cards.forEach { (k, v) -> c.put(k, JSONArray().put(v.box).put(v.due)) }
        val tb = JSONObject()
        testBest.forEach { (k, v) -> tb.put(k.toString(), v) }
        return JSONObject().put("v", 1).put("c", c).put("tb", tb).put("ok", ok).put("bad", bad).put("t", updated).toString()
    }

    fun add(ids: List<String>): Int {
        var n = 0
        val now = System.currentTimeMillis()
        ids.forEach { if (!cards.containsKey(it)) { cards[it] = Card(1, now); n++ } }
        return n
    }

    fun answer(id: String, correct: Boolean) {
        val cur = cards[id] ?: Card(1, 0)
        val nb = if (correct) minOf(cur.box + 1, 6) else 1
        val due = if (nb >= 6) Long.MAX_VALUE else today() + DAY * interval(if (correct) nb else 1)
        cards[id] = Card(nb, due)
        if (correct) ok++ else bad++
    }

    fun due(): List<String> {
        val now = System.currentTimeMillis()
        return cards.filter { it.value.box <= 5 && it.value.due <= now }
            .entries.sortedWith(compareBy({ it.value.box }, { it.value.due })).map { it.key }
    }

    fun inBox(b: Int) = cards.filter { it.value.box == b }.keys.toList()
    fun learnedIn(ids: Collection<String>) = ids.count { (cards[it]?.box ?: 0) >= 6 }
    fun startedIn(ids: Collection<String>) = ids.count { cards.containsKey(it) }
}

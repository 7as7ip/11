package app.laitno

import android.content.Context
import org.json.JSONArray

data class Example(val en: String, val fa: String)
data class Sense(val pos: String, val syn: String, val fa: String, val examples: List<Example>)
data class Word(
    val id: String, val en: String, val ipa: String, val faPron: String,
    val senses: List<Sense>, val lesson: Int, val level: String
) {
    val meaning: String get() = senses.firstOrNull()?.fa ?: ""
}
data class Lesson(val n: Int, val level: String, val reviewOf: String, val words: List<Word>)
data class WordCollection(val key: String, val title: String, val desc: String, val color: Long, val lessons: List<Lesson>) {
    val wordCount: Int get() = lessons.sumOf { it.words.size }
}
data class Question(val text: String, val options: List<String>, val answer: Int)
data class WordTest(val n: Int, val level: String, val lessons: String, val questions: List<Question>)
data class MorphTable(val title: String, val kind: String, val rows: List<List<String>>)

object Repo {
    var collections: List<WordCollection> = emptyList(); private set
    var lessons: Map<Int, Lesson> = emptyMap(); private set
    var words: Map<String, Word> = emptyMap(); private set
    var tests: List<WordTest> = emptyList(); private set
    var morph: List<MorphTable> = emptyList(); private set

    private val defs = listOf(
        Triple("intro", "مقدماتی", 0xFFC9D36B),
        Triple("main", "اصلی", 0xFF9AA6BB),
        Triple("adv", "پیشرفته", 0xFFE99A3C),
        Triple("sup", "تکمیلی", 0xFFE5546A),
        Triple("b504", "۵۰۴ واژه", 0xFF55B6F2),
        Triple("basic", "لغات پایه", 0xFF7C9CFF),
        Triple("msrt", "لغات MSRT", 0xFF2FBF9B),
    )
    private val descs = mapOf(
        "intro" to "مهم‌ترین لغات دبیرستان و پایه",
        "main" to "پرتکرارترین لغات همهٔ آزمون‌ها",
        "adv" to "برای ورود با اعتمادبه‌نفس به جلسه",
        "sup" to "لغات سخت‌تر برای چالش بیشتر",
        "b504" to "۵۰۴ واژهٔ کاملاً ضروری",
        "basic" to "لغات پایه و پرکاربرد",
        "msrt" to "لغات آزمون MSRT",
    )

    private fun text(ctx: Context, name: String) =
        ctx.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun parseLessons(arr: JSONArray): List<Lesson> {
        val out = ArrayList<Lesson>()
        for (i in 0 until arr.length()) {
            val l = arr.optJSONArray(i) ?: continue
            val n = l.optInt(0)
            val lv = l.optString(1).let { if (it.startsWith("msrt")) "msrt" else it }
            val wa = l.optJSONArray(3) ?: JSONArray()
            val ws = ArrayList<Word>()
            for (j in 0 until wa.length()) {
                val w = wa.optJSONArray(j) ?: continue
                val sa = w.optJSONArray(3) ?: JSONArray()
                val senses = ArrayList<Sense>()
                for (k in 0 until sa.length()) {
                    val s = sa.optJSONArray(k) ?: continue
                    val ex = s.optJSONArray(3) ?: JSONArray()
                    val exs = ArrayList<Example>()
                    for (m in 0 until ex.length()) {
                        val e = ex.optJSONArray(m) ?: continue
                        exs.add(Example(e.optString(0), e.optString(1)))
                    }
                    senses.add(Sense(s.optString(0), s.optString(1), s.optString(2), exs))
                }
                ws.add(Word("$n-$j", w.optString(0), w.optString(1), w.optString(2), senses, n, lv))
            }
            out.add(Lesson(n, lv, l.optString(2), ws))
        }
        return out
    }

    fun load(ctx: Context) {
        if (collections.isNotEmpty()) return
        val all = ArrayList<Lesson>()
        for (f in listOf("RAW_LES.json", "RAW_504.json", "RAW_BASIC.json", "RAW_MSRT.json"))
            all.addAll(parseLessons(JSONArray(text(ctx, f))))
        lessons = all.associateBy { it.n }
        words = all.flatMap { it.words }.associateBy { it.id }
        collections = defs.map { (k, t, c) ->
            WordCollection(k, t, descs[k] ?: "", c, all.filter { it.level == k })
        }.filter { it.lessons.isNotEmpty() }

        val ta = JSONArray(text(ctx, "RAW_TESTS.json"))
        val tl = ArrayList<WordTest>()
        for (i in 0 until ta.length()) {
            val t = ta.optJSONArray(i) ?: continue
            val qa = t.optJSONArray(4) ?: JSONArray()
            val qs = ArrayList<Question>()
            for (j in 0 until qa.length()) {
                val q = qa.optJSONArray(j) ?: continue
                val o = q.optJSONArray(1) ?: JSONArray()
                qs.add(Question(q.optString(0), (0 until o.length()).map { o.optString(it) }, q.optInt(2) - 1))
            }
            tl.add(WordTest(t.optInt(0), t.optString(1), t.optString(2), qs))
        }
        tests = tl

        val ma = JSONArray(text(ctx, "RAW_MORPH.json"))
        morph = (0 until ma.length()).mapNotNull { i ->
            val o = ma.optJSONObject(i) ?: return@mapNotNull null
            val rows = o.optJSONArray("rows") ?: JSONArray()
            MorphTable(o.optString("title"), o.optString("kind"), (0 until rows.length()).map { r ->
                val row = rows.optJSONArray(r) ?: JSONArray()
                (0 until row.length()).map { row.optString(it) }
            })
        }
    }

    fun search(q: String): List<Word> {
        val s = q.trim().lowercase()
        if (s.isEmpty()) return emptyList()
        val all = words.values
        val starts = all.filter { it.en.lowercase().startsWith(s) }
        val rest = all.filter { !it.en.lowercase().startsWith(s) && (it.en.lowercase().contains(s) || it.senses.any { x -> x.fa.contains(s) }) }
        return (starts.sortedBy { it.en.length } + rest).distinctBy { it.en.lowercase() + it.lesson }.take(80)
    }
}

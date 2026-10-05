package app.laitno

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

sealed class Screen {
    data object Login : Screen()
    data object Home : Screen()
    data class Coll(val key: String) : Screen()
    data class LessonS(val n: Int) : Screen()
    data class WordS(val id: String) : Screen()
    data class Review(val ids: List<String>, val practice: Boolean, val title: String) : Screen()
    data object Boxes : Screen()
    data class BoxList(val box: Int) : Screen()
    data object Tests : Screen()
    data class TestRun(val n: Int) : Screen()
    data object Search : Screen()
    data object Morph : Screen()
    data object Profile : Screen()
}

class AppVM(app: Application) : AndroidViewModel(app), TextToSpeech.OnInitListener {
    private val prefs = app.getSharedPreferences("laitno", Context.MODE_PRIVATE)
    val cloud = Cloud(prefs)

    var loaded by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    var toast by mutableStateOf<String?>(null)
    var rev by mutableIntStateOf(0); private set
    val stack = mutableStateListOf<Screen>()
    var progress = Progress(); private set

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pushJob: Job? = null

    val screen: Screen? get() = stack.lastOrNull()
    val userName: String get() = cloud.session?.name ?: ""

    init {
        tts = TextToSpeech(app, this)
        viewModelScope.launch {
            withContext(Dispatchers.Default) { Repo.load(getApplication()) }
            loaded = true
            if (cloud.session != null) {
                loadLocal(); stack.add(Screen.Home); syncDown()
            } else stack.add(Screen.Login)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.9f)
            ttsReady = true
        }
    }

    fun speak(text: String) {
        if (!ttsReady) { toast = "موتور تلفظ گوشی آماده نیست"; return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "w")
    }

    // ---------- navigation ----------
    fun go(s: Screen) { stack.add(s) }
    fun back(): Boolean {
        if (stack.size > 1) { stack.removeAt(stack.lastIndex); return true }
        return false
    }
    private fun reset(s: Screen) { stack.clear(); stack.add(s) }

    // ---------- storage ----------
    private fun key() = "state_" + (cloud.session?.uid ?: "none")
    private fun loadLocal() { progress = Progress.fromJson(prefs.getString(key(), null)); rev++ }
    private fun saveLocal() { prefs.edit().putString(key(), progress.toJson()).putBoolean(key() + "_dirty", true).apply() }

    private fun changed() {
        progress.updated = System.currentTimeMillis()
        saveLocal(); rev++
        pushJob?.cancel()
        pushJob = viewModelScope.launch { delay(3000); pushNow() }
    }

    private suspend fun pushNow() {
        val js = progress.toJson(); val t = progress.updated
        val ok = withContext(Dispatchers.IO) { runCatching { cloud.push(js, t) }.isSuccess }
        if (ok) prefs.edit().putBoolean(key() + "_dirty", false).apply()
    }

    fun flush() {
        if (cloud.session == null || !prefs.getBoolean(key() + "_dirty", false)) return
        pushJob?.cancel()
        viewModelScope.launch { pushNow() }
    }

    private fun syncDown() {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { cloud.pull() } }
            val remote = r.getOrNull()
            if (remote != null && remote.second > progress.updated && !prefs.getBoolean(key() + "_dirty", false)) {
                progress = Progress.fromJson(remote.first); saveLocal()
                prefs.edit().putBoolean(key() + "_dirty", false).apply(); rev++
            } else if (prefs.getBoolean(key() + "_dirty", false)) pushNow()
        }
    }

    // ---------- auth ----------
    fun auth(signup: Boolean, user: String, pass: String, pass2: String) {
        error = null
        val u = user.trim()
        if (!Regex("^[a-zA-Z0-9._]{3,20}$").matches(u)) { error = "نام کاربری معتبر نیست (فقط حروف انگلیسی، عدد، . یا _)"; return }
        if (pass.length < 6) { error = "رمز باید حداقل ۶ کاراکتر باشه."; return }
        if (signup && pass != pass2) { error = "رمزها یکی نیستن."; return }
        busy = true
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    if (signup) cloud.signUp(u, pass) else cloud.signIn(u, pass)
                    cloud.pull()
                }
            }
            busy = false
            r.onSuccess { remote ->
                loadLocal()
                if (remote != null && remote.second >= progress.updated) { progress = Progress.fromJson(remote.first); saveLocal(); rev++ }
                prefs.edit().putBoolean(key() + "_dirty", false).apply()
                if (signup) changed()
                reset(Screen.Home)
            }.onFailure { error = Cloud.message(it) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            pushJob?.cancel(); pushNow()
            prefs.edit().remove(key()).remove(key() + "_dirty").apply()
            cloud.logout(); progress = Progress(); rev++
            reset(Screen.Login)
        }
    }

    fun deleteAccount() {
        busy = true
        viewModelScope.launch {
            pushJob?.cancel()
            val k = key()
            val r = withContext(Dispatchers.IO) { runCatching { cloud.deleteAccount() } }
            busy = false
            r.onSuccess {
                prefs.edit().remove(k).remove(k + "_dirty").apply()
                progress = Progress(); rev++; reset(Screen.Login); toast = "حسابت کامل حذف شد."
            }.onFailure { toast = Cloud.message(it) }
        }
    }

    // ---------- leitner ----------
    fun addLesson(n: Int) {
        val ids = Repo.lessons[n]?.words?.map { it.id } ?: return
        val added = progress.add(ids)
        toast = if (added > 0) "${fa(added)} لغت به جعبه‌ی ۱ اضافه شد" else "همه‌ی لغات این درس از قبل در لایتنر هستن"
        if (added > 0) changed()
    }

    fun answer(id: String, correct: Boolean) { progress.answer(id, correct); changed() }

    fun saveTest(n: Int, score: Int) {
        if (score > (progress.testBest[n] ?: -1)) { progress.testBest[n] = score; changed() }
    }

    override fun onCleared() { tts?.shutdown(); super.onCleared() }
}

fun fa(n: Int): String = n.toString().map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")

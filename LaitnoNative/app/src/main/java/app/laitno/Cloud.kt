package app.laitno

import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class CloudException(val code: String) : Exception(code)

/** ورود/ثبت‌نام و ذخیره‌ی پیشرفت روی Firebase (بدون SDK، با REST) */
class Cloud(private val prefs: SharedPreferences) {
    data class Session(val uid: String, val name: String, var idToken: String, var refresh: String, var exp: Long)

    var session: Session? = loadSession(); private set

    private fun loadSession(): Session? {
        val uid = prefs.getString("s_uid", null) ?: return null
        return Session(uid, prefs.getString("s_name", "") ?: "", prefs.getString("s_tok", "") ?: "",
            prefs.getString("s_ref", "") ?: "", prefs.getLong("s_exp", 0))
    }

    private fun saveSession(s: Session?) {
        session = s
        val e = prefs.edit()
        if (s == null) listOf("s_uid", "s_name", "s_tok", "s_ref", "s_exp").forEach { e.remove(it) }
        else e.putString("s_uid", s.uid).putString("s_name", s.name).putString("s_tok", s.idToken)
            .putString("s_ref", s.refresh).putLong("s_exp", s.exp)
        e.apply()
    }

    private fun http(method: String, url: String, body: String? = null, token: String? = null, form: Boolean = false): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15000; c.readTimeout = 20000
            if (method == "PATCH") { c.requestMethod = "POST"; c.setRequestProperty("X-HTTP-Method-Override", "PATCH") }
            else c.requestMethod = method
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", if (form) "application/x-www-form-urlencoded" else "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val s = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return code to s
        } catch (e: java.io.IOException) {
            throw CloudException("NETWORK")
        } finally { c.disconnect() }
    }

    private fun err(s: String): CloudException {
        val m = runCatching { JSONObject(s).getJSONObject("error").getString("message") }.getOrDefault("UNKNOWN")
        return CloudException(m.substringBefore(" ").substringBefore(":"))
    }

    private fun email(u: String) = u.trim().lowercase() + "@users.laitno.app"
    private val auth = "https://identitytoolkit.googleapis.com/v1/accounts"

    private fun authCall(op: String, user: String, pass: String): Session {
        val body = JSONObject().put("email", email(user)).put("password", pass).put("returnSecureToken", true).toString()
        val (code, s) = http("POST", "$auth:$op?key=${Config.API_KEY}", body)
        if (code >= 400) throw err(s)
        val o = JSONObject(s)
        val ses = Session(o.getString("localId"), user.trim(), o.getString("idToken"), o.getString("refreshToken"),
            System.currentTimeMillis() + o.optString("expiresIn", "3600").toLong() * 1000)
        saveSession(ses)
        return ses
    }

    fun signUp(user: String, pass: String) = authCall("signUp", user, pass)
    fun signIn(user: String, pass: String) = authCall("signInWithPassword", user, pass)

    private fun token(): String {
        val s = session ?: throw CloudException("NO_SESSION")
        if (System.currentTimeMillis() < s.exp - 60_000) return s.idToken
        val (code, r) = http("POST", "https://securetoken.googleapis.com/v1/token?key=${Config.API_KEY}",
            "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(s.refresh, "UTF-8"), form = true)
        if (code >= 400) throw err(r)
        val o = JSONObject(r)
        s.idToken = o.getString("id_token"); s.refresh = o.getString("refresh_token")
        s.exp = System.currentTimeMillis() + o.optString("expires_in", "3600").toLong() * 1000
        saveSession(s)
        return s.idToken
    }

    private fun doc(uid: String) =
        "https://firestore.googleapis.com/v1/projects/${Config.PROJECT_ID}/databases/(default)/documents/users/$uid"

    /** برمی‌گردونه: (state, updated) یا null اگه هنوز چیزی ذخیره نشده */
    fun pull(): Pair<String, Long>? {
        val s = session ?: return null
        val (code, r) = http("GET", doc(s.uid), token = token())
        if (code == 404) return null
        if (code >= 400) throw err(r)
        val f = JSONObject(r).optJSONObject("fields") ?: return null
        val st = f.optJSONObject("state")?.optString("stringValue") ?: return null
        val t = f.optJSONObject("t")?.optString("integerValue")?.toLongOrNull() ?: 0L
        return st to t
    }

    fun push(state: String, updated: Long) {
        val s = session ?: return
        val fields = JSONObject()
            .put("state", JSONObject().put("stringValue", state))
            .put("t", JSONObject().put("integerValue", updated.toString()))
            .put("name", JSONObject().put("stringValue", s.name))
        val (code, r) = http("PATCH", doc(s.uid), JSONObject().put("fields", fields).toString(), token = token())
        if (code >= 400) throw err(r)
    }

    fun deleteAccount() {
        val s = session ?: return
        val tk = token()
        http("DELETE", doc(s.uid), token = tk)
        val (code, r) = http("POST", "$auth:delete?key=${Config.API_KEY}", JSONObject().put("idToken", tk).toString())
        if (code >= 400) throw err(r)
        saveSession(null)
    }

    fun logout() = saveSession(null)

    companion object {
        fun message(e: Throwable): String = when ((e as? CloudException)?.code) {
            "EMAIL_EXISTS" -> "این نام کاربری قبلاً گرفته شده."
            "INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD", "EMAIL_NOT_FOUND" -> "نام کاربری یا رمز اشتباهه."
            "WEAK_PASSWORD" -> "رمز باید حداقل ۶ کاراکتر باشه."
            "TOO_MANY_ATTEMPTS_TRY_LATER" -> "تلاش زیاد بود، چند دقیقه دیگه امتحان کن."
            "CREDENTIAL_TOO_OLD_LOGIN_AGAIN", "TOKEN_EXPIRED" -> "یه بار خارج شو و دوباره وارد شو."
            "NETWORK" -> "اتصال به سرور برقرار نشد. اینترنتت رو چک کن."
            "API", "API_KEY_INVALID" -> "مشخصات Firebase در Config.kt درست وارد نشده."
            else -> "یه مشکلی پیش اومد، دوباره امتحان کن."
        }
    }
}

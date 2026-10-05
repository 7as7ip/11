package app.laitno

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val paper = Color(0xFF0D1424)
    val paper2 = Color(0xFF141D31)
    val paper3 = Color(0xFF1C2840)
    val line = Color(0xFF24324D)
    val card = Color(0xFF111A2D)
    val ink = Color(0xFFE8EEFA)
    val ink2 = Color(0xFFB3BFD6)
    val ink3 = Color(0xFF8190AB)
    val acc = Color(0xFF4D87F6)
    val accSoft = Color(0xFF18284A)
    val ok = Color(0xFF3FC48D)
    val okSoft = Color(0xFF0F3027)
    val bad = Color(0xFFF2726A)
    val badSoft = Color(0xFF3A1B22)
    val boxes = listOf(Color(0xFFEF5A4F), Color(0xFFF59A23), Color(0xFFE8C21A), Color(0xFF22B07D), Color(0xFF16A3B8), Color(0xFF6457E8))
}

val Vazir = FontFamily(
    Font(R.font.vazir_regular, FontWeight.Normal),
    Font(R.font.vazir_medium, FontWeight.Medium),
    Font(R.font.vazir_bold, FontWeight.Bold),
    Font(R.font.vazir_black, FontWeight.Black),
)
val Serif = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_semibold, FontWeight.SemiBold),
)

@Composable
fun LaitnoTheme(content: @Composable () -> Unit) {
    val base = TextStyle(fontFamily = Vazir, color = C.ink)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.acc, onPrimary = Color.White, background = C.paper, surface = C.card,
            onSurface = C.ink, onBackground = C.ink, surfaceVariant = C.paper2, outline = C.line, error = C.bad,
        ),
        typography = Typography(
            bodyLarge = base.copy(fontSize = 16.sp), bodyMedium = base.copy(fontSize = 15.sp),
            labelLarge = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            titleLarge = base.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
        ),
        content = content
    )
}

fun en(size: Int, w: FontWeight = FontWeight.SemiBold) = TextStyle(fontFamily = Serif, fontSize = size.sp, fontWeight = w, color = C.ink)

@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, pad: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val m = modifier.clip(RoundedCornerShape(16.dp)).background(C.card).border(1.dp, C.line, RoundedCornerShape(16.dp))
    Column((if (onClick != null) m.clickable(onClick = onClick) else m).padding(pad), content = content)
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Text("→", fontSize = 22.sp, color = C.ink2)
            }
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

@Composable
fun MainButton(text: String, modifier: Modifier = Modifier, color: Color = C.acc, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = C.ink2, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, C.line), colors = ButtonDefaults.outlinedButtonColors(contentColor = color)) {
        Text(text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Text(text, fontSize = 14.sp, color = color, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp))
}

@Composable
fun SpeakBtn(onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(C.accSoft).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text("🔊", fontSize = 18.sp)
    }
}

val posFa = mapOf("n" to "اسم", "v" to "فعل", "adj" to "صفت", "adv" to "قید", "prep" to "حرف اضافه",
    "conj" to "حرف ربط", "pron" to "ضمیر", "phr" to "عبارت", "phrv" to "فعل عبارتی", "idm" to "اصطلاح", "det" to "معرف")

package app.laitno

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────── ورود / ثبت‌نام
@Composable
fun LoginScreen(vm: AppVM) {
    var signup by remember { mutableStateOf(false) }
    var u by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = C.acc, unfocusedBorderColor = C.line, focusedContainerColor = C.paper, unfocusedContainerColor = C.paper,
        focusedTextColor = C.ink, unfocusedTextColor = C.ink, focusedLabelColor = C.acc, unfocusedLabelColor = C.ink3, cursorColor = C.acc)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.acc), contentAlignment = Alignment.Center) {
                Text("L", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text("LAITNO", fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("لایتنر هوشمند لغات", color = C.ink3, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        Panel(Modifier.fillMaxWidth(), pad = 20.dp) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.paper).padding(4.dp)) {
                listOf(false to "ورود", true to "ثبت‌نام").forEach { (m, t) ->
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (signup == m) C.acc else Color.Transparent)
                        .clickable { signup = m; vm.error = null }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text(t, color = if (signup == m) Color.White else C.ink3, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(u, { u = it }, label = { Text("نام کاربری") }, singleLine = true, colors = fieldColors,
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
            if (signup) Text("فقط حروف انگلیسی، عدد، نقطه یا _ (۳ تا ۲۰ کاراکتر)", fontSize = 14.sp, color = C.ink3, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(p, { p = it }, label = { Text("رمز عبور") }, singleLine = true, colors = fieldColors,
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (signup) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(p2, { p2 = it }, label = { Text("تکرار رمز عبور") }, singleLine = true, colors = fieldColors,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            }
            Spacer(Modifier.height(20.dp))
            MainButton(if (vm.busy) "صبر کن…" else if (signup) "ساخت حساب" else "ورود", Modifier.fillMaxWidth(), enabled = !vm.busy) {
                vm.auth(signup, u, p, p2)
            }
            vm.error?.let { Text(it, color = C.bad, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) }
        }
    }
}

// ───────────────────────── خانه
@Composable
fun HomeScreen(vm: AppVM) {
    @Suppress("UNUSED_VARIABLE") val r = vm.rev
    val due = vm.progress.due()
    val learned = vm.progress.inBox(6).size
    val started = vm.progress.cards.size
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("سلام ${vm.userName} 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${fa(learned)} لغت یاد گرفتی · ${fa(started)} در لایتنر", color = C.ink3, fontSize = 15.sp)
                }
                Box(Modifier.size(46.dp).clip(CircleShape).background(C.paper3).clickable { vm.go(Screen.Profile) }, contentAlignment = Alignment.Center) {
                    Text(vm.userName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.acc).padding(22.dp)) {
                Column {
                    Text("مرور امروز", color = Color.White.copy(alpha = .85f), fontSize = 16.sp)
                    Text(fa(due.size), color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
                    Text(if (due.isEmpty()) "امروز چیزی برای مرور نداری. یه درس جدید شروع کن!" else "لغت منتظر مرورن",
                        color = Color.White.copy(alpha = .85f), fontSize = 15.sp)
                    if (due.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).clickable {
                            vm.go(Screen.Review(due, false, "مرور امروز"))
                        }.padding(horizontal = 22.dp, vertical = 12.dp)) {
                            Text("شروع مرور", color = C.acc, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tool("📦", "جعبه‌ها", Modifier.weight(1f)) { vm.go(Screen.Boxes) }
                Tool("📝", "آزمون‌ها", Modifier.weight(1f)) { vm.go(Screen.Tests) }
                Tool("🔎", "جست‌وجو", Modifier.weight(1f)) { vm.go(Screen.Search) }
                Tool("🧩", "ریشه‌ها", Modifier.weight(1f)) { vm.go(Screen.Morph) }
            }
        }
        item { Text("مجموعه‌ها", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        items(Repo.collections, key = { it.key }) { c ->
            val ids = c.lessons.flatMap { l -> l.words.map { it.id } }
            val done = vm.progress.learnedIn(ids)
            val col = Color(c.color)
            Panel(Modifier.fillMaxWidth(), onClick = { vm.go(Screen.Coll(c.key)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(col))
                    Text(c.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text("${fa(c.lessons.size)} درس", color = C.ink3, fontSize = 14.sp)
                }
                Text(c.desc, color = C.ink3, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { if (ids.isEmpty()) 0f else done.toFloat() / ids.size },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = col, trackColor = C.paper3)
                Text("${fa(done)} از ${fa(ids.size)} لغت", color = C.ink3, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun Tool(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Panel(modifier, onClick = onClick, pad = 12.dp) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(label, fontSize = 14.sp, color = C.ink2, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
    }
}

// ───────────────────────── مجموعه → درس‌ها
@Composable
fun CollectionScreen(vm: AppVM, key: String) {
    @Suppress("UNUSED_VARIABLE") val r = vm.rev
    val c = Repo.collections.firstOrNull { it.key == key } ?: return
    val col = Color(c.color)
    Column(Modifier.fillMaxSize()) {
        TopBar(c.title) { vm.back() }
        LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(c.lessons, key = { it.n }) { l ->
                val ids = l.words.map { it.id }
                val learned = vm.progress.learnedIn(ids); val started = vm.progress.startedIn(ids)
                Panel(Modifier.fillMaxWidth(), onClick = { vm.go(Screen.LessonS(l.n)) }, pad = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(col.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                            Text(fa(c.lessons.indexOf(l) + 1), color = col, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("درس ${fa(c.lessons.indexOf(l) + 1)}", fontWeight = FontWeight.Bold)
                            Text(l.words.take(3).joinToString(" · ") { it.en }, style = en(15, androidx.compose.ui.text.font.FontWeight.Normal).copy(color = C.ink3), maxLines = 1)
                        }
                        when {
                            learned == ids.size && ids.isNotEmpty() -> Chip("✓ کامل", C.ok)
                            started > 0 -> Chip("${fa(started)}/${fa(ids.size)}", C.acc)
                            else -> Chip("${fa(ids.size)} لغت", C.ink3)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── درس
@Composable
fun LessonScreen(vm: AppVM, n: Int) {
    @Suppress("UNUSED_VARIABLE") val r = vm.rev
    val l = Repo.lessons[n] ?: return
    val c = Repo.collections.firstOrNull { it.key == l.level }
    val idx = (c?.lessons?.indexOf(l) ?: 0) + 1
    Column(Modifier.fillMaxSize()) {
        TopBar("${c?.title ?: ""} · درس ${fa(idx)}") { vm.back() }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MainButton("افزودن به لایتنر", Modifier.weight(1f)) { vm.addLesson(n) }
            GhostButton("تمرین سریع", Modifier.weight(1f)) { vm.go(Screen.Review(l.words.map { it.id }.shuffled(), true, "تمرین درس ${fa(idx)}")) }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(l.words, key = { it.id }) { w -> WordRow(vm, w) }
        }
    }
}

@Composable
fun WordRow(vm: AppVM, w: Word) {
    val box = vm.progress.cards[w.id]?.box
    Panel(Modifier.fillMaxWidth(), onClick = { vm.go(Screen.WordS(w.id)) }, pad = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(w.en, style = en(20))
                Text(w.meaning, color = C.ink2, fontSize = 15.sp, maxLines = 1)
            }
            if (box != null) Box(Modifier.padding(end = 8.dp).size(26.dp).clip(CircleShape).background(C.boxes[box - 1]), contentAlignment = Alignment.Center) {
                Text(if (box == 6) "✓" else fa(box), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            SpeakBtn { vm.speak(w.en) }
        }
    }
}

// ───────────────────────── جزئیات لغت
@Composable
fun WordScreen(vm: AppVM, id: String) {
    val w = Repo.words[id] ?: return
    Column(Modifier.fillMaxSize()) {
        TopBar("") { vm.back() }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp)) {
            WordDetail(vm, w)
        }
    }
}

@Composable
fun WordDetail(vm: AppVM, w: Word) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(w.en, style = en(38))
            Text("${w.ipa}   ${w.faPron}", color = C.ink3, fontSize = 15.sp)
        }
        SpeakBtn { vm.speak(w.en) }
    }
    Spacer(Modifier.height(16.dp))
    w.senses.forEach { s ->
        Panel(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(posFa[s.pos] ?: s.pos, C.acc)
                if (s.syn.isNotBlank()) Text("= ${s.syn}", style = en(15, FontWeight.Normal).copy(color = C.ink3), modifier = Modifier.padding(start = 8.dp))
            }
            Text(s.fa, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            s.examples.forEach { e ->
                Row(Modifier.fillMaxWidth().padding(top = 10.dp).clickable { vm.speak(e.en) }) {
                    Box(Modifier.width(3.dp).height(40.dp).background(C.line))
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(e.en, style = en(17, FontWeight.Normal))
                        Text(e.fa, color = C.ink3, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ───────────────────────── مرور (فلش‌کارت)
@Composable
fun ReviewScreen(vm: AppVM, sc: Screen.Review) {
    var i by remember(sc) { mutableIntStateOf(0) }
    var shown by remember(sc) { mutableStateOf(false) }
    var ok by remember(sc) { mutableIntStateOf(0) }
    val total = sc.ids.size
    Column(Modifier.fillMaxSize()) {
        TopBar(sc.title) { vm.back() }
        if (i >= total) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🎉", fontSize = 64.sp)
                Text("تموم شد!", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("${fa(ok)} از ${fa(total)} رو بلد بودی", color = C.ink2, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(24.dp))
                MainButton("برگشت", Modifier.fillMaxWidth()) { vm.back() }
            }
            return@Column
        }
        val w = Repo.words[sc.ids[i]]
        if (w == null) { i++; return@Column }
        val prog by animateFloatAsState(i.toFloat() / total, label = "p")
        LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = C.acc, trackColor = C.paper3)
        Text("${fa(i + 1)} / ${fa(total)}", color = C.ink3, fontSize = 14.sp, modifier = Modifier.padding(16.dp, 6.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            val box = vm.progress.cards[w.id]?.box
            Panel(Modifier.fillMaxWidth(), onClick = { shown = true }, pad = 24.dp) {
                if (box != null && !sc.practice) Chip("جعبه‌ی ${fa(box)}", C.boxes[(box - 1).coerceIn(0, 5)])
                Spacer(Modifier.height(30.dp))
                Text(w.en, style = en(40), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(w.ipa, color = C.ink3, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SpeakBtn { vm.speak(w.en) } }
                Spacer(Modifier.height(30.dp))
                if (!shown) Text("برای دیدن معنی بزن", color = C.ink3, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            if (shown) {
                Spacer(Modifier.height(12.dp))
                w.senses.forEach { s ->
                    Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Chip(posFa[s.pos] ?: s.pos, C.acc)
                            Text(s.fa, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                        }
                        s.examples.firstOrNull()?.let { e ->
                            Text(e.en, style = en(16, FontWeight.Normal), modifier = Modifier.padding(top = 8.dp))
                            Text(e.fa, color = C.ink3, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!shown) MainButton("نمایش معنی", Modifier.weight(1f)) { shown = true }
            else {
                MainButton("بلد نبودم", Modifier.weight(1f), color = C.bad) {
                    if (!sc.practice) vm.answer(w.id, false); shown = false; i++
                }
                MainButton("بلد بودم", Modifier.weight(1f), color = C.ok) {
                    if (!sc.practice) vm.answer(w.id, true); ok++; shown = false; i++
                }
            }
        }
    }
}

// ───────────────────────── جعبه‌ها
@Composable
fun BoxesScreen(vm: AppVM) {
    @Suppress("UNUSED_VARIABLE") val r = vm.rev
    val names = listOf("هر روز", "هر ۲ روز", "هر ۴ روز", "هر ۸ روز", "هر ۱۶ روز", "یادگرفته")
    Column(Modifier.fillMaxSize()) {
        TopBar("جعبه‌های لایتنر") { vm.back() }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..6).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { b ->
                        val n = vm.progress.inBox(b).size
                        val col = C.boxes[b - 1]
                        Box(Modifier.weight(1f).aspectRatio(1.1f).clip(RoundedCornerShape(20.dp)).background(col.copy(alpha = .14f))
                            .border(1.dp, col.copy(alpha = .4f), RoundedCornerShape(20.dp)).clickable { vm.go(Screen.BoxList(b)) }.padding(16.dp)) {
                            Column {
                                Text(if (b == 6) "✓" else "جعبه‌ی ${fa(b)}", color = col, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(names[b - 1], color = C.ink3, fontSize = 14.sp)
                            }
                            Text(fa(n), fontSize = 40.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomStart))
                        }
                    }
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Text("جواب درست: ${fa(vm.progress.ok)}   ·   جواب غلط: ${fa(vm.progress.bad)}", color = C.ink2)
            }
        }
    }
}

@Composable
fun BoxListScreen(vm: AppVM, box: Int) {
    @Suppress("UNUSED_VARIABLE") val r = vm.rev
    val ws = vm.progress.inBox(box).mapNotNull { Repo.words[it] }
    Column(Modifier.fillMaxSize()) {
        TopBar(if (box == 6) "یادگرفته‌ها" else "جعبه‌ی ${fa(box)}") { vm.back() }
        if (ws.isNotEmpty()) MainButton("تمرین همین جعبه", Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            vm.go(Screen.Review(ws.map { it.id }.shuffled(), true, "تمرین جعبه"))
        }
        if (ws.isEmpty()) Text("این جعبه خالیه.", color = C.ink3, modifier = Modifier.padding(24.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ws, key = { it.id }) { WordRow(vm, it) }
        }
    }
}

// ───────────────────────── آزمون‌ها
@Composable
fun TestsScreen(vm: AppVM) {
    @Suppress("UNUSED_VARIABLE") val r = vm.rev
    Column(Modifier.fillMaxSize()) {
        TopBar("آزمون‌ها") { vm.back() }
        LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Repo.tests, key = { it.n }) { t ->
                val best = vm.progress.testBest[t.n]
                val lv = Repo.collections.firstOrNull { it.key == t.level }
                Panel(Modifier.fillMaxWidth(), onClick = { vm.go(Screen.TestRun(t.n)) }, pad = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("آزمون ${fa(t.n)}", fontWeight = FontWeight.Bold)
                            Text("${lv?.title ?: ""} · درس‌های ${t.lessons}", color = C.ink3, fontSize = 14.sp)
                        }
                        if (best != null) Chip("${fa(best)}/${fa(t.questions.size)}", if (best * 2 >= t.questions.size) C.ok else C.bad)
                        else Chip("${fa(t.questions.size)} سؤال", C.ink3)
                    }
                }
            }
        }
    }
}

@Composable
fun TestRunScreen(vm: AppVM, n: Int) {
    val t = Repo.tests.firstOrNull { it.n == n } ?: return
    var i by remember { mutableIntStateOf(0) }
    var picked by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TopBar("آزمون ${fa(t.n)}") { vm.back() }
        if (i >= t.questions.size) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(fa(score), fontSize = 72.sp, fontWeight = FontWeight.Black, color = if (score * 2 >= t.questions.size) C.ok else C.bad)
                Text("از ${fa(t.questions.size)} سؤال", color = C.ink2, fontSize = 17.sp)
                Spacer(Modifier.height(24.dp))
                MainButton("دوباره", Modifier.fillMaxWidth()) { i = 0; score = 0; picked = -1 }
                Spacer(Modifier.height(10.dp))
                GhostButton("برگشت", Modifier.fillMaxWidth()) { vm.back() }
            }
            return@Column
        }
        val q = t.questions[i]
        LinearProgressIndicator(progress = { i.toFloat() / t.questions.size }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = C.acc, trackColor = C.paper3)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("سؤال ${fa(i + 1)}", color = C.ink3, fontSize = 14.sp)
            Text(q.text, style = en(21, FontWeight.Normal), modifier = Modifier.padding(vertical = 14.dp))
            q.options.forEachIndexed { k, o ->
                val bg = when {
                    picked < 0 -> C.card
                    k == q.answer -> C.okSoft
                    k == picked -> C.badSoft
                    else -> C.card
                }
                val bd = when {
                    picked < 0 -> C.line
                    k == q.answer -> C.ok
                    k == picked -> C.bad
                    else -> C.line
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(14.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(14.dp))
                    .clickable(enabled = picked < 0) { picked = k; if (k == q.answer) score++ }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fa(k + 1), color = C.ink3, modifier = Modifier.width(24.dp))
                    Text(o, style = en(19))
                }
            }
        }
        if (picked >= 0) MainButton(if (i + 1 < t.questions.size) "سؤال بعد" else "دیدن نتیجه", Modifier.fillMaxWidth().padding(16.dp)) {
            if (i + 1 >= t.questions.size) vm.saveTest(t.n, score)
            i++; picked = -1
        }
    }
}

// ───────────────────────── جست‌وجو
@Composable
fun SearchScreen(vm: AppVM) {
    var q by remember { mutableStateOf("") }
    val res = remember(q) { Repo.search(q) }
    Column(Modifier.fillMaxSize()) {
        TopBar("جست‌وجو") { vm.back() }
        OutlinedTextField(q, { q = it }, placeholder = { Text("لغت انگلیسی یا معنی فارسی…") }, singleLine = true,
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.acc, unfocusedBorderColor = C.line, focusedTextColor = C.ink, unfocusedTextColor = C.ink, cursorColor = C.acc))
        if (q.isNotBlank() && res.isEmpty()) Text("چیزی پیدا نشد.", color = C.ink3, modifier = Modifier.padding(24.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(res, key = { it.id }) { WordRow(vm, it) }
        }
    }
}

// ───────────────────────── پیشوند و پسوند
@Composable
fun MorphScreen(vm: AppVM) {
    Column(Modifier.fillMaxSize()) {
        TopBar("پیشوندها و پسوندها") { vm.back() }
        LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Repo.morph) { m ->
                Panel(Modifier.fillMaxWidth()) {
                    Text(m.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    m.rows.forEach { row ->
                        Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(10.dp)).background(C.paper2).padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(row.getOrElse(0) { "" }, style = en(19).copy(color = C.acc))
                                Text("  ${row.getOrElse(1) { "" }}", color = C.ink2, fontSize = 15.sp)
                            }
                            if (row.size >= 6) Text("${row[2]} (${row[3]})  ←  ${row[4]} (${row[5]})", color = C.ink3, fontSize = 15.sp)
                            else if (row.size > 2) Text(row.drop(2).joinToString(" · "), color = C.ink3, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── حساب کاربری
@Composable
fun ProfileScreen(vm: AppVM) {
    var askDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopBar("حساب کاربری") { vm.back() }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.fillMaxWidth()) {
                Text(vm.userName, style = en(26))
                Text("پیشرفتت روی سرور ذخیره می‌شه و با ورود از هر گوشی‌ای برمی‌گرده.", color = C.ink3, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            }
            GhostButton("خروج از حساب", Modifier.fillMaxWidth()) { vm.logout() }
            GhostButton(if (vm.busy) "در حال حذف…" else "حذف همیشگی حساب", Modifier.fillMaxWidth(), color = C.bad) { askDelete = true }
            Panel(Modifier.fillMaxWidth()) {
                Text("حریم خصوصی", fontWeight = FontWeight.Bold)
                Text("LAITNO فقط نام کاربری، رمز (رمزنگاری‌شده) و پیشرفت یادگیری‌ات رو نگه می‌داره و با هیچ‌کس به اشتراک نمی‌ذاره. با حذف حساب، همه‌چیز برای همیشه پاک می‌شه.",
                    color = C.ink3, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
    if (askDelete) AlertDialog(
        onDismissRequest = { askDelete = false },
        title = { Text("حذف حساب؟") },
        text = { Text("حسابت و همه‌ی پیشرفتت برای همیشه پاک می‌شه.") },
        confirmButton = { TextButton({ askDelete = false; vm.deleteAccount() }) { Text("حذف کن", color = C.bad) } },
        dismissButton = { TextButton({ askDelete = false }) { Text("نه") } },
        containerColor = C.paper2,
    )
}

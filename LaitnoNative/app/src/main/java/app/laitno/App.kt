package app.laitno

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LaitnoApp(vm: AppVM) {
    LaitnoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val ctx = LocalContext.current
            LaunchedEffect(vm.toast) {
                vm.toast?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.toast = null }
            }
            Box(Modifier.fillMaxSize().background(C.paper).safeDrawingPadding()) {
                val s = vm.screen
                if (!vm.loaded || s == null) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LAITNO", fontSize = 34.sp, fontWeight = FontWeight.Black)
                        CircularProgressIndicator(color = C.acc)
                    }
                } else {
                    BackHandler(enabled = vm.stack.size > 1) { vm.back() }
                    AnimatedContent(targetState = s, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { sc ->
                        when (sc) {
                            Screen.Login -> LoginScreen(vm)
                            Screen.Home -> HomeScreen(vm)
                            is Screen.Coll -> CollectionScreen(vm, sc.key)
                            is Screen.LessonS -> LessonScreen(vm, sc.n)
                            is Screen.WordS -> WordScreen(vm, sc.id)
                            is Screen.Review -> ReviewScreen(vm, sc)
                            Screen.Boxes -> BoxesScreen(vm)
                            is Screen.BoxList -> BoxListScreen(vm, sc.box)
                            Screen.Tests -> TestsScreen(vm)
                            is Screen.TestRun -> TestRunScreen(vm, sc.n)
                            Screen.Search -> SearchScreen(vm)
                            Screen.Morph -> MorphScreen(vm)
                            Screen.Profile -> ProfileScreen(vm)
                        }
                    }
                }
            }
        }
    }
}

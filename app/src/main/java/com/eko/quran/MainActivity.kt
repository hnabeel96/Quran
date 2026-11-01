package com.eko.quran

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------- palette ----------
private val Bg = Color(0xFF0F1A17)
private val Card = Color(0xFF172521)
private val Line = Color(0xFF26372F)
private val Ink = Color(0xFFEDE4CC)
private val Gold = Color(0xFFC9A55C)
private val Muted = Color(0xFF8B9A91)
private val Gloss = Color(0xFF9FC2B0)

private val Uthmani = FontFamily(Font(R.font.amiri_quran))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Bg, surface = Card, primary = Gold, onPrimary = Bg,
                    onBackground = Ink, onSurface = Ink, secondary = Gloss,
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = Bg) { App() }
            }
        }
    }
}

private class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("murajaah", Context.MODE_PRIVATE)
    var surah by IntPref(p, "surah", 0)
    var ayah by IntPref(p, "ayah", 0)
    var mode by IntPref(p, "mode", 0)
    var glosses by BoolPref(p, "glosses", true)
    var translation by BoolPref(p, "translation", true)
    var showPrev by BoolPref(p, "context", true)
    var size by IntPref(p, "size", 30)
}

private class IntPref(val p: android.content.SharedPreferences, val key: String, val def: Int) {
    operator fun getValue(t: Any?, prop: kotlin.reflect.KProperty<*>) = p.getInt(key, def)
    operator fun setValue(t: Any?, prop: kotlin.reflect.KProperty<*>, v: Int) = p.edit().putInt(key, v).apply()
}

private class BoolPref(val p: android.content.SharedPreferences, val key: String, val def: Boolean) {
    operator fun getValue(t: Any?, prop: kotlin.reflect.KProperty<*>) = p.getBoolean(key, def)
    operator fun setValue(t: Any?, prop: kotlin.reflect.KProperty<*>, v: Boolean) = p.edit().putBoolean(key, v).apply()
}

@Composable
private fun App() {
    val context = LocalContext.current
    var quran by remember { mutableStateOf<Quran?>(null) }
    LaunchedEffect(Unit) { quran = withContext(Dispatchers.IO) { loadQuran(context) } }

    val q = quran
    if (q == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold)
        }
    } else {
        Reader(q, remember { Prefs(context) })
    }
}

@Composable
private fun Reader(q: Quran, prefs: Prefs) {
    var surahIdx by rememberSaveable { mutableIntStateOf(prefs.surah.coerceIn(0, 113)) }
    var ayahIdx by rememberSaveable {
        mutableIntStateOf(prefs.ayah.coerceIn(0, q.surahs[prefs.surah.coerceIn(0, 113)].ayahs.lastIndex))
    }
    var mode by rememberSaveable { mutableStateOf(Mode.entries[prefs.mode.coerceIn(0, 2)]) }
    var revealed by rememberSaveable { mutableIntStateOf(0) }
    var showGloss by remember { mutableStateOf(prefs.glosses) }
    var showTranslation by remember { mutableStateOf(prefs.translation) }
    var showContext by remember { mutableStateOf(prefs.showPrev) }
    var size by remember { mutableIntStateOf(prefs.size) }
    var navigator by rememberSaveable { mutableStateOf(false) }
    var peek by remember { mutableStateOf(false) }

    val surah = q.surahs[surahIdx]
    val ayah = surah.ayahs[ayahIdx]
    val total = ayah.units(mode)
    val done = revealed >= total

    LaunchedEffect(surahIdx, ayahIdx) { prefs.surah = surahIdx; prefs.ayah = ayahIdx }

    fun goTo(s: Int, a: Int, fullyShown: Boolean = false) {
        surahIdx = s; ayahIdx = a
        revealed = if (fullyShown) q.surahs[s].ayahs[a].units(mode) else 0
    }
    fun nextAyah() {
        if (ayahIdx < surah.ayahs.lastIndex) goTo(surahIdx, ayahIdx + 1)
        else if (surahIdx < 113) goTo(surahIdx + 1, 0)
    }
    fun prevAyah(fullyShown: Boolean) {
        if (ayahIdx > 0) goTo(surahIdx, ayahIdx - 1, fullyShown)
        else if (surahIdx > 0) goTo(surahIdx - 1, q.surahs[surahIdx - 1].ayahs.lastIndex, fullyShown)
    }
    fun next() { if (done) nextAyah() else revealed++ }
    fun back() { if (revealed > 0) revealed-- else prevAyah(fullyShown = true) }
    fun finishWord() {
        if (mode != Mode.LETTER || done) { next(); return }
        val starts = ayah.letterStart
        revealed = starts.first { it > revealed }
    }

    if (navigator) {
        BackHandler { navigator = false }
        Navigator(q, surahIdx, onClose = { navigator = false }) { s, a ->
            goTo(s, a); navigator = false
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // ---- header ----
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { prevAyah(fullyShown = false) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous ayah", tint = Muted)
            }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { navigator = true }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${surah.number}. ${surah.name}  ·  ${surah.arabicName}", color = Ink,
                    fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Ayah ${ayah.number} of ${surah.ayahs.size}  ·  tap to change", color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = { nextAyah() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next ayah", tint = Muted)
            }
            Box {
                var menu by remember { mutableStateOf(false) }
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Settings", tint = Muted) }
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    ToggleItem("Word meanings", showGloss) { showGloss = it; prefs.glosses = it }
                    ToggleItem("Translation", showTranslation) { showTranslation = it; prefs.translation = it }
                    ToggleItem("Previous ayah", showContext) { showContext = it; prefs.showPrev = it }
                    HorizontalDivider()
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Arabic size", Modifier.weight(1f), fontSize = 14.sp)
                        TextButton(onClick = { size = (size - 2).coerceAtLeast(20); prefs.size = size }) { Text("A−") }
                        TextButton(onClick = { size = (size + 2).coerceAtMost(48); prefs.size = size }) { Text("A+") }
                    }
                }
            }
        }

        // ---- mode switch ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(50)).background(Card).padding(4.dp)
        ) {
            Mode.entries.forEach { m ->
                val sel = m == mode
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(if (sel) Gold else Color.Transparent)
                        .clickable {
                            mode = m; prefs.mode = m.ordinal; revealed = 0
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(m.label, color = if (sel) Bg else Ink, fontSize = 14.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        // ---- progress ----
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else revealed.coerceAtMost(total) / total.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
            color = Gold, trackColor = Line, drawStopIndicator = {},
        )

        // ---- reading area: tap = next, long-press = peek ----
        val scroll = rememberScrollState()
        LaunchedEffect(revealed, surahIdx, ayahIdx, peek) { scroll.animateScrollTo(scroll.maxValue) }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(mode, done, surahIdx, ayahIdx) {
                    detectTapGestures(
                        onPress = { tryAwaitRelease(); peek = false },
                        onLongPress = { peek = true },
                        onTap = { next() },
                    )
                }
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            if (showContext) {
                val prev = when {
                    ayahIdx > 0 -> surah.ayahs[ayahIdx - 1]
                    else -> null
                }
                if (prev != null) {
                    ArabicText("${prev.arabic}  ﴿${arabicNumber(prev.number)}﴾", size * 0.62f, Muted.copy(alpha = 0.75f))
                    Spacer(Modifier.height(14.dp))
                }
            }
            if (ayahIdx == 0 && surah.number != 1 && surah.number != 9) {
                ArabicText(q.basmala, size * 0.72f, Gold.copy(alpha = 0.85f), center = true)
                Spacer(Modifier.height(14.dp))
            }

            AyahView(ayah, mode, if (peek) total else revealed, size, showGloss, peek)

            if ((done || peek) && showTranslation) {
                Spacer(Modifier.height(18.dp))
                Text(
                    ayah.english, color = Ink.copy(alpha = 0.88f), fontSize = 16.sp, lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card)
                        .padding(14.dp)
                )
            }
            if (!done && !peek) {
                Spacer(Modifier.height(20.dp))
                Text(
                    when {
                        revealed == 0 && mode == Mode.AYAH -> "Recite ayah ${ayah.number}, then tap to check"
                        revealed == 0 -> "Tap to reveal · hold to peek"
                        else -> ""
                    },
                    color = Muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
            }
        }

        // ---- controls ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton("Back", Modifier.weight(1f), primary = false) { back() }
            if (mode == Mode.LETTER) ControlButton("Word", Modifier.weight(1f), primary = false) { finishWord() }
            if (mode != Mode.AYAH) ControlButton("Show all", Modifier.weight(1f), primary = false) {
                if (done) nextAyah() else revealed = total
            }
            ControlButton(if (done) "Next ayah" else "Next", Modifier.weight(1.3f), primary = true) { next() }
        }
    }
}

@Composable
private fun ToggleItem(label: String, on: Boolean, set: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = { Switch(checked = on, onCheckedChange = null) },
        onClick = { set(!on) },
    )
}

@Composable
private fun ControlButton(label: String, modifier: Modifier, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) Gold else Card)
            .border(1.dp, if (primary) Gold else Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (primary) Bg else Ink, fontSize = 15.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
private fun ArabicText(text: String, size: Float, color: Color, center: Boolean = false) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text, color = color, fontFamily = Uthmani, fontSize = size.sp, lineHeight = (size * 1.9f).sp,
            textAlign = if (center) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The current ayah, revealed up to [revealed] units of [mode]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AyahView(ayah: Ayah, mode: Mode, revealed: Int, size: Int, showGloss: Boolean, peek: Boolean) {
    val arabicStyle = TextStyle(fontFamily = Uthmani, fontSize = size.sp, lineHeight = (size * 1.75f).sp, color = Ink)
    val total = ayah.units(mode)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (i in ayah.words.indices) {
                // how much of word i is visible
                val text: String? = when (mode) {
                    Mode.AYAH -> if (revealed >= 1) ayah.words[i] else null
                    Mode.WORD -> if (i < revealed) ayah.words[i] else null
                    Mode.LETTER -> {
                        val start = ayah.letterStart[i]
                        val n = ayah.letters[i].size
                        val shown = (revealed - start).coerceIn(0, n)
                        when {
                            shown == 0 -> null
                            shown == n -> ayah.words[i]
                            // zero-width joiner keeps the last letter in its joined (mid-word) shape
                            else -> ayah.letters[i].take(shown).joinToString("") + "‍"
                        }
                    }
                }
                if (text == null) break
                val complete = text == ayah.words[i]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text, style = arabicStyle.copy(color = if (peek) Ink.copy(alpha = 0.55f) else Ink))
                    if (showGloss && complete) {
                        val g = ayah.gloss(i)
                        if (g != null) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    g, color = Gloss, fontSize = 12.sp, lineHeight = 15.sp, textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (revealed >= total) {
                Text("﴿${arabicNumber(ayah.number)}﴾", style = arabicStyle.copy(color = Gold))
            }
        }
    }
}

// ---------------- navigator ----------------

@Composable
private fun Navigator(q: Quran, current: Int, onClose: () -> Unit, onPick: (Int, Int) -> Unit) {
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    val p = picked
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (p != null) picked = null else onClose() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink)
            }
            Text(
                if (p == null) "Choose surah" else "${q.surahs[p].name} · choose ayah",
                color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium
            )
        }
        if (p == null) {
            BackHandler { onClose() }
            SurahList(q, current) { picked = it }
        } else {
            BackHandler { picked = null }
            val s = q.surahs[p]
            LazyVerticalGrid(
                GridCells.Adaptive(56.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(s.ayahs) { a ->
                    Box(
                        Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Card)
                            .border(1.dp, Line, RoundedCornerShape(12.dp))
                            .clickable { onPick(p, a.number - 1) },
                        contentAlignment = Alignment.Center
                    ) { Text("${a.number}", color = Ink, fontSize = 15.sp) }
                }
            }
        }
    }
}

@Composable
private fun SurahList(q: Quran, current: Int, onPick: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query) {
        val t = query.trim().lowercase().replace("-", "").replace("'", "")
        q.surahs.filter {
            t.isEmpty() || it.number.toString() == t ||
                it.name.lowercase().replace("-", "").replace("'", "").contains(t) ||
                it.english.lowercase().contains(t) || it.arabicName.contains(query.trim())
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp)).background(Card).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Muted)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search by name or number", color = Muted, fontSize = 15.sp)
            BasicTextField(
                query, { query = it }, singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 15.sp), cursorBrush = SolidColor(Gold),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 6.dp)) {
        items(filtered, key = { it.number }) { s ->
            val here = s.number - 1 == current
            Row(
                Modifier.fillMaxWidth().clickable { onPick(s.number - 1) }
                    .background(if (here) Card else Color.Transparent)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, if (here) Gold else Line, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("${s.number}", color = if (here) Gold else Ink, fontSize = 13.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.name, color = Ink, fontSize = 16.sp)
                    Text("${s.english} · ${s.ayahs.size} ayat · ${s.type.replaceFirstChar { it.uppercase() }}",
                        color = Muted, fontSize = 12.sp)
                }
                Text(s.arabicName, color = Gold, fontFamily = Uthmani, fontSize = 20.sp)
            }
        }
    }
}

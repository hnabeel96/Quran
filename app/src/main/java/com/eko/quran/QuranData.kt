package com.eko.quran

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Ayah(
    val surah: Int,
    val number: Int,
    val arabic: String,
    val english: String,
    val glosses: List<String>?,
) {
    val words: List<String> = arabic.split(" ")

    /** Each word split into letter clusters: a base letter plus the marks sitting on it. */
    val letters: List<List<String>> by lazy { words.map(::clusters) }

    /** letterStart[i] = number of letter clusters before word i; last entry = total. */
    val letterStart: IntArray by lazy {
        val a = IntArray(words.size + 1)
        for (i in words.indices) a[i + 1] = a[i] + letters[i].size
        a
    }

    fun units(mode: Mode): Int = when (mode) {
        Mode.AYAH -> 1
        Mode.WORD -> words.size
        Mode.LETTER -> letterStart[words.size]
    }

    fun gloss(i: Int): String? = glosses?.getOrNull(i)?.takeIf { it.isNotBlank() }
}

class Surah(
    val number: Int,
    val arabicName: String,
    val name: String,
    val english: String,
    val type: String,
    val ayahs: List<Ayah>,
)

class Quran(val basmala: String, val surahs: List<Surah>)

enum class Mode(val label: String) { AYAH("Ayah"), WORD("Word"), LETTER("Letter") }

private fun isMark(c: Char): Boolean =
    Character.getType(c) == Character.NON_SPACING_MARK.toInt() ||
        c == 'ۥ' || c == 'ۦ' // small waw / small ya: pronunciation marks

fun clusters(word: String): List<String> {
    val out = ArrayList<StringBuilder>()
    for (c in word) {
        if (isMark(c) && out.isNotEmpty()) out.last().append(c) else out.add(StringBuilder().append(c))
    }
    return out.map { it.toString() }
}

fun loadQuran(context: Context): Quran {
    fun read(name: String): String? = try {
        context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (e: Exception) {
        null
    }

    val root = JSONObject(read("quran.json")!!)
    val wbw = read("wbw.json")?.let { JSONObject(it) }

    val surahs = ArrayList<Surah>(114)
    val arr = root.getJSONArray("surahs")
    for (i in 0 until arr.length()) {
        val s = arr.getJSONObject(i)
        val n = s.getInt("n")
        val a = s.getJSONArray("a")
        val ayahs = ArrayList<Ayah>(a.length())
        for (j in 0 until a.length()) {
            val pair = a.getJSONArray(j)
            val g: JSONArray? = wbw?.optJSONArray("$n:${j + 1}")
            val glosses = g?.let { ga -> List(ga.length()) { ga.optString(it) } }
            ayahs.add(Ayah(n, j + 1, pair.getString(0), pair.getString(1), glosses))
        }
        surahs.add(Surah(n, s.getString("ar"), s.getString("tr"), s.getString("en"), s.getString("t"), ayahs))
    }
    return Quran(root.getString("basmala"), surahs)
}

private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
fun arabicNumber(n: Int): String = n.toString().map { arabicDigits[it - '0'] }.joinToString("")

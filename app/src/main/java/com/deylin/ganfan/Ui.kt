package com.deylin.ganfan

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** 统一视觉：青绿主题（沿用干饭盲盒网页配色） */
object Ui {
    val BG = 0xFFEEFBFB.toInt()
    val CARD = 0xFFFFFFFF.toInt()
    val PRIMARY = 0xFF15B8A6.toInt()
    val PRIMARY_D = 0xFF0E9488.toInt()
    val PRIMARY_LIGHT = 0xFFE0F2F1.toInt()
    val TEXT = 0xFF1F3B39.toInt()
    val SUB = 0xFF6F9A98.toInt()
    val LINE = 0xFFDDEFED.toInt()
    val GOLD = 0xFFD99A0B.toInt()
    val RED = 0xFFE8543F.toInt()

    val MP = LinearLayout.LayoutParams.MATCH_PARENT
    val WC = LinearLayout.LayoutParams.WRAP_CONTENT

    val RARITY = mapOf(
        "普通" to 0xFF8AA6A3.toInt(),
        "稀有" to 0xFF2F9EE8.toInt(),
        "史诗" to 0xFF9B6BE0.toInt(),
        "传说" to 0xFFE08A1E.toInt(),
        "神话" to 0xFFE0455B.toInt()
    )

    fun dp(c: Context, v: Float): Int = (v * c.resources.displayMetrics.density + 0.5f).toInt()

    fun round(c: Context, color: Int, r: Float, stroke: Float = 0f, strokeColor: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.RECTANGLE
        g.setColor(color)
        g.cornerRadius = dp(c, r).toFloat()
        if (stroke > 0f) g.setStroke(dp(c, stroke), strokeColor)
        return g
    }

    fun tv(c: Context, t: CharSequence, sp: Float, color: Int, bold: Boolean = false): TextView {
        val v = TextView(c)
        v.text = t
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        v.setTextColor(color)
        v.includeFontPadding = false
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD)
        return v
    }

    fun row(c: Context, gravity: Int = Gravity.CENTER_VERTICAL): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.HORIZONTAL
        l.gravity = gravity
        return l
    }

    fun col(c: Context): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        return l
    }

    fun lp(w: Int, h: Int, weight: Float = 0f): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        p.weight = weight
        return p
    }

    fun margin(p: ViewGroup.MarginLayoutParams, l: Int, t: Int, r: Int, b: Int): ViewGroup.MarginLayoutParams {
        p.setMargins(l, t, r, b)
        return p
    }

    fun pad(v: View, l: Int, t: Int, r: Int, b: Int): View {
        v.setPadding(dp(v.context, l.toFloat()), dp(v.context, t.toFloat()), dp(v.context, r.toFloat()), dp(v.context, b.toFloat()))
        return v
    }

    fun divider(c: Context): View {
        val v = View(c)
        v.setBackgroundColor(LINE)
        return v
    }

    fun card(c: Context, padH: Float = 14f, padV: Float = 14f): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.setBackground(round(c, CARD, 16f))
        l.setPadding(dp(c, padH), dp(c, padV), dp(c, padH), dp(c, padV))
        return l
    }

    fun button(c: Context, text: String, bg: Int, fg: Int, sp: Float = 15f): TextView {
        val b = tv(c, text, sp, fg, true)
        b.gravity = Gravity.CENTER
        b.setBackground(round(c, bg, 22f))
        b.setPadding(0, dp(c, 11f), 0, dp(c, 11f))
        return b
    }

    fun iconCircle(c: Context, emoji: String): TextView {
        val t = tv(c, emoji, 17f, TEXT)
        t.gravity = Gravity.CENTER
        t.setBackground(round(c, PRIMARY_LIGHT, 22f))
        val s = dp(c, 40f)
        t.layoutParams = LinearLayout.LayoutParams(s, s)
        return t
    }

    /** 稀有度小标签 */
    fun rarityChip(c: Context, rarity: String): TextView {
        val color = RARITY[rarity] ?: SUB
        val t = tv(c, rarity, 10.5f, color, true)
        t.setBackground(round(c, PRIMARY_LIGHT, 8f))
        t.setPadding(dp(c, 7f), dp(c, 2f), dp(c, 7f), dp(c, 2f))
        return t
    }
}

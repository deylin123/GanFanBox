package com.deylin.ganfan

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 本次运行内已抽中状态（切换 tab 不丢） */
object HomeState {
    val drawn = HashMap<String, Dish>()
}

/** 主页：早餐 / 中餐 / 晚餐 抽菜 + 今日靓汤单独抽 */
class PageHome(private val act: MainActivity) {

    private val c: Context = act
    private lateinit var box: LinearLayout

    private val meals = listOf("早餐", "中餐", "晚餐", "汤")
    private val weight = mapOf("普通" to 100, "稀有" to 45, "史诗" to 18, "传说" to 6, "神话" to 2)

    fun build(): View {
        val sv = ScrollView(c)
        box = Ui.col(c)
        box.setBackgroundColor(Ui.BG)
        Ui.pad(box, 14, 12, 14, 16)
        sv.addView(box)
        drawAll()
        return sv
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

    private fun titleOf(m: String) = if (m == "汤") "今日靓汤" else m

    private fun drawAll() {
        box.removeAllViews()
        for (m in meals) box.addView(cardFor(m), cardLp())
    }

    private fun cardLp(): LinearLayout.LayoutParams {
        val p = Ui.lp(Ui.MP, Ui.WC)
        p.bottomMargin = Ui.dp(c, 12f)
        return p
    }

    private fun cardFor(meal: String): View {
        val isSoup = meal == "汤"
        val card = Ui.card(c, 14f, 14f)
        if (isSoup) card.setBackground(Ui.round(c, Ui.CARD, 16f, 1.5f, Ui.PRIMARY))
        val d = HomeState.drawn[meal]

        val head = Ui.row(c)
        head.addView(Ui.tv(c, titleOf(meal), 15f, Ui.TEXT, true))
        head.addView(View(c), Ui.lp(0, 1, 1f))
        val badge = Ui.tv(c, if (d != null) "已抽中" else "未抽", 10.5f,
            if (d != null) Ui.PRIMARY_D else Ui.SUB, true)
        badge.setBackground(Ui.round(c, if (d != null) Ui.PRIMARY_LIGHT else 0xFFF3F6F5.toInt(), 9f))
        badge.setPadding(Ui.dp(c, 8f), Ui.dp(c, 3f), Ui.dp(c, 8f), Ui.dp(c, 3f))
        head.addView(badge)
        card.addView(head, Ui.lp(Ui.MP, Ui.WC))

        val body = Ui.col(c)
        Ui.pad(body, 0, 12, 0, 10)
        body.gravity = Gravity.CENTER_HORIZONTAL
        if (d == null) {
            body.addView(Ui.tv(c, if (isSoup) "🍲" else "🎲", 40f, Ui.SUB))
            val tip = Ui.tv(c, if (isSoup) "想喝点什么汤？" else "还没抽，手气如何？", 12f, Ui.SUB)
            Ui.pad(tip, 0, 6, 0, 0)
            body.addView(tip)
        } else {
            body.addView(Ui.tv(c, if (d.type == "汤") "🍲" else "🍽️", 36f, Ui.TEXT))
            val nm = Ui.tv(c, d.name, 19f, Ui.TEXT, true)
            Ui.pad(nm, 0, 6, 0, 0)
            body.addView(nm)
            val rar = Ui.rarityChip(c, d.rarity)
            Ui.pad(rar, 0, 7, 0, 0)
            body.addView(rar)
            val meta = Ui.tv(c, "${d.kcal} kcal · ${d.minutes} 分钟 · ${d.taste}", 11.5f, Ui.SUB)
            Ui.pad(meta, 0, 7, 0, 0)
            body.addView(meta)
            val link = Ui.tv(c, "📖 看做法", 12f, Ui.PRIMARY_D, true)
            Ui.pad(link, 0, 9, 0, 0)
            link.setOnClickListener { openSteps(d) }
            body.addView(link)
        }
        card.addView(body, Ui.lp(Ui.MP, Ui.WC))

        val acts = Ui.row(c)
        if (d == null) {
            val b = Ui.button(c, if (isSoup) "抽一碗" else "抽一张", Ui.PRIMARY, 0xFFFFFFFF.toInt(), 14f)
            b.setOnClickListener { draw(meal) }
            acts.addView(b, Ui.lp(Ui.MP, Ui.WC))
        } else {
            val cook = Ui.button(c, "就做它", Ui.PRIMARY, 0xFFFFFFFF.toInt(), 14f)
            cook.setOnClickListener { cookIt(meal) }
            acts.addView(cook, Ui.lp(0, Ui.WC, 1f))
            val again = Ui.button(c, if (isSoup) "换一碗" else "换一道", Ui.PRIMARY_LIGHT, Ui.PRIMARY_D, 14f)
            val lp2 = Ui.lp(0, Ui.WC, 1f)
            lp2.leftMargin = Ui.dp(c, 8f)
            again.setOnClickListener { draw(meal) }
            acts.addView(again, lp2)
        }
        card.addView(acts, Ui.lp(Ui.MP, Ui.WC))
        return card
    }

    private fun draw(meal: String) {
        val pool = if (meal == "汤") act.db().soups() else act.db().dishesForMeal(meal)
        if (pool.isEmpty()) {
            Toast.makeText(act, if (meal == "汤") "汤池还没汤，去菜品池加一道" else "这个餐次还没菜", Toast.LENGTH_SHORT).show()
            return
        }
        var total = 0
        for (d in pool) total += (weight[d.rarity] ?: 10)
        val roll = Math.random() * total
        var pick = pool[0]
        var acc = 0
        for (d in pool) {
            acc += (weight[d.rarity] ?: 10)
            if (roll <= acc) {
                pick = d
                break
            }
        }
        HomeState.drawn[meal] = pick
        drawAll()
    }

    private fun cookIt(meal: String) {
        val d = HomeState.drawn[meal] ?: return
        act.db().addRecord(Rec(today(), meal, d.name, d.taste, d.kcal, d.rarity, "已做"))
        HomeState.drawn.remove(meal)
        Toast.makeText(act, "定了！开火做饭 🍳", Toast.LENGTH_SHORT).show()
        drawAll()
    }

    private fun openSteps(d: Dish) {
        val sv = ScrollView(c)
        val b = Ui.col(c)
        Ui.pad(b, 18, 8, 18, 8)
        val lines = d.steps.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            b.addView(Ui.tv(c, "这道还没写做法，可以去菜品池加上。", 13f, Ui.SUB))
        } else {
            for ((i, t) in lines.withIndex()) {
                val row = Ui.row(c, Gravity.TOP)
                val no = Ui.tv(c, "${i + 1}", 11f, 0xFFFFFFFF.toInt(), true)
                no.gravity = Gravity.CENTER
                no.setBackground(Ui.round(c, Ui.PRIMARY, 11f))
                val sz = Ui.dp(c, 22f)
                no.layoutParams = LinearLayout.LayoutParams(sz, sz)
                row.addView(no)
                val txt = Ui.tv(c, t, 13f, Ui.TEXT)
                val lp = Ui.lp(0, Ui.WC, 1f)
                lp.leftMargin = Ui.dp(c, 10f)
                row.addView(txt, lp)
                val rl = Ui.lp(Ui.MP, Ui.WC)
                rl.bottomMargin = Ui.dp(c, 9f)
                b.addView(row, rl)
            }
        }
        sv.addView(b)
        AlertDialog.Builder(act)
            .setTitle("${d.name} · 做法")
            .setView(sv)
            .setPositiveButton("知道了", null)
            .show()
    }
}

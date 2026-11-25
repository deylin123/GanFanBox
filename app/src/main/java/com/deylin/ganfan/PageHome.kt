package com.deylin.ganfan

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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

    // ---- 抽卡仪式感：滚动状态 ----
    private val handler = Handler(Looper.getMainLooper())
    private var rolling: String? = null
    private var rollDish: Dish? = null

    // 滚动帧只改文字，不重建布局
    private val iconTv = HashMap<String, TextView>()
    private val nameTv = HashMap<String, TextView>()
    private val metaTv = HashMap<String, TextView>()

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
        iconTv.clear(); nameTv.clear(); metaTv.clear()
        for (m in meals) box.addView(cardFor(m), cardLp())
    }

    private fun cardLp(): LinearLayout.LayoutParams {
        val p = Ui.lp(Ui.MP, Ui.WC)
        p.bottomMargin = Ui.dp(c, 12f)
        return p
    }

    /** 只重建单张卡，避免整页闪 */
    private fun redrawCard(meal: String) {
        val idx = meals.indexOf(meal)
        if (idx < 0 || idx >= box.childCount) {
            drawAll()
            return
        }
        box.removeViewAt(idx)
        box.addView(cardFor(meal), idx, cardLp())
    }

    private fun cardFor(meal: String): View {
        val isSoup = meal == "汤"
        val isRolling = rolling == meal
        val card = Ui.card(c, 14f, 14f)
        if (isSoup) card.setBackground(Ui.round(c, Ui.CARD, 16f, 1.5f, Ui.PRIMARY))
        val d = if (isRolling) rollDish else HomeState.drawn[meal]

        val head = Ui.row(c)
        head.addView(Ui.tv(c, titleOf(meal), 15f, Ui.TEXT, true))
        head.addView(View(c), Ui.lp(0, 1, 1f))
        val badgeText = when {
            isRolling -> "抽卡中…"
            d != null -> "已抽中"
            else -> "未抽"
        }
        val badgeOn = isRolling || d != null
        val badge = Ui.tv(c, badgeText, 10.5f, if (badgeOn) Ui.PRIMARY_D else Ui.SUB, true)
        badge.setBackground(Ui.round(c, if (badgeOn) Ui.PRIMARY_LIGHT else 0xFFF3F6F5.toInt(), 9f))
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
            val ic = Ui.tv(c, if (d.type == "汤") "🍲" else "🍽️", 36f, Ui.TEXT)
            body.addView(ic)
            iconTv[meal] = ic
            val nm = Ui.tv(c, d.name, 19f, Ui.TEXT, true)
            Ui.pad(nm, 0, 6, 0, 0)
            body.addView(nm)
            nameTv[meal] = nm
            if (!isRolling) {
                val rar = Ui.rarityChip(c, d.rarity)
                Ui.pad(rar, 0, 7, 0, 0)
                body.addView(rar)
            }
            val meta = Ui.tv(c, "${d.kcal} kcal · ${d.minutes} 分钟 · ${d.taste}", 11.5f, Ui.SUB)
            Ui.pad(meta, 0, 7, 0, 0)
            body.addView(meta)
            metaTv[meal] = meta
            if (!isRolling) {
                val link = Ui.tv(c, "📖 看做法", 12f, Ui.PRIMARY_D, true)
                Ui.pad(link, 0, 9, 0, 0)
                link.setOnClickListener { openSteps(d) }
                body.addView(link)
            }
        }
        card.addView(body, Ui.lp(Ui.MP, Ui.WC))

        val acts = Ui.row(c)
        if (isRolling) {
            val b = Ui.button(c, "抽卡中…", Ui.PRIMARY_LIGHT, Ui.PRIMARY_D, 14f)
            b.isEnabled = false
            acts.addView(b, Ui.lp(Ui.MP, Ui.WC))
        } else if (d == null) {
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

    /** 按稀有度加权抽一道 */
    private fun pickWeighted(pool: List<Dish>): Dish {
        var total = 0
        for (d in pool) total += (weight[d.rarity] ?: 10)
        val roll = Math.random() * total
        var acc = 0
        for (d in pool) {
            acc += (weight[d.rarity] ?: 10)
            if (roll <= acc) return d
        }
        return pool[pool.size - 1]
    }

    private fun draw(meal: String) {
        if (rolling != null) return
        val pool = if (meal == "汤") act.db().soups() else act.db().dishesForMeal(meal)
        if (pool.isEmpty()) {
            Toast.makeText(act, if (meal == "汤") "汤池还没汤，去菜品池加一道" else "这个餐次还没菜", Toast.LENGTH_SHORT).show()
            return
        }
        val pick = pickWeighted(pool)
        rolling = meal
        rollDish = pool[(Math.random() * pool.size).toInt().coerceIn(0, pool.size - 1)]
        redrawCard(meal)
        startRoll(meal, pool, pick)
    }

    /** 老虎机式滚动：先快后慢，停住后回弹 + 震动 */
    private fun startRoll(meal: String, pool: List<Dish>, pick: Dish) {
        val frames = 20

        fun delayOf(i: Int): Long = when {
            i < 10 -> 55L
            i < 15 -> 90L
            i < 18 -> 140L
            else -> 200L
        }

        fun tick(i: Int) {
            if (i >= frames) {
                rolling = null
                rollDish = null
                HomeState.drawn[meal] = pick
                redrawCard(meal)
                bounce(meal)
                return
            }
            rollDish = pool[(Math.random() * pool.size).toInt().coerceIn(0, pool.size - 1)]
            updateRollViews(meal)
            handler.postDelayed({ tick(i + 1) }, delayOf(i))
        }

        tick(0)
    }

    private fun updateRollViews(meal: String) {
        val d = rollDish ?: return
        iconTv[meal]?.text = if (d.type == "汤") "🍲" else "🍽️"
        nameTv[meal]?.text = d.name
        metaTv[meal]?.text = "${d.kcal} kcal · ${d.minutes} 分钟 · ${d.taste}"
    }

    /** 结果落定：菜名缩放回弹 + 轻轻震一下 */
    private fun bounce(meal: String) {
        val v = nameTv[meal] ?: return
        v.scaleX = 0.86f
        v.scaleY = 0.86f
        v.animate().scaleX(1.10f).scaleY(1.10f).setDuration(110)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(DecelerateInterpolator()).start()
            }.start()
        buzz()
    }

    private fun buzz() {
        try {
            val vib = act.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(35)
            }
        } catch (e: Exception) {
            // 无振动马达或被系统限制，忽略
        }
    }

    /** 就做它：写入记录后直接给出做法 */
    private fun cookIt(meal: String) {
        val d = HomeState.drawn[meal] ?: return
        act.db().addRecord(Rec(today(), meal, d.name, d.taste, d.kcal, d.rarity, "已做"))
        HomeState.drawn.remove(meal)
        drawAll()
        openSteps(d)
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

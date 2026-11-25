package com.deylin.ganfan

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 记录页：按日查看 + 我的足迹（全部历史）+ 连续下厨统计 + 分享卡 */
class PageRecords(private val act: MainActivity) {

    private val c: Context = act
    private var selDate: String = today()
    private var mode = 0          // 0 = 按日查看，1 = 我的足迹
    private lateinit var root: LinearLayout

    fun build(): View {
        val sv = ScrollView(c)
        root = Ui.col(c)
        root.setBackgroundColor(Ui.BG)
        Ui.pad(root, 14, 12, 14, 16)
        sv.addView(root)
        render()
        return sv
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Calendar.getInstance().time)

    private fun shift(s: String, n: Int): String {
        val p = s.split("-")
        val cal = Calendar.getInstance()
        cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
        cal.add(Calendar.DAY_OF_MONTH, n)
        return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time)
    }

    private fun humanDate(d: String): String {
        val p = d.split("-")
        return if (p.size == 3) "${p[1].toInt()}月${p[2].toInt()}日" else d
    }

    private fun lpTop(top: Int): LinearLayout.LayoutParams {
        val p = Ui.lp(Ui.MP, Ui.WC)
        p.topMargin = Ui.dp(c, top.toFloat())
        return p
    }

    private fun render() {
        root.removeAllViews()

        // 模式切换
        val tabs = Ui.row(c)
        val t0 = chip("📅 按日查看", mode == 0) { mode = 0; render() }
        tabs.addView(t0, Ui.lp(0, Ui.WC, 1f))
        val t1 = chip("🧾 我的足迹", mode == 1) { mode = 1; render() }
        val tlp = Ui.lp(0, Ui.WC, 1f)
        tlp.leftMargin = Ui.dp(c, 8f)
        tabs.addView(t1, tlp)
        root.addView(tabs, Ui.lp(Ui.MP, Ui.WC))

        if (mode == 0) renderByDay() else renderFootprint()
    }

    private fun chip(text: String, on: Boolean, click: () -> Unit): View {
        val t = Ui.tv(c, text, 12.5f, if (on) 0xFFFFFFFF.toInt() else Ui.PRIMARY_D, true)
        t.gravity = Gravity.CENTER
        t.setBackground(Ui.round(c, if (on) Ui.PRIMARY else Ui.PRIMARY_LIGHT, 16f))
        t.setPadding(0, Ui.dp(c, 9f), 0, Ui.dp(c, 9f))
        t.setOnClickListener { click() }
        return t
    }

    // ---------------- 按日查看 ----------------
    private fun renderByDay() {
        // 日期切换
        val dateRow = Ui.row(c)
        val prev = Ui.tv(c, "‹", 22f, Ui.PRIMARY_D, true)
        prev.setPadding(Ui.dp(c, 12f), 0, Ui.dp(c, 12f), 0)
        prev.setOnClickListener { selDate = shift(selDate, -1); render() }
        val next = Ui.tv(c, "›", 22f, Ui.PRIMARY_D, true)
        next.setPadding(Ui.dp(c, 12f), 0, Ui.dp(c, 12f), 0)
        next.setOnClickListener { selDate = shift(selDate, 1); render() }
        dateRow.addView(prev)
        val lbl = Ui.tv(c, if (selDate == today()) "今天 · ${humanDate(selDate)}" else humanDate(selDate), 15f, Ui.TEXT, true)
        lbl.gravity = Gravity.CENTER
        dateRow.addView(lbl, Ui.lp(0, Ui.WC, 1f))
        dateRow.addView(next)
        root.addView(dateRow, lpTop(14))

        val dayRecs = act.db().recordsByDate(selDate)
        val done = dayRecs.filter { it.status == "已做" }
        val dishes = act.db().listDishes()
        val minMap = HashMap<String, Int>()
        for (d in dishes) minMap[d.name] = d.minutes
        val kcal = done.sumOf { it.kcal }
        val mins = done.sumOf { minMap[it.name] ?: 0 }

        val sumCard = Ui.card(c)
        val sumRow = Ui.row(c)
        sumRow.addView(stat("合计大卡", "$kcal"), Ui.lp(0, Ui.WC, 1f))
        sumRow.addView(stat("下厨", "${done.size} 道"), Ui.lp(0, Ui.WC, 1f))
        sumRow.addView(stat("耗时", "$mins 分"), Ui.lp(0, Ui.WC, 1f))
        sumCard.addView(sumRow, Ui.lp(Ui.MP, Ui.WC))
        root.addView(sumCard, lpTop(10))

        if (dayRecs.isEmpty()) {
            val e = Ui.tv(c, "这天还没开火 🍳\n回主页抽一张，或选别的日期", 12.5f, Ui.SUB)
            e.gravity = Gravity.CENTER
            e.setLineSpacing(Ui.dp(c, 4f).toFloat(), 1f)
            val ec = Ui.card(c, 14f, 22f)
            ec.gravity = Gravity.CENTER
            ec.addView(e, Ui.lp(Ui.MP, Ui.WC))
            root.addView(ec, lpTop(10))
        } else {
            for (r in dayRecs) root.addView(recRow(r), lpTop(10))
        }

        // 统计卡
        val allDone = act.db().allDone()
        val ym = today().substring(0, 7)
        val month = allDone.count { it.date.startsWith(ym) }
        val stCard = Ui.card(c)
        val stRow = Ui.row(c)
        stRow.addView(stat("连续下厨", "${computeStreak(allDone)} 天"), Ui.lp(0, Ui.WC, 1f))
        stRow.addView(stat("累计下厨", "${allDone.size} 次"), Ui.lp(0, Ui.WC, 1f))
        stRow.addView(stat("本月下厨", "$month 次"), Ui.lp(0, Ui.WC, 1f))
        stCard.addView(stRow, Ui.lp(Ui.MP, Ui.WC))
        root.addView(stCard, lpTop(10))

        val btn = Ui.button(c, "📸 生成分享卡（菜谱日记）", Ui.PRIMARY, 0xFFFFFFFF.toInt(), 14f)
        btn.setOnClickListener { share() }
        root.addView(btn, lpTop(12))
    }

    // ---------------- 我的足迹（全部历史） ----------------
    private fun renderFootprint() {
        val all = act.db().allDone()

        val stCard = Ui.card(c)
        val stRow = Ui.row(c)
        stRow.addView(stat("累计下厨", "${all.size} 次"), Ui.lp(0, Ui.WC, 1f))
        stRow.addView(stat("做过的菜", "${act.db().distinctDishCount()} 道"), Ui.lp(0, Ui.WC, 1f))
        stRow.addView(stat("连续下厨", "${computeStreak(all)} 天"), Ui.lp(0, Ui.WC, 1f))
        stCard.addView(stRow, Ui.lp(Ui.MP, Ui.WC))
        val hp = Ui.lp(Ui.MP, Ui.WC)
        hp.topMargin = Ui.dp(c, 14f)
        root.addView(stCard, hp)

        if (all.isEmpty()) {
            val e = Ui.tv(c, "还没有下厨记录。\n去主页抽一张，点「就做它」就会记在这里。", 12.5f, Ui.SUB)
            e.gravity = Gravity.CENTER
            e.setLineSpacing(Ui.dp(c, 4f).toFloat(), 1f)
            val ec = Ui.card(c, 14f, 22f)
            ec.gravity = Gravity.CENTER
            ec.addView(e, Ui.lp(Ui.MP, Ui.WC))
            root.addView(ec, lpTop(12))
            return
        }

        // 按日期分组，倒序
        val byDate = LinkedHashMap<String, MutableList<Rec>>()
        for (r in all) byDate.getOrPut(r.date) { ArrayList() }.add(r)

        val tip = Ui.tv(c, "点任意一道可回看做法 · 小红书也在里面", 11.5f, Ui.SUB)
        root.addView(tip, lpTop(14))

        for ((date, list) in byDate) {
            val head = Ui.tv(c, if (date == today()) "今天 · ${humanDate(date)}" else humanDate(date),
                13f, Ui.PRIMARY_D, true)
            val hPlus = Ui.lp(Ui.MP, Ui.WC)
            hPlus.topMargin = Ui.dp(c, 14f)
            hPlus.bottomMargin = Ui.dp(c, 6f)
            root.addView(head, hPlus)
            for (r in list) root.addView(footRow(r), lpTop(6))
        }
    }

    private fun stat(label: String, value: String): View {
        val col = Ui.col(c)
        col.gravity = Gravity.CENTER
        val v = Ui.tv(c, value, 17f, Ui.TEXT, true)
        v.gravity = Gravity.CENTER
        col.addView(v)
        val l = Ui.tv(c, label, 11f, Ui.SUB)
        l.gravity = Gravity.CENTER
        Ui.pad(l, 0, 4, 0, 0)
        col.addView(l)
        return col
    }

    private fun recRow(r: Rec): View {
        val card = Ui.card(c, 12f, 10f)
        val row = Ui.row(c)
        row.addView(Ui.tv(c, if (r.meal == "汤") "🍲" else "🍽️", 20f, Ui.TEXT))
        val info = Ui.col(c)
        info.addView(Ui.tv(c, r.name, 14f, Ui.TEXT, true))
        val sub = Ui.tv(c, "${r.meal} · ${r.taste} · ${r.rarity}", 11f, Ui.SUB)
        Ui.pad(sub, 0, 4, 0, 0)
        info.addView(sub)
        val lp = Ui.lp(0, Ui.WC, 1f)
        lp.leftMargin = Ui.dp(c, 10f)
        row.addView(info, lp)
        row.addView(Ui.tv(c, "${r.kcal} kcal", 12.5f, Ui.PRIMARY_D, true))
        card.addView(row, Ui.lp(Ui.MP, Ui.WC))
        card.setOnClickListener { openStepsOf(r.name) }
        return card
    }

    /** 足迹行：带做过几次 + 可点回看做法 */
    private fun footRow(r: Rec): View {
        val card = Ui.card(c, 11f, 9f)
        val row = Ui.row(c)
        row.addView(Ui.tv(c, if (r.meal == "汤") "🍲" else "🍽️", 17f, Ui.TEXT))
        val info = Ui.col(c)
        info.addView(Ui.tv(c, r.name, 13.5f, Ui.TEXT, true))
        val n = act.db().timesCooked(r.name)
        val sub = if (n > 1) "${r.meal} · 做过 $n 次" else r.meal
        val st = Ui.tv(c, sub, 10.5f, Ui.SUB)
        Ui.pad(st, 0, 3, 0, 0)
        info.addView(st)
        val lp = Ui.lp(0, Ui.WC, 1f)
        lp.leftMargin = Ui.dp(c, 10f)
        row.addView(info, lp)
        row.addView(Ui.tv(c, "${r.kcal}", 11.5f, Ui.SUB))
        card.addView(row, Ui.lp(Ui.MP, Ui.WC))
        card.setOnClickListener { openStepsOf(r.name) }
        return card
    }

    /** 从记录里回看这道菜的做法（菜谱可能已被删，做个兜底） */
    private fun openStepsOf(name: String) {
        val d = act.db().listDishes().firstOrNull { it.name == name }
        if (d == null) {
            val n = act.db().timesCooked(name)
            Toast.makeText(act, "「$name」已不在菜品池里，你一共做过 $n 次", Toast.LENGTH_LONG).show()
            return
        }
        val sv = ScrollView(c)
        val b = Ui.col(c)
        Ui.pad(b, 18, 8, 18, 8)
        val n = act.db().timesCooked(d.name)
        val tip = Ui.tv(c, "🍽️ 你一共做过 $n 次", 12f, Ui.PRIMARY_D, true)
        b.addView(tip)
        val lines = d.steps.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
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
        val xhs = Ui.tv(c, "📕 去小红书看这道菜", 12.5f, Ui.PRIMARY_D, true)
        Ui.pad(xhs, 0, 6, 0, 0)
        b.addView(xhs)
        xhs.setOnClickListener { Xhs.openDish(act, d.name) }
        sv.addView(b)
        AlertDialog.Builder(act)
            .setTitle("${d.name} · 做法")
            .setView(sv)
            .setPositiveButton("知道了", null)
            .setNeutralButton("小红书") { _, _ -> Xhs.openDish(act, d.name) }
            .show()
    }

    private fun computeStreak(allDone: List<Rec>): Int {
        val dates = HashSet<String>()
        for (r in allDone) dates.add(r.date)
        var n = 0
        var d = today()
        if (!dates.contains(d)) d = shift(d, -1)
        while (dates.contains(d)) {
            n++
            d = shift(d, -1)
        }
        return n
    }

    private fun share() {
        val dayRecs = act.db().recordsByDate(selDate)
        val streak = computeStreak(act.db().allDone())
        val lbl = if (selDate == today()) "今天" else selDate
        val bmp = ShareCard.generate(c, lbl, dayRecs, streak)
        val iv = ImageView(c)
        iv.setImageBitmap(bmp)
        iv.adjustViewBounds = true
        val pad = Ui.col(c)
        Ui.pad(pad, 12, 8, 12, 8)
        pad.addView(iv, Ui.lp(Ui.MP, Ui.WC))
        AlertDialog.Builder(act)
            .setTitle("菜谱日记")
            .setView(pad)
            .setPositiveButton("保存到相册") { _, _ ->
                try {
                    ShareCard.save(c, bmp)
                    Toast.makeText(act, "已保存到相册 📸", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(act, "保存失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }
}

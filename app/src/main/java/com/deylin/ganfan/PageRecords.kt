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

/** 记录页：按日查看 + 合计 + 连续下厨统计 + 分享卡 */
class PageRecords(private val act: MainActivity) {

    private val c: Context = act
    private var selDate: String = today()
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

    private fun lpTop(top: Int): LinearLayout.LayoutParams {
        val p = Ui.lp(Ui.MP, Ui.WC)
        p.topMargin = Ui.dp(c, top.toFloat())
        return p
    }

    private fun render() {
        root.removeAllViews()

        // 日期切换
        val dateRow = Ui.row(c)
        val prev = Ui.tv(c, "‹", 22f, Ui.PRIMARY_D, true)
        prev.setPadding(Ui.dp(c, 12f), 0, Ui.dp(c, 12f), 0)
        prev.setOnClickListener { selDate = shift(selDate, -1); render() }
        val next = Ui.tv(c, "›", 22f, Ui.PRIMARY_D, true)
        next.setPadding(Ui.dp(c, 12f), 0, Ui.dp(c, 12f), 0)
        next.setOnClickListener { selDate = shift(selDate, 1); render() }
        dateRow.addView(prev)
        val lbl = Ui.tv(c, if (selDate == today()) "今天" else selDate, 15f, Ui.TEXT, true)
        lbl.gravity = Gravity.CENTER
        dateRow.addView(lbl, Ui.lp(0, Ui.WC, 1f))
        dateRow.addView(next)
        root.addView(dateRow, Ui.lp(Ui.MP, Ui.WC))

        // 数据
        val dayRecs = act.db().recordsByDate(selDate)
        val done = dayRecs.filter { it.status == "已做" }
        val dishes = act.db().listDishes()
        val minMap = HashMap<String, Int>()
        for (d in dishes) minMap[d.name] = d.minutes
        val kcal = done.sumOf { it.kcal }
        val mins = done.sumOf { minMap[it.name] ?: 0 }

        // 汇总卡
        val sumCard = Ui.card(c)
        val sumRow = Ui.row(c)
        sumRow.addView(stat("合计大卡", "$kcal"), Ui.lp(0, Ui.WC, 1f))
        sumRow.addView(stat("下厨", "${done.size} 道"), Ui.lp(0, Ui.WC, 1f))
        sumRow.addView(stat("耗时", "$mins 分"), Ui.lp(0, Ui.WC, 1f))
        sumCard.addView(sumRow, Ui.lp(Ui.MP, Ui.WC))
        root.addView(sumCard, lpTop(10))

        // 记录列表
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
        val total = allDone.size
        val ym = today().substring(0, 7)
        val month = allDone.count { it.date.startsWith(ym) }
        val streak = computeStreak(allDone)
        val stCard = Ui.card(c)
        val stRow = Ui.row(c)
        stRow.addView(stat("连续下厨", "$streak 天"), Ui.lp(0, Ui.WC, 1f))
        stRow.addView(stat("累计下厨", "$total 次"), Ui.lp(0, Ui.WC, 1f))
        stRow.addView(stat("本月下厨", "$month 次"), Ui.lp(0, Ui.WC, 1f))
        stCard.addView(stRow, Ui.lp(Ui.MP, Ui.WC))
        root.addView(stCard, lpTop(10))

        // 分享按钮
        val btn = Ui.button(c, "📸 生成分享卡（菜谱日记）", Ui.PRIMARY, 0xFFFFFFFF.toInt(), 14f)
        btn.setOnClickListener { share() }
        root.addView(btn, lpTop(12))
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
        return card
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

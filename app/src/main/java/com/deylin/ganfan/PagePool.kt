package com.deylin.ganfan

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** 菜品池：全部菜品/汤 + 加菜 */
class PagePool(private val act: MainActivity) {

    private val c: Context = act
    private lateinit var listBox: LinearLayout
    private lateinit var countTv: TextView

    fun build(): View {
        val sv = ScrollView(c)
        val root = Ui.col(c)
        root.setBackgroundColor(Ui.BG)
        Ui.pad(root, 14, 12, 14, 16)

        countTv = Ui.tv(c, "", 12.5f, Ui.SUB)
        root.addView(countTv, Ui.lp(Ui.MP, Ui.WC))

        listBox = Ui.col(c)
        Ui.pad(listBox, 0, 10, 0, 0)
        root.addView(listBox, Ui.lp(Ui.MP, Ui.WC))

        val addBar = Ui.tv(c, "＋ 嘴又馋了？加道菜", 13.5f, Ui.PRIMARY_D, true)
        addBar.gravity = Gravity.CENTER
        addBar.setBackground(Ui.round(c, Ui.PRIMARY_LIGHT, 14f))
        addBar.setPadding(0, Ui.dp(c, 12f), 0, Ui.dp(c, 12f))
        addBar.setOnClickListener { openAdd() }
        val ap = Ui.lp(Ui.MP, Ui.WC)
        ap.topMargin = Ui.dp(c, 10f)
        root.addView(addBar, ap)

        sv.addView(root)
        render()
        return sv
    }

    private fun render() {
        val all = act.db().listDishes()
        val soupN = all.count { it.type == "汤" }
        countTv.text = "共 ${all.size} 道（含 $soupN 道汤）· 点任意一道看做法"
        listBox.removeAllViews()
        for (d in all) {
            val p = Ui.lp(Ui.MP, Ui.WC)
            p.bottomMargin = Ui.dp(c, 8f)
            listBox.addView(rowOf(d), p)
        }
    }

    private fun rowOf(d: Dish): View {
        val card = Ui.card(c, 13f, 11f)
        val r = Ui.row(c)
        r.addView(Ui.tv(c, if (d.type == "汤") "🍲" else "🍽️", 22f, Ui.TEXT))

        val info = Ui.col(c)
        val lp = Ui.lp(0, Ui.WC, 1f)
        lp.leftMargin = Ui.dp(c, 10f)

        val nameRow = Ui.row(c)
        nameRow.addView(Ui.tv(c, d.name, 14.5f, Ui.TEXT, true))
        if (d.source == "自己加") {
            val tag = Ui.tv(c, "自建", 9.5f, Ui.PRIMARY_D, true)
            tag.setBackground(Ui.round(c, Ui.PRIMARY_LIGHT, 6f))
            tag.setPadding(Ui.dp(c, 5f), Ui.dp(c, 1f), Ui.dp(c, 5f), Ui.dp(c, 1f))
            val tlp = Ui.lp(Ui.WC, Ui.WC)
            tlp.leftMargin = Ui.dp(c, 6f)
            nameRow.addView(tag, tlp)
        }
        if (d.type == "汤") {
            val tag = Ui.tv(c, "汤", 9.5f, Ui.PRIMARY_D, true)
            tag.setBackground(Ui.round(c, Ui.PRIMARY_LIGHT, 6f))
            tag.setPadding(Ui.dp(c, 5f), Ui.dp(c, 1f), Ui.dp(c, 5f), Ui.dp(c, 1f))
            val tlp = Ui.lp(Ui.WC, Ui.WC)
            tlp.leftMargin = Ui.dp(c, 6f)
            nameRow.addView(tag, tlp)
        }
        info.addView(nameRow, Ui.lp(Ui.MP, Ui.WC))

        if (d.desc.isNotEmpty()) {
            val ds = Ui.tv(c, d.desc, 11.5f, Ui.SUB)
            ds.maxLines = 1
            Ui.pad(ds, 0, 4, 0, 0)
            info.addView(ds)
        }

        val meta = StringBuilder("${d.meal} · ${d.taste} · ${d.rarity}")
        if (d.minutes > 0) meta.append(" · ${d.minutes}分钟")
        if (d.kcal > 0) meta.append(" · ${d.kcal}kcal")
        val tags = Ui.tv(c, meta.toString(), 11f, Ui.SUB)
        Ui.pad(tags, 0, 5, 0, 0)
        info.addView(tags)

        if (d.ref.isNotEmpty()) {
            val ref = Ui.tv(c, d.ref, 10f, Ui.PRIMARY_D)
            Ui.pad(ref, 0, 4, 0, 0)
            info.addView(ref)
        }

        r.addView(info, lp)
        card.addView(r, Ui.lp(Ui.MP, Ui.WC))
        card.setOnClickListener { showSteps(d) }
        return card
    }

    private fun showSteps(d: Dish) {
        val sv = ScrollView(c)
        val b = Ui.col(c)
        Ui.pad(b, 18, 8, 18, 8)
        val lines = d.steps.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            b.addView(Ui.tv(c, "这道还没写做法。", 13f, Ui.SUB))
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

        val tag = if (d.ref.isNotEmpty()) d.ref else "#粤菜家常"
        val xhs = Ui.tv(c, "📕 去小红书看这道菜 $tag", 12.5f, Ui.PRIMARY_D, true)
        val xp = Ui.lp(Ui.MP, Ui.WC)
        xp.topMargin = Ui.dp(c, 8f)
        b.addView(xhs, xp)
        xhs.setOnClickListener { Xhs.openDish(act, d.name) }

        sv.addView(b)
        AlertDialog.Builder(act)
            .setTitle("${d.name} · 做法")
            .setView(sv)
            .setPositiveButton("知道了", null)
            .setNeutralButton("小红书") { _, _ -> Xhs.openDish(act, d.name) }
            .show()
    }

    private fun openAdd() {
        var meal = "晚餐"
        var type = "菜"
        var taste = "咸"
        var rarity = "普通"

        val form = Ui.col(c)
        Ui.pad(form, 18, 10, 18, 4)

        fun label(t: String): TextView = Ui.tv(c, t, 11.5f, Ui.SUB)

        fun addField(t: String, v: View) {
            form.addView(label(t))
            val p = Ui.lp(Ui.MP, Ui.WC)
            p.topMargin = Ui.dp(c, 4f)
            p.bottomMargin = Ui.dp(c, 10f)
            form.addView(v, p)
        }

        fun edit(hint: String, numeric: Boolean): EditText {
            val e = EditText(c)
            e.hint = hint
            e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            if (numeric) e.inputType = InputType.TYPE_CLASS_NUMBER
            return e
        }

        fun picker(init: String, opts: Array<String>, onChange: (String) -> Unit): TextView {
            val t = Ui.tv(c, init, 13f, Ui.TEXT)
            t.setBackground(Ui.round(c, Ui.PRIMARY_LIGHT, 10f))
            t.setPadding(Ui.dp(c, 12f), Ui.dp(c, 9f), Ui.dp(c, 12f), Ui.dp(c, 9f))
            t.setOnClickListener {
                AlertDialog.Builder(act)
                    .setTitle("选择")
                    .setItems(opts) { _, w ->
                        onChange(opts[w])
                        t.text = opts[w]
                    }
                    .show()
            }
            return t
        }

        val name = edit("例如：豆豉蒸鲮鱼", false)
        addField("菜名", name)
        addField("适合餐次", picker("晚餐", arrayOf("早餐", "中餐", "晚餐")) { meal = it })
        addField("类型（菜 / 汤）", picker("菜", arrayOf("菜", "汤")) { type = it })
        addField("口味", picker("咸", arrayOf("酸", "甜", "苦", "辣", "咸", "鲜", "香", "淡", "臭")) { taste = it })
        addField("稀有度（越稀越难抽中）", picker("普通", arrayOf("普通", "稀有", "史诗", "传说", "神话")) { rarity = it })
        val kcalEdit = edit("300", true)
        addField("大卡(kcal，1人份)", kcalEdit)
        val minEdit = edit("30", true)
        addField("耗时(分钟)", minEdit)
        val descEdit = edit("让人想动手的描述", false)
        addField("一句话简介", descEdit)

        val stepsEdit = edit("每行一步，可留空", false)
        stepsEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        stepsEdit.minLines = 3
        stepsEdit.gravity = Gravity.TOP
        addField("做法", stepsEdit)

        val sv = ScrollView(c)
        sv.addView(form)

        AlertDialog.Builder(act)
            .setTitle("➕ 加道自己的菜")
            .setView(sv)
            .setPositiveButton("加进池子") { _, _ ->
                val nm = name.text.toString().trim()
                if (nm.isEmpty()) {
                    Toast.makeText(act, "先填菜名", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val kcal = kcalEdit.text.toString().toIntOrNull() ?: 0
                val mins = minEdit.text.toString().toIntOrNull() ?: 0
                val desc = descEdit.text.toString().trim()
                val st = stepsEdit.text.toString().trim()
                act.db().addDish(
                    Dish(nm, meal, type, taste, desc, kcal, mins, rarity, st, "自己加")
                )
                Toast.makeText(act, "加菜成功，已进抽取池 🎉", Toast.LENGTH_SHORT).show()
                render()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

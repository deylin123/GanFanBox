package com.deylin.ganfan

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

class MainActivity : Activity() {

    lateinit var content: FrameLayout
    lateinit var host: FrameLayout
    private var tab = 0
    private val navs = ArrayList<android.widget.TextView>()
    private var helper: DbHelper? = null

    fun db(): DbHelper {
        var h = helper
        if (h == null) {
            h = DbHelper(this)
            helper = h
        }
        return h
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setBackgroundColor(Ui.BG)
        col.addView(buildHeader(), Ui.lp(Ui.MP, Ui.WC))
        content = FrameLayout(this)
        col.addView(content, Ui.lp(Ui.MP, 0, 1f))
        col.addView(buildNav(), Ui.lp(Ui.MP, Ui.dp(this, 58f)))
        root.addView(col, FrameLayout.LayoutParams(Ui.MP, Ui.MP))

        host = FrameLayout(this)
        host.visibility = View.GONE
        root.addView(host, FrameLayout.LayoutParams(Ui.MP, Ui.MP))

        setContentView(root)
        show(0)
    }

    private fun buildHeader(): View {
        val h = Ui.col(this)
        h.setBackgroundColor(Ui.PRIMARY)
        Ui.pad(h, 18, 16, 18, 14)
        h.addView(Ui.tv(this, "🍚 干饭盲盒", 20f, 0xFFFFFFFF.toInt(), true))
        val sub = Ui.tv(this, "今天吃什么，让盲盒替你决定", 11.5f, 0xFFD8F5F0.toInt())
        Ui.pad(sub, 0, 5, 0, 0)
        h.addView(sub)
        return h
    }

    private fun buildNav(): View {
        val bar = Ui.col(this)
        bar.setBackgroundColor(Ui.CARD)
        bar.addView(Ui.divider(this), Ui.lp(Ui.MP, 1))
        val row = Ui.row(this)
        bar.addView(row, Ui.lp(Ui.MP, 0, 1f))
        val titles = arrayOf("🍳\n主页", "🍲\n菜品池", "📅\n记录")
        for (i in titles.indices) {
            val t = Ui.tv(this, titles[i], 11f, Ui.SUB)
            t.gravity = Gravity.CENTER
            t.layoutParams = Ui.lp(0, Ui.MP, 1f)
            t.setOnClickListener { show(i) }
            row.addView(t)
            navs.add(t)
        }
        return bar
    }

    fun show(i: Int) {
        tab = i
        content.removeAllViews()
        val v: View = when (i) {
            0 -> PageHome(this).build()
            1 -> PagePool(this).build()
            else -> PageRecords(this).build()
        }
        content.addView(v, FrameLayout.LayoutParams(Ui.MP, Ui.MP))
        for (k in navs.indices) {
            navs[k].setTextColor(if (k == i) Ui.PRIMARY_D else Ui.SUB)
            navs[k].setTypeface(if (k == i) Typeface.DEFAULT_BOLD else Typeface.DEFAULT)
        }
    }

    fun refresh() {
        show(tab)
    }
}

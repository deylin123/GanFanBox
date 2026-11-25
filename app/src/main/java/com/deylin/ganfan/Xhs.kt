package com.deylin.ganfan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * 关联小红书：用手上的菜名直接去小红书搜笔记。
 * 装了小红书 App 会被系统直接唤起（universal link），没装则走浏览器。
 */
object Xhs {

    fun searchKeyword(dishName: String): String = "$dishName 做法 粤菜"

    fun openDish(c: Context, dishName: String) {
        val kw = searchKeyword(dishName)
        val web = "https://www.xiaohongshu.com/search_result?keyword=" + Uri.encode(kw)
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(web))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            c.startActivity(i)
        } catch (e: Exception) {
            // 再退一步：尝试小红书 App 的搜索 scheme
            try {
                val i2 = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("xhsdiscover://search/result?keyword=" + Uri.encode(kw))
                )
                i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                c.startActivity(i2)
            } catch (e2: Exception) {
                Toast.makeText(c, "没找到可用的浏览器或小红书 App", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

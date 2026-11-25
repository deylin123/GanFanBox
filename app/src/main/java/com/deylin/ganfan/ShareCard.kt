package com.deylin.ganfan

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareCard {

    fun generate(c: Context, dateLabel: String, recs: List<Rec>, streak: Int): Bitmap {
        val w = 750
        val h = 1000
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)

        val bg = Paint()
        bg.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFF2DD4BF.toInt(), 0xFFEAF9F7.toInt()), null, Shader.TileMode.CLAMP
        )
        cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFF15403E.toInt()
        p.textAlign = Paint.Align.CENTER
        p.textSize = 46f
        p.isFakeBoldText = true
        cv.drawText("今天吃什么 · 菜谱日记", w / 2f, 90f, p)

        p.textSize = 24f
        p.color = 0xFF6F9A98.toInt()
        p.isFakeBoldText = false
        cv.drawText(dateLabel, w / 2f, 130f, p)

        val order = ArrayList<String>()
        order.add("早餐"); order.add("中餐"); order.add("晚餐")
        if (recs.any { it.meal == "汤" }) order.add("汤")

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        cardPaint.color = Color.WHITE
        val rr = RectF()
        var y = 170f
        for (m in order) {
            rr.set(60f, y, (w - 60).toFloat(), y + 140f)
            cv.drawRoundRect(rr, 22f, 22f, cardPaint)
            val title = if (m == "汤") "今日靓汤" else m
            p.textAlign = Paint.Align.LEFT
            p.color = 0xFF0E9488.toInt()
            p.textSize = 30f
            p.isFakeBoldText = true
            cv.drawText(title, 90f, y + 48f, p)
            val r = recs.firstOrNull { it.meal == m }
            if (r != null) {
                p.color = 0xFF15403E.toInt()
                p.textSize = 34f
                cv.drawText(r.name, 90f, y + 95f, p)
                p.color = 0xFF6F9A98.toInt()
                p.textSize = 22f
                p.isFakeBoldText = false
                cv.drawText("${r.kcal} kcal · ${r.rarity}", 90f, y + 126f, p)
            } else {
                p.color = 0xFF6F9A98.toInt()
                p.textSize = 26f
                p.isFakeBoldText = false
                cv.drawText("— 还没定 —", 90f, y + 95f, p)
            }
            y += 160f
        }

        val done = recs.filter { it.status == "已做" }
        val kcal = done.sumOf { it.kcal }
        p.textAlign = Paint.Align.CENTER
        p.color = 0xFF15403E.toInt()
        p.textSize = 28f
        p.isFakeBoldText = true
        cv.drawText("今日合计 $kcal kcal · 下厨 ${done.size} 道 · 连续 $streak 天", w / 2f, y + 40f, p)
        p.color = 0xFF0E9488.toInt()
        p.textSize = 22f
        cv.drawText("—— 自己煮的，最安心 ——", w / 2f, h - 50f, p)

        return bmp
    }

    fun save(c: Context, bmp: Bitmap) {
        val name = "干饭菜谱_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date()) + ".png"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/干饭盲盒")
        }
        val uri = c.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法创建相册文件")
        c.contentResolver.openOutputStream(uri)?.use { os ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os)
        }
    }
}

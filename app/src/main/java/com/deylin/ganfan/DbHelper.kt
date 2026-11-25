package com.deylin.ganfan

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "ganfan.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        makeTables(db)
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 增量：只补建表，绝不 DROP，保留用户已有数据
        makeTables(db)
        seed(db)
    }

    private fun makeTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS dishes(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "meal TEXT NOT NULL DEFAULT '晚餐'," +
                "type TEXT NOT NULL DEFAULT '菜'," +
                "taste TEXT NOT NULL DEFAULT '咸'," +
                "descr TEXT NOT NULL DEFAULT ''," +
                "kcal INTEGER NOT NULL DEFAULT 0," +
                "minutes INTEGER NOT NULL DEFAULT 0," +
                "rarity TEXT NOT NULL DEFAULT '普通'," +
                "steps TEXT NOT NULL DEFAULT ''," +
                "source TEXT NOT NULL DEFAULT '默认池')"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS records(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT NOT NULL," +
                "meal TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "taste TEXT NOT NULL DEFAULT ''," +
                "kcal INTEGER NOT NULL DEFAULT 0," +
                "rarity TEXT NOT NULL DEFAULT '普通'," +
                "status TEXT NOT NULL DEFAULT '已做')"
        )
    }

    private fun seed(db: SQLiteDatabase) {
        var n = 0
        db.rawQuery("SELECT COUNT(*) FROM dishes", null).use { if (it.moveToFirst()) n = it.getInt(0) }
        if (n > 0) return
        db.beginTransaction()
        try {
            for (d in Seed.DISHES) {
                val cv = ContentValues()
                cv.put("name", d.name)
                cv.put("meal", d.meal)
                cv.put("type", d.type)
                cv.put("taste", d.taste)
                cv.put("descr", d.desc)
                cv.put("kcal", d.kcal)
                cv.put("minutes", d.minutes)
                cv.put("rarity", d.rarity)
                cv.put("steps", d.steps)
                cv.put("source", d.source)
                db.insert("dishes", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private val DISH_COLS =
        "id,name,meal,type,taste,descr,kcal,minutes,rarity,steps,source"

    private fun readDish(c: Cursor): Dish = Dish(
        name = c.getString(1), meal = c.getString(2), type = c.getString(3),
        taste = c.getString(4), desc = c.getString(5), kcal = c.getInt(6),
        minutes = c.getInt(7), rarity = c.getString(8), steps = c.getString(9),
        source = c.getString(10), id = c.getLong(0)
    )

    fun listDishes(): List<Dish> {
        val out = ArrayList<Dish>()
        readableDatabase.rawQuery("SELECT $DISH_COLS FROM dishes ORDER BY id", null)
            .use { c -> while (c.moveToNext()) out.add(readDish(c)) }
        return out
    }

    /** 某餐次可抽的「菜」，排除汤 */
    fun dishesForMeal(meal: String): List<Dish> {
        val out = ArrayList<Dish>()
        readableDatabase.rawQuery(
            "SELECT $DISH_COLS FROM dishes WHERE meal=? AND type<>'汤' ORDER BY id",
            arrayOf(meal)
        ).use { c -> while (c.moveToNext()) out.add(readDish(c)) }
        return out
    }

    /** 汤池 */
    fun soups(): List<Dish> {
        val out = ArrayList<Dish>()
        readableDatabase.rawQuery("SELECT $DISH_COLS FROM dishes WHERE type='汤' ORDER BY id", null)
            .use { c -> while (c.moveToNext()) out.add(readDish(c)) }
        return out
    }

    fun dishCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM dishes", null)
            .use { if (it.moveToFirst()) return it.getInt(0) }
        return 0
    }

    fun addDish(d: Dish): Long {
        val cv = ContentValues()
        cv.put("name", d.name)
        cv.put("meal", d.meal)
        cv.put("type", d.type)
        cv.put("taste", d.taste)
        cv.put("descr", d.desc)
        cv.put("kcal", d.kcal)
        cv.put("minutes", d.minutes)
        cv.put("rarity", d.rarity)
        cv.put("steps", d.steps)
        cv.put("source", "自己加")
        return writableDatabase.insert("dishes", null, cv)
    }

    fun addRecord(r: Rec): Long {
        val cv = ContentValues()
        cv.put("date", r.date)
        cv.put("meal", r.meal)
        cv.put("name", r.name)
        cv.put("taste", r.taste)
        cv.put("kcal", r.kcal)
        cv.put("rarity", r.rarity)
        cv.put("status", r.status)
        return writableDatabase.insert("records", null, cv)
    }

    fun recordsByDate(date: String): List<Rec> {
        val out = ArrayList<Rec>()
        readableDatabase.rawQuery(
            "SELECT id,date,meal,name,taste,kcal,rarity,status FROM records WHERE date=? ORDER BY id",
            arrayOf(date)
        ).use { c ->
            while (c.moveToNext()) out.add(
                Rec(
                    date = c.getString(1), meal = c.getString(2), name = c.getString(3),
                    taste = c.getString(4), kcal = c.getInt(5), rarity = c.getString(6),
                    status = c.getString(7), id = c.getLong(0)
                )
            )
        }
        return out
    }

    fun allDone(): List<Rec> {
        val out = ArrayList<Rec>()
        readableDatabase.rawQuery(
            "SELECT id,date,meal,name,taste,kcal,rarity,status FROM records WHERE status='已做' ORDER BY date DESC,id DESC",
            null
        ).use { c ->
            while (c.moveToNext()) out.add(
                Rec(
                    date = c.getString(1), meal = c.getString(2), name = c.getString(3),
                    taste = c.getString(4), kcal = c.getInt(5), rarity = c.getString(6),
                    status = c.getString(7), id = c.getLong(0)
                )
            )
        }
        return out
    }

    fun doneDatesDesc(): List<String> {
        val out = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT date FROM records WHERE status='已做' ORDER BY date DESC", null
        ).use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }
}

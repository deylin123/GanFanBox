package com.deylin.ganfan

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "ganfan.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        makeTables(db)
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 增量：补建表 / 补列，绝不 DROP，保留用户已有数据与记录
        makeTables(db)
        ensureColumn(db, "dishes", "ref", "TEXT NOT NULL DEFAULT ''")
        // 老版本用户也能拿到新增菜谱：按菜名判重补入
        seed(db)
    }

    /** 给已有表补列（列已存在则跳过） */
    private fun ensureColumn(db: SQLiteDatabase, table: String, col: String, def: String) {
        var exists = false
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == col) { exists = true; break }
            }
        }
        if (!exists) db.execSQL("ALTER TABLE $table ADD COLUMN $col $def")
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
                "source TEXT NOT NULL DEFAULT '默认池'," +
                "ref TEXT NOT NULL DEFAULT '')"
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
        // 按日期查记录走索引，记录攒多了也不卡
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_rec_date ON records(date)")
    }

    /**
     * 预置菜谱：增量写入，按菜名判重。
     * 老用户升级后，新增的菜会自动补进池子，自己加的菜不受影响。
     */
    private fun seed(db: SQLiteDatabase) {
        val exists = HashSet<String>()
        db.rawQuery("SELECT name FROM dishes", null).use { c ->
            while (c.moveToNext()) exists.add(c.getString(0))
        }
        val todo = Seed.DISHES.filter { !exists.contains(it.name) }
        if (todo.isEmpty()) return

        db.beginTransaction()
        try {
            for (d in todo) {
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
                cv.put("ref", d.ref)
                db.insert("dishes", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private val DISH_COLS =
        "id,name,meal,type,taste,descr,kcal,minutes,rarity,steps,source,ref"

    private fun readDish(c: Cursor): Dish = Dish(
        name = c.getString(1), meal = c.getString(2), type = c.getString(3),
        taste = c.getString(4), desc = c.getString(5), kcal = c.getInt(6),
        minutes = c.getInt(7), rarity = c.getString(8), steps = c.getString(9),
        source = c.getString(10), ref = c.getString(11), id = c.getLong(0)
    )

    fun listDishes(): List<Dish> {
        val out = ArrayList<Dish>()
        readableDatabase.rawQuery("SELECT $DISH_COLS FROM dishes ORDER BY meal,id", null)
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
        cv.put("ref", d.ref)
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

    /** 删一条下厨记录（只删记录，不动菜品池） */
    fun deleteRecord(id: Long): Int =
        writableDatabase.delete("records", "id=?", arrayOf(id.toString()))

    /** 清空某一天的全部记录 */
    fun deleteRecordsByDate(date: String): Int =
        writableDatabase.delete("records", "date=?", arrayOf(date))

    private fun readRec(c: Cursor): Rec = Rec(
        date = c.getString(1), meal = c.getString(2), name = c.getString(3),
        taste = c.getString(4), kcal = c.getInt(5), rarity = c.getString(6),
        status = c.getString(7), id = c.getLong(0)
    )

    fun recordsByDate(date: String): List<Rec> {
        val out = ArrayList<Rec>()
        readableDatabase.rawQuery(
            "SELECT id,date,meal,name,taste,kcal,rarity,status FROM records WHERE date=? ORDER BY id",
            arrayOf(date)
        ).use { c -> while (c.moveToNext()) out.add(readRec(c)) }
        return out
    }

    fun allDone(): List<Rec> {
        val out = ArrayList<Rec>()
        readableDatabase.rawQuery(
            "SELECT id,date,meal,name,taste,kcal,rarity,status FROM records WHERE status='已做' ORDER BY date DESC,id DESC",
            null
        ).use { c -> while (c.moveToNext()) out.add(readRec(c)) }
        return out
    }

    /** 全部下厨记录（不分日期，倒序）——用来做「我的下厨足迹」 */
    fun allRecords(): List<Rec> = allDone()

    fun doneDatesDesc(): List<String> {
        val out = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT date FROM records WHERE status='已做' ORDER BY date DESC", null
        ).use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }

    /** 做过的菜有多少种（去重），看自己厨艺面铺得多宽 */
    fun distinctDishCount(): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(DISTINCT name) FROM records WHERE status='已做'", null
        ).use { if (it.moveToFirst()) return it.getInt(0) }
        return 0
    }

    /** 某道菜做过几次 */
    fun timesCooked(name: String): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM records WHERE status='已做' AND name=?", arrayOf(name)
        ).use { if (it.moveToFirst()) return it.getInt(0) }
        return 0
    }
}

package com.deylin.ganfan

/** 一道菜/汤 */
data class Dish(
    val name: String,
    val meal: String,       // 早餐 / 中餐 / 晚餐
    val type: String,       // 菜 / 汤
    val taste: String,
    val desc: String,
    val kcal: Int,
    val minutes: Int,
    val rarity: String,     // 普通 / 稀有 / 史诗 / 传说 / 神话
    val steps: String,      // 做法，换行分隔
    val source: String = "默认池",
    val ref: String = "",   // 小红书话题标签，如 #广式家常 #下饭
    val id: Long = 0L
)

/** 一条下厨记录 */
data class Rec(
    val date: String,       // YYYY-MM-DD
    val meal: String,       // 早餐 / 中餐 / 晚餐 / 汤
    val name: String,
    val taste: String,
    val kcal: Int,
    val rarity: String,
    val status: String = "已做",
    val id: Long = 0L
)

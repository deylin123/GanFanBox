const fs = require('fs');
const HTML = 'D:/WorkBuddy产物/2026-10-23-15-02-15/干饭盲盒工作台/index_v2.html';
const html = fs.readFileSync(HTML, 'utf8');
const m = html.match(/var FALLBACK_DISHES = (\[[\s\S]*?\n  \]);/);
if (!m) { console.error('NO_MATCH'); process.exit(1); }
const arr = Function('return ' + m[1])();
console.log('dish count:', arr.length);

const soup = new Set(['番茄薯仔排骨汤','丝瓜瘦肉汤','紫菜蛋花汤','玉米羹','虾米粉丝煮节瓜']);
function kq(s) {
  return '"' + String(s == null ? '' : s)
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\r/g, '')
    .replace(/\n/g, '\\n') + '"';
}

let out = 'package com.deylin.ganfan\n\n';
out += '// 预置 40 道粤菜家常（含 5 道汤），1-2 人份，含 3-6 步做法\n';
out += 'object Seed {\n    val DISHES: List<Dish> = listOf(\n';
arr.forEach((d, i) => {
  const ty = soup.has(d.n) ? '汤' : '菜';
  out += '        Dish(' +
    kq(d.n) + ', ' + kq(d.m) + ', ' + kq(ty) + ', ' + kq(d.t) + ', ' +
    kq(d.d || '') + ', ' + (d.k || 0) + ', ' + (d.min || 0) + ', ' +
    kq(d.r) + ', ' + kq(d.s || '') + ', ' + kq('默认池') + ')' +
    (i < arr.length - 1 ? ',' : '') + '\n';
});
out += '    )\n}\n';

const dest = 'app/src/main/java/com/deylin/ganfan/Seed.kt';
fs.writeFileSync(dest, out);
console.log('written', dest, out.length, 'bytes');

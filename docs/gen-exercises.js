// 动作示意图生成器：读取 exercise_lib 导出的 TSV，按动作名匹配姿势原型，输出 SVG + image_url 更新 SQL
const fs = require('fs')

const COLOR = { 胸: '#e74c3c', 背: '#3498db', 腿: '#9b59b6', 肩: '#f39c12', 手臂: '#e67e22', 核心: '#1abc9c', 有氧: '#2ecc71' }
const INK = '#34495e'

const ground = `<line x1="15" y1="188" x2="205" y2="188" stroke="#ececec" stroke-width="4" stroke-linecap="round"/>`
const head = (x, y) => `<circle cx="${x}" cy="${y}" r="13" fill="${INK}"/>`
const limb = (x1, y1, x2, y2, w = 7) => `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${INK}" stroke-width="${w}" stroke-linecap="round"/>`
const hl = (cx, cy, rx, ry, g) => `<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="${COLOR[g]}" opacity="0.35"/>`
// 杠铃：横杆 + 两端铃片
const barbell = (cx, cy, w) => `<line x1="${cx - w / 2}" y1="${cy}" x2="${cx + w / 2}" y2="${cy}" stroke="${INK}" stroke-width="4"/><circle cx="${cx - w / 2}" cy="${cy}" r="9" fill="${INK}"/><circle cx="${cx + w / 2}" cy="${cy}" r="9" fill="${INK}"/>`
// 哑铃：短柄 + 两端的圆片
const db = (x, y, angle = 0) => `<g transform="rotate(${angle} ${x} ${y})"><line x1="${x - 10}" y1="${y}" x2="${x + 10}" y2="${y}" stroke="${INK}" stroke-width="4"/><circle cx="${x - 10}" cy="${y}" r="5.5" fill="${INK}"/><circle cx="${x + 10}" cy="${y}" r="5.5" fill="${INK}"/></g>`
const svg = (inner) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 220" width="220" height="220"><rect width="220" height="220" fill="#ffffff"/>${ground}${inner}</svg>`

// ---- 姿势原型（front view，220x220，地面 y=188）----
const P = {
  // 卧推类：平躺凳上，手臂上举
  bench: (g, eq) => svg(`
    <rect x="35" y="128" width="130" height="10" rx="4" fill="#bdc3c7"/>
    ${limb(60, 138, 60, 178, 6)}${limb(140, 138, 140, 178, 6)}
    ${head(55, 118)}
    ${limb(66, 122, 150, 122)}
    ${hl(105, 116, 42, 12, g)}
    ${limb(150, 122, 132, 62)}${limb(150, 122, 168, 62)}
    ${eq === '杠铃' ? barbell(110, 52, 130) : db(112, 56, 0) + db(112, 56, 90)}`),
  // 哑铃飞鸟：平躺，手臂向两侧展开
  flyBench: (g) => svg(`
    <rect x="35" y="128" width="130" height="10" rx="4" fill="#bdc3c7"/>
    ${limb(60, 138, 60, 178, 6)}${limb(140, 138, 140, 178, 6)}
    ${head(55, 118)}
    ${limb(66, 122, 150, 122)}
    ${hl(105, 116, 42, 12, g)}
    ${limb(150, 122, 178, 92)}${limb(150, 122, 122, 92)}
    ${db(182, 88, 40)}${db(118, 88, -40)}`),
  // 深蹲类
  squat: (g, eq) => svg(`
    ${head(110, 62)}
    ${limb(110, 76, 104, 116)}
    ${hl(110, 135, 34, 18, g)}
    ${limb(104, 116, 76, 148)}${limb(76, 148, 78, 184)}
    ${limb(104, 116, 132, 148)}${limb(132, 148, 134, 184)}
    ${limb(104, 84, 150, 80)}${limb(150, 80, 146, 104)}
    ${eq === '杠铃' ? barbell(104, 72, 130) : db(146, 108, 0)}`),
  // 箭步蹲
  lunge: (g) => svg(`
    ${head(110, 52)}
    ${limb(110, 66, 110, 116)}
    ${hl(112, 120, 26, 30, g)}
    ${limb(110, 116, 78, 128)}${limb(78, 128, 74, 184)}
    ${limb(110, 116, 140, 138)}${limb(140, 138, 142, 184)}
    ${limb(110, 74, 84, 104)}${limb(110, 74, 136, 104)}
    ${db(80, 110, 0)}${db(140, 110, 0)}`),
  // 硬拉/俯身类
  deadlift: (g) => svg(`
    ${head(118, 92)}
    ${limb(114, 106, 96, 140)}
    ${hl(104, 122, 26, 16, g)}
    ${limb(96, 140, 90, 184)}${limb(96, 140, 122, 184)}
    ${limb(108, 112, 82, 152)}${limb(108, 112, 134, 152)}
    ${barbell(108, 162, 120)}`),
  // 划船类（俯身拉）
  row: (g, eq) => svg(`
    ${head(126, 88)}
    ${limb(120, 102, 98, 134)}
    ${hl(106, 118, 26, 14, g)}
    ${limb(98, 134, 92, 184)}${limb(98, 134, 120, 184)}
    ${limb(116, 108, 148, 128)}${limb(148, 128, 138, 104)}
    ${eq === '杠铃' ? barbell(120, 158, 110) : db(136, 100, 0)}`),
  // 肩上推举
  overhead: (g, eq) => svg(`
    ${head(110, 58)}
    ${limb(110, 72, 110, 124)}
    ${hl(110, 76, 24, 12, g)}
    ${limb(110, 124, 104, 184)}${limb(110, 124, 116, 184)}
    ${limb(110, 80, 92, 40)}${limb(110, 80, 128, 40)}
    ${eq === '杠铃' ? barbell(110, 32, 120) : db(88, 36, 0) + db(132, 36, 0)}`),
  // 弯举
  curl: (g) => svg(`
    ${head(110, 56)}
    ${limb(110, 70, 110, 122)}
    ${limb(110, 122, 104, 184)}${limb(110, 122, 116, 184)}
    ${limb(110, 78, 92, 108)}${limb(92, 108, 96, 78)}
    ${limb(110, 78, 128, 108)}${limb(128, 108, 132, 78)}
    ${hl(128, 96, 14, 18, g)}${hl(92, 96, 14, 18, g)}
    ${db(96, 70, 0)}${db(132, 70, 0)}`),
  // 侧平举
  lateral: (g) => svg(`
    ${head(110, 56)}
    ${limb(110, 70, 110, 122)}
    ${limb(110, 122, 104, 184)}${limb(110, 122, 116, 184)}
    ${limb(110, 76, 58, 100)}${limb(58, 100, 40, 92)}
    ${limb(110, 76, 162, 100)}${limb(162, 100, 180, 92)}
    ${hl(70, 92, 18, 12, g)}${hl(150, 92, 18, 12, g)}
    ${db(36, 88, 0)}${db(184, 88, 0)}`),
  // 前平举
  frontraise: (g) => svg(`
    ${head(110, 56)}
    ${limb(110, 70, 110, 122)}
    ${limb(110, 122, 104, 184)}${limb(110, 122, 116, 184)}
    ${limb(110, 76, 98, 44)}${limb(110, 76, 122, 44)}
    ${hl(108, 60, 20, 10, g)}
    ${db(96, 38, 0)}${db(124, 38, 0)}`),
  // 下压/双杠臂屈伸（手臂下推）
  pushdown: (g) => svg(`
    ${head(110, 52)}
    ${limb(110, 66, 110, 118)}
    ${limb(110, 118, 104, 184)}${limb(110, 118, 116, 184)}
    ${limb(110, 74, 92, 106)}${limb(92, 106, 94, 140)}
    ${limb(110, 74, 128, 106)}${limb(128, 106, 130, 140)}
    ${hl(94, 124, 12, 16, g)}${hl(128, 124, 12, 16, g)}
    ${db(94, 148, 0)}${db(130, 148, 0)}`),
  // 悬挂（引体/悬垂举腿）
  hang: (g, eq, legsUp) => svg(`
    <line x1="60" y1="34" x2="160" y2="34" stroke="${INK}" stroke-width="5" stroke-linecap="round"/>
    ${head(110, 62)}
    ${limb(110, 76, 110, 126)}
    ${hl(110, 90, 26, 16, g)}
    ${limb(110, 82, 86, 38)}${limb(110, 82, 134, 38)}
    ${legsUp
      ? limb(110, 126, 104, 96) + limb(110, 126, 116, 96) + hl(110, 128, 26, 10, g)
      : limb(110, 126, 105, 184) + limb(110, 126, 115, 184)}`),
  // 高位下拉/面拉
  pulldown: (g) => svg(`
    <line x1="70" y1="30" x2="150" y2="30" stroke="${INK}" stroke-width="5" stroke-linecap="round"/>
    <line x1="110" y1="30" x2="110" y2="44" stroke="${INK}" stroke-width="4"/>
    ${head(110, 70)}
    ${limb(110, 84, 110, 130)}
    ${hl(110, 96, 24, 16, g)}
    ${limb(110, 130, 104, 184)}${limb(110, 130, 116, 184)}
    ${limb(110, 90, 88, 60)}${limb(110, 90, 132, 60)}
    ${limb(88, 60, 110, 48)}${limb(132, 60, 110, 48)}`),
  // 山羊挺身
  backext: (g) => svg(`
    <rect x="40" y="130" width="70" height="10" rx="4" fill="#bdc3c7"/>
    ${head(128, 92)}
    ${limb(122, 104, 104, 132)}
    ${hl(112, 116, 26, 12, g)}
    ${limb(104, 132, 84, 152)}${limb(84, 152, 82, 184)}
    ${limb(104, 132, 100, 184)}
    ${limb(120, 108, 142, 128)}`),
  // 腿举
  legpress: (g) => svg(`
    <rect x="120" y="60" width="12" height="110" rx="4" fill="#bdc3c7"/>
    ${head(88, 84)}
    ${limb(92, 98, 108, 132)}
    ${hl(100, 118, 20, 14, g)}
    ${limb(108, 132, 132, 118)}${limb(132, 118, 134, 84)}
    ${limb(94, 104, 74, 124)}${limb(74, 124, 78, 146)}`),
  // 坐姿腿屈伸
  legext: (g) => svg(`
    <rect x="60" y="130" width="60" height="10" rx="4" fill="#bdc3c7"/>
    ${head(96, 84)}
    ${limb(98, 98, 104, 132)}
    ${limb(104, 132, 128, 128)}${limb(128, 128, 152, 124)}
    ${hl(128, 126, 24, 9, g)}
    ${limb(100, 104, 78, 122)}`),
  // 俯卧腿弯举
  legcurl: (g) => svg(`
    <rect x="45" y="138" width="120" height="10" rx="4" fill="#bdc3c7"/>
    ${head(58, 122)}
    ${limb(68, 130, 140, 132)}
    ${limb(140, 132, 158, 112)}${limb(158, 112, 148, 92)}
    ${hl(146, 122, 22, 18, g)}
    ${limb(74, 132, 66, 156)}${limb(74, 132, 88, 158)}`),
  // 提踵
  calf: (g) => svg(`
    ${head(110, 50)}
    ${limb(110, 64, 110, 116)}
    ${limb(110, 116, 106, 160)}${limb(106, 160, 104, 176)}
    ${limb(110, 116, 114, 160)}${limb(114, 160, 116, 176)}
    ${hl(108, 152, 26, 18, g)}
    <path d="M96 182 Q110 168 124 182" stroke="${INK}" stroke-width="5" fill="none" stroke-linecap="round"/>
    ${limb(110, 72, 92, 106)}${limb(110, 72, 128, 106)}
    ${db(88, 112, 0)}${db(132, 112, 0)}`),
  // 平板支撑/俯卧撑
  plank: (g) => svg(`
    ${head(64, 142)}
    ${limb(74, 148, 146, 148)}
    ${hl(106, 142, 36, 11, g)}
    ${limb(146, 148, 142, 184)}${limb(146, 148, 156, 184)}
    ${limb(80, 150, 66, 176)}${limb(80, 150, 92, 176)}`),
  // 卷腹
  crunch: (g) => svg(`
    ${head(96, 118)}
    ${limb(102, 130, 128, 150)}
    ${hl(112, 134, 24, 13, g)}
    ${limb(128, 150, 152, 128)}${limb(152, 128, 158, 156)}
    ${limb(106, 134, 82, 148)}${limb(106, 134, 122, 108)}`),
  // 俄罗斯转体
  twist: (g) => svg(`
    ${head(110, 96)}
    ${limb(110, 110, 110, 140)}
    ${hl(110, 124, 22, 13, g)}
    ${limb(110, 140, 86, 154)}${limb(110, 140, 134, 154)}
    ${limb(110, 118, 92, 132)}${limb(110, 118, 130, 136)}
    ${db(88, 136, 0)}${db(132, 140, 0)}`),
  // 跑步
  run: (g) => svg(`
    ${head(112, 46)}
    ${limb(108, 60, 104, 104)}
    ${limb(104, 104, 76, 126)}${limb(76, 126, 62, 158)}${limb(62, 158, 74, 184)}
    ${limb(104, 104, 132, 120)}${limb(132, 120, 140, 152)}${limb(140, 152, 158, 172)}
    ${hl(108, 82, 18, 16, g)}
    ${limb(106, 66, 132, 82)}${limb(132, 82, 154, 72)}
    ${limb(106, 66, 80, 86)}${limb(80, 86, 60, 78)}`),
  // 跳绳
  jumprope: (g) => svg(`
    <path d="M52 150 Q110 214 168 150" stroke="#95a5a6" stroke-width="4" fill="none"/>
    ${head(110, 52)}
    ${limb(110, 66, 108, 108)}
    ${limb(108, 108, 92, 134)}${limb(92, 134, 96, 162)}
    ${limb(108, 108, 124, 134)}${limb(124, 134, 120, 162)}
    ${hl(108, 80, 18, 16, g)}
    ${limb(108, 72, 84, 96)}${limb(84, 96, 56, 146)}
    ${limb(108, 72, 136, 96)}${limb(136, 96, 164, 146)}`),
  // 波比跳
  burpee: (g) => svg(`
    ${head(70, 138)}
    ${limb(80, 144, 140, 142)}
    ${hl(106, 136, 32, 11, g)}
    ${limb(140, 142, 138, 168)}${limb(140, 142, 154, 168)}
    ${limb(140, 142, 118, 170)}${limb(118, 170, 126, 184)}`),
  // 动感单车
  cycle: (g) => svg(`
    <circle cx="118" cy="158" r="24" fill="none" stroke="#bdc3c7" stroke-width="5"/>
    ${head(100, 74)}
    ${limb(102, 88, 110, 122)}
    ${limb(110, 122, 118, 150)}
    ${hl(106, 106, 18, 15, g)}
    ${limb(110, 122, 100, 146)}
    ${limb(104, 92, 124, 110)}${limb(104, 92, 86, 112)}`),
  // 游泳
  swim: (g) => svg(`
    <path d="M20 158 Q55 150 90 158 T160 158 T230 158" stroke="#85c1e9" stroke-width="4" fill="none"/>
    <path d="M20 172 Q55 164 90 172 T160 172 T230 172" stroke="#aed6f1" stroke-width="4" fill="none"/>
    ${head(74, 140)}
    ${limb(84, 146, 150, 146)}
    ${hl(112, 140, 34, 10, g)}
    ${limb(150, 146, 170, 158)}${limb(150, 146, 168, 140)}
    ${limb(88, 148, 70, 164)}`),
}

// 动作名 → [姿势原型, 是否举腿(悬挂类)]
const MAP = {
  '杠铃卧推': ['bench'], '哑铃卧推': ['bench'], '上斜哑铃卧推': ['bench'], '窄距卧推': ['bench'],
  '哑铃飞鸟': ['flyBench'], '绳索夹胸': ['flyBench'],
  '俯卧撑': ['plank'], '平板支撑': ['plank'],
  '双杠臂屈伸': ['pushdown'], '凳上臂屈伸': ['pushdown'], '绳索下压': ['pushdown'], '直臂下压': ['pushdown'],
  '深蹲': ['squat'], '前蹲': ['squat'],
  '箭步蹲': ['lunge'], '罗马尼亚硬拉': ['deadlift'],
  '杠铃划船': ['row'], '哑铃单臂划船': ['row'], '坐姿划船': ['row'], '划船机': ['row'],
  '腿举': ['legpress'], '腿屈伸': ['legext'], '腿弯举': ['legcurl'],
  '引体向上': ['hang'], '悬垂举腿': ['hang', true],
  '高位下拉': ['pulldown'], '面拉': ['pulldown'],
  '站姿提踵': ['calf'], '山羊挺身': ['backext'],
  '哑铃推举': ['overhead'], '杠铃推举': ['overhead'],
  '侧平举': ['lateral'], '前平举': ['frontraise'], '俯身飞鸟': ['row'],
  '杠铃弯举': ['curl'], '哑铃弯举': ['curl'], '锤式弯举': ['curl'],
  '卷腹': ['crunch'], '死虫式': ['crunch'],
  '俄罗斯转体': ['twist'],
  '慢跑': ['run'], '跳绳': ['jumprope'], '波比跳': ['burpee'], 'HIIT循环': ['burpee'],
  '动感单车': ['cycle'], '游泳': ['swim'],
}

// ---- 主流程 ----
const tsv = fs.readFileSync(process.argv[2], 'utf8').trim().split('\n')
const outDir = process.argv[3]
const sql = []
for (const line of tsv) {
  const [id, name, group, equipment] = line.split('\t')
  const pose = MAP[name]
  if (!pose) { console.error('未匹配姿势:', name); continue }
  const fn = P[pose[0]]
  // 姿势函数已返回完整 SVG 文档，直接落盘
  const inner = fn.length >= 3 ? fn(group, equipment, pose[1]) : fn.length === 2 ? fn(group, equipment) : fn(group)
  fs.writeFileSync(`${outDir}/${id}.svg`, inner)
  sql.push(`UPDATE exercise_lib SET image_url='/exercises/${id}.svg' WHERE id=${id};`)
}
fs.writeFileSync(process.argv[4], sql.join('\n') + '\n')
console.log(`生成 ${tsv.length} 个 SVG，SQL 已输出`)

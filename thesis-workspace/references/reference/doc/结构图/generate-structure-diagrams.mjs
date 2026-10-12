import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const outputDir = dirname(fileURLToPath(import.meta.url))

const trees = [
  {
    file: 'admin-structure',
    label: '管理员结构图',
    root: '管理员',
    children: [
      { title: '数据看板', children: ['核心\n指标', '趋势\n统计', '分布\n统计'] },
      { title: '用户管理', children: ['分页\n筛选', '启用/\n禁用', '重置\n密码', '删除\n账号'] },
      { title: '企业管理', children: ['分页\n筛选', '启用/\n禁用', '删除\n企业'] },
      { title: '岗位管理', children: ['分页\n筛选', '强制\n上下架', '删除\n岗位'] },
      { title: '简历管理', children: ['分页\n筛选', '查看\n简历', '删除\n简历'] },
      { title: '投递管理', children: ['分页\n筛选', '查看\n投递', '删除\n记录'] },
      { title: '举报管理', children: ['处理\n举报', '忽略\n举报', '处理\n备注', '删除\n举报'] },
      { title: '系统日志', children: ['登录\n日志', '操作\n日志', 'AI\n日志', '错误\n日志'] },
      { title: 'AI 配置', children: ['配置\n查看', '连通\n测试'] },
    ],
  },
  {
    file: 'user-structure',
    label: '用户结构图',
    root: '用户',
    children: [
      { title: '注册登录', children: ['用户\n注册', '账号\n登录'] },
      { title: '首页', children: ['岗位\n统计', '职位\n搜索', 'AI功能\n入口'] },
      { title: '个人中心', children: ['资料\n查看', '资料\n修改', '头像\n上传', '密码\n修改'] },
      {
        title: '职位中心',
        children: ['岗位\n列表', '岗位\n详情', '智能\n搜索'],
      },
      {
        title: '我的简历',
        children: ['简历\n管理', '经历\n维护', '附件\n上传', 'AI简历\n解析', 'AI简历\n分析', 'AI简历\n优化'],
      },
      { title: '智能匹配', children: ['岗位\n推荐', '岗位\n匹配', '刷新\n推荐'] },
      { title: '我的投递', children: ['投递\n岗位', '状态\n筛选', '撤销\n投递'] },
      { title: '我的收藏', children: ['收藏\n岗位', '取消\n收藏'] },
      {
        title: 'AI模拟面试',
        children: ['新建\n面试', '在线\n答题', '评分\n点评', '面试\n报告', '历史\n记录'],
      },
      { title: 'AI智能助手', children: ['在线\n问答', '对话\n记录', '快捷\n追问'] },
      { title: '举报反馈', children: ['举报\n岗位', '举报\n企业', '查看\n记录'] },
    ],
  },
  {
    file: 'company-structure',
    label: '企业结构图',
    root: '企业',
    children: [
      {
        title: '数据看板',
        children: ['岗位\n统计', '在线\n岗位', '投递\n统计', '待处理\n投递', '面试\n统计', '录用\n统计', '状态\n分布'],
      },
      {
        title: '企业信息',
        children: ['企业\n资料', '企业\nLogo', '行业\n规模', '地址\n官网', '企业\n简介', '密码\n修改'],
      },
      {
        title: '岗位管理',
        children: ['岗位\n列表', '发布\n岗位', '编辑\n岗位', '删除\n岗位', '技能\n标签', '上架\n下架'],
      },
      {
        title: '收到投递',
        children: ['投递\n列表', '岗位\n筛选', '状态\n筛选', '查看\n简历', '标记\n已查看', '邀请\n面试', '标记\n不合适', '确认\n录用'],
      },
      {
        title: '候选人库',
        children: ['候选人\n列表', '关键词\n搜索', 'AI匹配\n分析', '优势\n分析', '风险\n提示', '面试\n建议', '录用\n建议'],
      },
    ],
  },
]

const style = {
  marginX: 44,
  top: 44,
  rootWidth: 160,
  rootHeight: 58,
  moduleWidth: 40,
  moduleGap: 28,
  childWidth: 40,
  childGap: 14,
  childDrop: 22,
  moduleDrop: 22,
  rootDrop: 34,
  rootGap: 34,
  bottomPadding: 44,
  lineHeight: 19,
  stroke: '#171717',
  fill: '#ffffff',
  text: '#111111',
}

function xml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function textLines(label, x, y, options = {}) {
  const lines = label.split('\n')
  const { size = 13, weight = 400, singular = false } = options
  const lineHeight = singular ? 0 : 18
  const startY = singular ? y : y - ((lines.length - 1) * lineHeight) / 2

  return lines
    .map(
      (line, index) =>
        `<text x="${x}" y="${startY + index * lineHeight}" text-anchor="middle" ` +
        `dominant-baseline="middle" font-family="Microsoft YaHei, SimHei, sans-serif" ` +
        `font-size="${size}" font-weight="${weight}" fill="${style.text}">${xml(line)}</text>`,
    )
    .join('')
}

function verticalText(label, x, y, options = {}) {
  const characters = Array.from(label.replace(/\s+/g, ''))
  const { size = 13, weight = 400 } = options
  const lineHeight = style.lineHeight
  const startY = y - ((characters.length - 1) * lineHeight) / 2

  return characters
    .map(
      (character, index) =>
        `<text x="${x}" y="${startY + index * lineHeight}" text-anchor="middle" ` +
        `dominant-baseline="middle" font-family="Microsoft YaHei, SimHei, sans-serif" ` +
        `font-size="${size}" font-weight="${weight}" fill="${style.text}">${xml(character)}</text>`,
    )
    .join('')
}

function rect(x, y, width, height, radius = 0) {
  const rx = radius ? ` rx="${radius}"` : ''
  return `<rect x="${x}" y="${y}" width="${width}" height="${height}"${rx} ` +
    `fill="${style.fill}" stroke="${style.stroke}" stroke-width="1"/>`
}

function line(x1, y1, x2, y2) {
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" ` +
    `stroke="${style.stroke}" stroke-width="1" shape-rendering="crispEdges"/>`
}

function moduleWidth(module) {
  const count = module.children.length
  const childrenWidth = count * style.childWidth + Math.max(0, count - 1) * style.childGap
  return Math.max(style.moduleWidth, childrenWidth)
}

function rowWidth(modules) {
  const subtrees = modules.reduce((sum, module) => sum + moduleWidth(module), 0)
  return subtrees + Math.max(0, modules.length - 1) * style.moduleGap
}

function characters(label) {
  return Array.from(label.replace(/\s+/g, ''))
}

function boxHeight(labels, padding) {
  const longest = labels.reduce((max, label) => Math.max(max, characters(label).length), 1)
  return longest * style.lineHeight + padding
}

// 模块按左右顺序均分为上下两排，使两排宽度尽量接近
function splitModules(modules) {
  if (modules.length < 2) return [modules, []]

  let bestIndex = 1
  let bestScore = Number.POSITIVE_INFINITY

  for (let index = 1; index < modules.length; index += 1) {
    const upper = rowWidth(modules.slice(0, index))
    const lower = rowWidth(modules.slice(index))
    const score = Math.max(upper, lower) * 1000 + Math.abs(upper - lower)

    if (score < bestScore) {
      bestScore = score
      bestIndex = index
    }
  }

  return [modules.slice(0, bestIndex), modules.slice(bestIndex)]
}

function layoutRow(modules, startX) {
  let cursor = startX

  return modules.map((module) => {
    const subtreeWidth = moduleWidth(module)
    const centerX = cursor + subtreeWidth / 2
    const count = module.children.length
    const childrenWidth = count * style.childWidth + Math.max(0, count - 1) * style.childGap
    const firstChildCenter = centerX - childrenWidth / 2 + style.childWidth / 2

    cursor += subtreeWidth + style.moduleGap

    return {
      title: module.title,
      centerX,
      children: module.children.map((label, index) => ({
        label,
        centerX: firstChildCenter + index * (style.childWidth + style.childGap),
      })),
    }
  })
}

function drawRow(svg, row, moduleTop, moduleHeight, childTop, childHeight) {
  for (const module of row) {
    const moduleLeft = module.centerX - style.moduleWidth / 2
    svg.push(rect(moduleLeft, moduleTop, style.moduleWidth, moduleHeight))
    svg.push(verticalText(module.title, module.centerX, moduleTop + moduleHeight / 2, {
      size: 15,
      weight: 500,
    }))

    for (const child of module.children) {
      const childLeft = child.centerX - style.childWidth / 2
      svg.push(rect(childLeft, childTop, style.childWidth, childHeight, 4))
      svg.push(verticalText(child.label, child.centerX, childTop + childHeight / 2, {
        size: 13,
        weight: 400,
      }))
    }
  }
}

function buildDiagram(tree) {
  const [upperModules, lowerModules] = splitModules(tree.children)

  const upperRowWidth = rowWidth(upperModules)
  const lowerRowWidth = rowWidth(lowerModules)
  const width = Math.ceil(Math.max(upperRowWidth, lowerRowWidth) + style.marginX * 2)

  const upperRow = layoutRow(upperModules, (width - upperRowWidth) / 2)
  const lowerRow = layoutRow(lowerModules, (width - lowerRowWidth) / 2)

  const moduleHeight = boxHeight(tree.children.map((module) => module.title), 30)
  const childHeight = boxHeight(tree.children.flatMap((module) => module.children), 36)

  const upperChildTop = style.top
  const upperChildBottom = upperChildTop + childHeight
  const upperChildLineY = upperChildBottom + style.childDrop
  const upperModuleTop = upperChildLineY + style.moduleDrop
  const upperModuleBottom = upperModuleTop + moduleHeight
  const upperRootLineY = upperModuleBottom + style.rootDrop
  const rootTop = upperRootLineY + style.rootGap
  const rootBottom = rootTop + style.rootHeight
  const lowerRootLineY = rootBottom + style.rootGap
  const lowerModuleTop = lowerRootLineY + style.rootDrop
  const lowerModuleBottom = lowerModuleTop + moduleHeight
  const lowerChildLineY = lowerModuleBottom + style.moduleDrop
  const lowerChildTop = lowerChildLineY + style.childDrop
  const lowerChildBottom = lowerChildTop + childHeight
  const height = lowerChildBottom + style.bottomPadding

  const rootCenter = width / 2
  const rootLeft = rootCenter - style.rootWidth / 2

  const svg = []
  svg.push(
    `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" ` +
      `viewBox="0 0 ${width} ${height}" role="img" aria-label="${xml(tree.label)}">`,
  )
  svg.push(`<rect width="${width}" height="${height}" fill="#ffffff"/>`)
  svg.push('<g fill="none" stroke-linecap="square">')

  if (upperRow.length) {
    const firstUpper = upperRow[0].centerX
    const lastUpper = upperRow.at(-1).centerX
    svg.push(line(firstUpper, upperRootLineY, lastUpper, upperRootLineY))
    svg.push(line(rootCenter, upperRootLineY, rootCenter, rootTop))

    for (const module of upperRow) {
      svg.push(line(module.centerX, upperModuleBottom, module.centerX, upperRootLineY))
      svg.push(line(module.centerX, upperModuleTop, module.centerX, upperChildLineY))

      if (!module.children.length) continue

      svg.push(line(
        module.children[0].centerX,
        upperChildLineY,
        module.children.at(-1).centerX,
        upperChildLineY,
      ))

      for (const child of module.children) {
        svg.push(line(child.centerX, upperChildLineY, child.centerX, upperChildBottom))
      }
    }
  }

  if (lowerRow.length) {
    const firstLower = lowerRow[0].centerX
    const lastLower = lowerRow.at(-1).centerX
    svg.push(line(rootCenter, rootBottom, rootCenter, lowerRootLineY))
    svg.push(line(firstLower, lowerRootLineY, lastLower, lowerRootLineY))

    for (const module of lowerRow) {
      svg.push(line(module.centerX, lowerRootLineY, module.centerX, lowerModuleTop))
      svg.push(line(module.centerX, lowerModuleBottom, module.centerX, lowerChildLineY))

      if (!module.children.length) continue

      svg.push(line(
        module.children[0].centerX,
        lowerChildLineY,
        module.children.at(-1).centerX,
        lowerChildLineY,
      ))

      for (const child of module.children) {
        svg.push(line(child.centerX, lowerChildLineY, child.centerX, lowerChildTop))
      }
    }
  }

  drawRow(svg, upperRow, upperModuleTop, moduleHeight, upperChildTop, childHeight)
  drawRow(svg, lowerRow, lowerModuleTop, moduleHeight, lowerChildTop, childHeight)

  svg.push(rect(rootLeft, rootTop, style.rootWidth, style.rootHeight))
  svg.push(textLines(tree.root, rootCenter, rootTop + style.rootHeight / 2, {
    size: 16,
    weight: 500,
    singular: true,
  }))
  svg.push('</g></svg>')

  return svg.join('')
}

mkdirSync(outputDir, { recursive: true })

for (const tree of trees) {
  const svg = buildDiagram(tree)
  const file = join(outputDir, `${tree.file}.svg`)
  writeFileSync(file, `${svg}\n`, 'utf8')
  console.log(file)
}

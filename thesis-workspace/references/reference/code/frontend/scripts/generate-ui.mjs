import { readdirSync, readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { resolve, extname } from 'node:path'
import { fileURLToPath } from 'node:url'
import * as Icons from '@element-plus/icons-vue'
const root = fileURLToPath(new URL('../', import.meta.url))
function files(dir) { return readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(resolve(dir,e.name)):[resolve(dir,e.name)]) }
const sources=files(resolve(root,'src')).filter(p=>['.vue','.ts'].includes(extname(p))&&!p.includes('plugins')&&!p.endsWith('main.ts'))
const source=sources.map(p=>readFileSync(p,'utf8')).join('\n')
const names=Object.keys(Icons).filter(name=>new RegExp(`\\b${name}\\b`).test(source))
const components=[...new Set([...source.matchAll(/<el-([a-z-]+)/g)].map(m=>'El'+m[1].split('-').map(s=>s[0].toUpperCase()+s.slice(1)).join('')))].sort()
mkdirSync(resolve(root,'src/plugins'),{recursive:true})
writeFileSync(resolve(root,'src/plugins/ui.ts'),`// Register only components and icons used by this application.\n// Regenerate with: node scripts/generate-ui.mjs\nimport type { App } from 'vue'\nimport { ${components.join(', ')}, ElLoading } from 'element-plus'\nimport { ${names.join(', ')} } from '@element-plus/icons-vue'\n\nexport function registerUi(app:App) {\n  const components = { ${components.join(', ')} }\n  const icons = { ${names.join(', ')} }\n  for (const [name,component] of Object.entries({ ...components,...icons })) app.component(name,component)\n  app.directive('loading',ElLoading.directive)\n}\n`)
console.log('Registered',components.length,'components and',names.length,'icons')

const fs = require("fs");
// 1) application.yml: 删除 ai.api-key 的空字符串定义（改为注释），避免占位被空值覆盖
let t = fs.readFileSync("src/main/resources/application.yml", "utf8");
t = t.replace("  api-key: \"\"\n", "  # api-key 不配置时默认为空：系统自动运行 Mock 模式；接入真实模型时在此处或环境变量 AI_API_KEY 中填写\n");
fs.writeFileSync("src/main/resources/application.yml", t, "utf8");
// 2) dev/prod: OpenAI 绑定 api-key 使用带默认的占位，空 key 时也能启动
for (const f of ["application-dev.yml", "application-prod.yml"]) {
  let s = fs.readFileSync("src/main/resources/" + f, "utf8");
  s = s.replace("api-key: ${ai.api-key:}", "api-key: ${ai.api-key:not-configured}");
  fs.writeFileSync("src/main/resources/" + f, s, "utf8");
}
console.log("yml patched");
console.log(fs.readFileSync("src/main/resources/application.yml","utf8").split("\n").filter(l=>/api-key|ai:/i.test(l)).join("\n"));

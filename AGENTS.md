# EasyReadSkill

JavaFX 桌面工具。扫描技能目录里最外层的 `SKILL.md`，取出 frontmatter 的 `description`，调用百度或有道翻译，再写回同一字段。备份和翻译记忆在 `~/.easyReadSkill/`，不改技能正文。

用户操作说明在 `README.md`。本文件只记代码归属和改代码时必须守住的行为。行为变了，同一处改动里改本文件对应段落。

## 主路径

`Launcher` 启动 `App`。`App` 建 `AppState`，`MainView` 把顶栏、左列表、右详情接上。

JavaFX 应用线程只画界面；翻译 / 测连通 / 扫描等排队进 `NetworkWorker` 单线程（名 `network`），结果用 `Platform.runLater` 回界面。

## 顶栏

扫描、筛选、外观、API 和语言左右排列，垂直居中。窗口可自由缩放，拥挤时顶栏自动换行成两行。

## 扫描路径

已启用的路径始终排在未启用前面，两组之间一条淡横线。各组按路径不区分大小写升序。只改对话框里的显示顺序，不改配置里的保存顺序。

## 翻译

HTTP 翻译实现在独立模块 `translator/`（`com.translator`）。主应用在 `app/`。根目录是 Maven 聚合工程：

```bash
mvn install
```

主包 `com.song.service.factory` 只做 ProviderType 映射、语种码转换和 `HttpCalls` 取消桥接，不重复实现签名与请求。改厂商 API 行为时改 `translate`，再从根目录 `mvn install`（或至少 `mvn -pl translate,app -am install`）。

API 配置提供商含百度、有道与各大模型（DeepSeek / 智谱 / Gemini / Kimi / OpenAI / 千问）。大模型走 `com.llm.facade.LlmFacade`（OpenAI 兼容 HTTP，`java.net.http` + Gson，不引入 Spring AI）；密钥界面为首3尾4掩码并标长度，失焦写入 `~/.easyReadSkill/config.json` 并立即生效，小字提示环境变量是否已读到。

`com.llm` 与 `com.translator` 并列、互不依赖。LLM 对外只 export `com.llm.api` / `com.llm.facade`。

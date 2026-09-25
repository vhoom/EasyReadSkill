# EasyReadSkill

JavaFX 桌面工具。扫描技能目录里最外层的 `SKILL.md`，取出 frontmatter 的 `description`，调用百度或有道翻译，再写回同一字段。备份和翻译记忆在 `~/.easyReadSkill/`，不改技能正文。

备份语义只有一条：**备份 = 即将被覆盖掉的那段 description**，在第一次翻译该文件时才产生（扫描阶段不写备份）。用户选"按当前文件重新备份"时旧备份会留在记录里可查，不静默丢弃。

用户操作说明在 `README.md`。本文件只记代码归属和改代码时必须守住的行为。行为变了，同一处改动里改本文件对应段落。

## 主路径

`Launcher` 启动 `App`。`App` 建 `AppState`，`MainView` 把顶栏、左列表、右详情接上。

JavaFX 应用线程只画界面；翻译 / 测连通 / 扫描等排队进 `NetworkWorker` 单线程（名 `network`），结果用 `Platform.runLater` 回界面。

## 顶栏

扫描、筛选、外观、API 和语言左右排列，垂直居中。窗口可自由缩放，拥挤时顶栏自动换行成两行。

## 扫描路径

已启用的路径始终排在未启用前面，两组之间一条淡横线。各组按路径不区分大小写升序。只改对话框里的显示顺序，不改配置里的保存顺序。

扫描匹配规则（`FileScanner.isExternalSkill`）：**外部 skill 只按相对扫描根的路径判断**，满足其一即可——(1) 扫描根下**第一级**目录就是 `skills`（大小写不敏感），SKILL.md 在该目录之下（`<根>/skills/<技能名>/SKILL.md`、`<根>/skills/<分类>/<技能名>/SKILL.md` 都算）；(2) SKILL.md 正好落在扫描根下**二级目录**里（`<根>/<一级>/<二级>/SKILL.md`）。其它情况算非外部，例如插件缓存 `<根>/plugins/cache/<插件>/<版本>/skills/<技能名>/SKILL.md`（skills 不在第一级）、`<根>/某目录/SKILL.md`（只有一级）、`<根>/某目录/a/b/SKILL.md`（三级以上）。非外部的仍然照常扫描、列出、勾选、翻译，只是列表里带"非外部skill"标记（tooltip 说明原因）。判定只看路径、不看内容；文件不在任何已配置扫描路径下时按外部处理（无从判断，不打扰用户）。

## 翻译

HTTP 翻译实现在独立模块 `translate/`（`com.translator` + `com.llm`）。主应用在 `app/`。根目录是 Maven 聚合工程：

```bash
mvn install
```

主包 `com.song.service.factory` 只做 ProviderType 映射、语种码转换和 `HttpCalls` 取消桥接，不重复实现签名与请求。改厂商 API 行为时改 `translate`，再从根目录 `mvn install`（或至少 `mvn -pl translate,app -am install`）。

**不要只构建 `app` 模块**：`app` 的 `com.song:translate` 依赖在非聚合构建（例如 IDEA 里单模块 package）时会从本地仓库取 jar，改了 `translate/` 不 install 就会出现"找不到 `com.llm.api.*` 符号"。改完 `translate/` 先 `mvn -pl translate,app -am install`，或在 IDEA 里打开聚合工程根目录构建。

API 配置提供商含百度、有道与各大模型（DeepSeek / 智谱 / Gemini / Kimi / OpenAI / 千问）。大模型走 `com.llm.facade.LlmFacade`（OpenAI 兼容 HTTP，`java.net.http` + Gson，不引入 Spring AI）；密钥界面为首3尾4掩码并标长度，失焦写入 `~/.easyReadSkill/config.json` 并立即生效，小字提示环境变量是否已读到。

`com.llm` 与 `com.translator` 并列、互不依赖。LLM 对外只 export `com.llm.api` / `com.llm.facade`。

### 大模型（`com.llm`）

- 请求走 `com.llm.http.OpenAiCompatClient`（`java.net.http` 异步 + Gson），地址由 `com.llm.api.LlmEndpoints` 规范化：按需补 `/v1`、剥掉误填的 `/chat/completions`、`/models`、去尾斜杠。界面上要显示"实际请求"地址。
- 取消：`com.llm.api.LlmHttpHooks`；主应用在 `TranslatorHttpBridge` 里把 `HttpCalls::isCancelled` 接进去，"中断翻译"会取消在途 exchange（不是只能等超时）。
- 译文缓存键含厂商 + 模型 + 提示词指纹 + 语种 + 原文（`SkillFileService.buildCacheKey`），换模型或改提示词不会吃旧译文；旧格式键仍能被 `RecordManager.getCachedTranslation(new, legacy)` 命中并顺手升级。
- 提示词里的源/目标语言必须用自然语言名（`LlmLanguages.displayName`），不能把 `jp/kor/fra` 这类厂商码丢给模型。
- 分段阈值按 provider 取：`TranslationProvider.maxChunkLength()`，百度/有道 1800 字，大模型 8000 字。
- "测试连接"必须真发一次 chat（只测 `/models` 会让"测试成功但翻译 400"混过去）；模型下拉不要强插厂商默认模型，只在列表里存在时才选中；下拉框可编辑，`load()` / `applyModels()` 必须保证至少有一个条目（返回空列表时保留原值并提示"可直接输入模型 id"），否则点不开。

### 批量拼接翻译（只有大模型用）

多文件翻译时，纯大模型可以把多段原文拼成一次对话，但必须能自行拆回、段与段互不影响：

- 能力声明在 `TranslationProvider.maxBatchItems()` / `maxBatchChars()`：大模型 5 段 / 6000 字，百度/有道 1 段（沿用逐段 NMT）。
- 拼装与拆解在 `com.llm.facade.LlmBatch`：标记 `<<<EASEREAD#n>>>`，系统提示由 `LlmPrompts.batchSystem(userPrompt)` 在原提示词后追加"逐段翻译、原样保留标记、不要合并"的硬约束。原文里出现标记 → `packable` 返回 false，不批量。
- `SkillFileService.translateFiles()` 的步骤固定：逐文件准备（读文件 / 判冲突 / 建首次备份 / 算缓存键）→ 命中缓存直接写回 → 剩下的按 `planGroups()` 分组（段数上限 + 字符上限，单项超限独占一组）→ 组内一次 `translateBatch` → 按标记拆回后**逐文件**写回与记缓存。
- 任何异常（拆解失败、段数不符、HTTP 报错）都回退成逐段翻译，且**一段失败不影响同组其他段**；取消（`HttpCalls.causedByCancel`）则整组返回中断。
- 单文件路径 `translateFile()` 保持原样（逐段请求、命中缓存不请求）：批量只是"发请求"这一步的合并，备份/冲突/缓存语义不变。

## 数据与备份

- `config.json`：唯一写入者语义由 `ConfigManager` 保证——只覆盖本应用认识的顶层字段，本进程没改过密钥时采用磁盘上的最新值，空白不覆盖非空（只有 `baseUrl` / `model` 允许清空表示用默认）。结构版本 `AppConfig.SCHEMA_VERSION`，升级前留 `.v<旧版本>.bak` / `.legacy.bak`，坏文件留 `.bad`。扫描路径由 `scanPathsInitialized` 保护：用户清空后不再补回默认目录。
- `records.json`：改动只 `markDirty()`，由调用方在批处理结束时 `flush()`（翻译一批一个文件都不该整文件重写一次）。损坏时留 `.bad` 不崩。
- 失效记录（文件已不存在）由 `DataMaintenance.archiveOrphanRecordsOnce` 一次性归档到 `records.archive.json`，**只归档不删除**（`originalDescription` 是原始文案的唯一副本），归档前必须先成功备份 records.json。
- 技能目录被移动/改名时（插件版本号变化），首次备份会继承旧路径的原始备份（按"当前内容 == 旧记录的译文"匹配），不把译文登记成原文。
- 原文与备份/上次写入不一致时，`SkillFileService.hasConflict` 先预检，右侧弹三个按钮：按当前文件重新备份 / 沿用旧备份 / 跳过这些文件。冲突文件在用户选择前不得改动。
- 启动时 `AppState.startStatusVerification` 在后台按文件实际内容核对状态（只改状态，不动备份），保证四个筛选计数与文件现状一致。
- 两个人工兜底动作（右侧按钮）：**标记为已翻译**只登记状态、不改文件内容，原文优先从内容相同的记录里采用，找不到就记 `originalUnknown=true`（此时「还原成备份」明确失败，不假装成功）；**清空全部备份**是整库操作（不再对勾选/当前生效），弹三个按钮（全部清空 / 只清备份记录 / 取消），清空前必须 `ConfigManager.backupOnce(records.json, "clear-<时间戳>")` 留副本；技能文件不动，清空后所有文件都不能再还原。

## 内容指纹（同一技能分布在多个目录）

账要分两本记：**按路径**决定"这条记录属于哪个文件"，**按内容指纹**（`com.song.util.ContentHash`，SHA-256 前 16 字节，规范化 CRLF 与首尾空白）只回答"这段文字是不是同一段"。状态判断永远以本路径的记录为准，指纹只用于三件事：

1. **首次备份继承**：文件没有记录、但当前内容等于某条已翻译记录的译文时，采用它的原文备份（技能目录被移动、改名、或复制到另一个扫描根）。候选要求 `originalHash != translatedHash`（源语言=目标语言时两者相同，不能当原文来源），并优先选源文件已经不存在的记录。
2. **状态核对**：`AppState.detectStatus` 对没有记录的文件调用 `RecordManager.adoptKnownTranslation`，命中就按已翻译登记并采用原文，避免"描述已经是中文却显示未翻译"。
3. **快速比对**：`hasConflict` 先比指纹，没指纹再逐字比。

配套约束：

- 记录新增 `originalHash` / `translatedHash` / `originalUnknown` / `originalSourcePath`（老记录由 `fillMissingHashes()` 在启动时补，字段缺失不影响读取）。
- `RecordManager.get(path)` 精确键未命中时，按规范化路径找"同一物理文件"（同一技能被两个扫描根命中、或用别名路径访问），命中后把记录改挂到当前路径。
- 用户清空某个文件的备份后写入 `suppressedHashes[path] = 当时的指纹`：内容不变就不再自动采用别的记录的原文；文件一变（重新翻译/重新备份）压制自动失效。

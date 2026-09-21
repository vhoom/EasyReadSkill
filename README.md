# EasyReadSkill
EasyReadSkill 是一个基于 JavaFX 的技能说明翻译工具。它只扫描文件名恰好为 `SKILL.md` 的最外层技能目录，提取 frontmatter 里的 `Description`，调用翻译服务后写回文件。子目录里的 `skill.md` 或另一份 `SKILL.md` 不会被当成独立技能。

项目当前支持服务商 百度翻译 有道智云,后续增加适配

---

## 功能特性

- 只扫描最外层、文件名恰好为 `SKILL.md` 的技能目录
- 首次自动备份原始 Description
- 支持批量翻译、勾选翻译、停止翻译
- 支持还原原始备份、重新备份
- 支持翻译记忆，避免同一原文重复请求 API
- 支持翻译失败弹窗，并展示 API 文档错误说明
- 顶部筛选显示数量
- 支持全选、反选、取消选择
- 双击列表文件打开所在目录
- 支持浅色 / 深色主题（Claude 色板：暖奶油画布、珊瑚色主操作；深色为暖近黑产品表面），选择会写入配置
- 换肤与对话框有轻量淡入
- 顶栏可改源语言和目标语言
- 支持 Windows 原生标题栏深色主题
- 日志输出到 `~/.easyReadSkill/logs`

---

## 运行

### 方式一：完整依赖 Jar

下载：

```text
EasyReadSkill.jar
```

确保本机已安装：

```text
Java 17+
```

运行：

```powershell
java -jar EasyReadSkill.jar
```

### 方式二： 运行exe

下载：

```text
EasyReadSkill-windows-x64.zip
```

解压后进入目录，双击：

```text
EasyReadSkill.exe
```
---

## 快速使用

### 1. 配置 API

点击顶部：

```text
API 配置
```

选择提供商：

- 百度翻译
- 有道智云

然后选择服务类型，前往对应平台登陆注册都会送额度.并填写对应配置。

### 2. 添加扫描路径

点击：

```text
扫描路径管理
```

选择包含技能目录的路径。程序只认该路径下最外层的 `SKILL.md`。

### 3. 扫描文件

点击：

```text
扫描文件
```

左侧会列出扫描到的最外层 `SKILL.md`。

### 4. 选择翻译方式

- 翻译当前文件：只翻译当前选中文件
- 翻译勾选文件：翻译左侧所有勾选文件

### 5. 查看结果

右侧会显示：

- 备份数据
- 翻译更新时间
- 实际读取数据



---

## 数据目录

程序运行数据保存在：

```text
~/.easyReadSkill/
```

Windows 下通常是：

```text
C:\Users\你的用户名\.easyReadSkill\
```

目录结构：

```text
.easyReadSkill/
 config.json          API 配置
 records.json         备份记录与翻译记忆
 logs/                运行日志
 skin/                自动生成的皮肤 CSS
```


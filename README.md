# EasyReadSkill
EasyReadSkill 是一个基于 JavaFX 的 `skill.md` 翻译工具，用于扫描目录中的 `skill.md` 文件，提取其中的 `Description` 文本，调用多种翻译服务完成翻译，并将结果写回文件。

项目当前支持翻译服务商: 百度翻译 有道智云

---

## 功能特性

- 扫描指定目录下的 `skill.md` 文件
- 首次自动备份原始 Description
- 支持批量翻译、勾选翻译、停止翻译
- 支持还原原始备份、重新备份
- 支持翻译记忆，避免同一原文重复请求 API
- 支持翻译失败弹窗，并展示 API 文档错误说明
- 顶部筛选显示数量
- 支持全选、反选、取消选择
- 双击列表文件打开所在目录
- 支持浅色 / 深色 / 高对比度主题
- 组件级 UI 动画
- 支持 Windows 原生标题栏深色主题
- 日志输出到 `~/.easyReadSkill/logs`

---

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
java -jar .\EasyReadSkill.jar
```

### 方式二：jpackage 打包版

下载：

```text
EasyReadSkill-windows-x64.zip
```

解压后进入目录，双击：

```text
EasyReadSkill.exe
```
### 从源码构建
 环境要求
- JDK 17
- Maven Wrapper 或 Maven 3.8+
- Windows / macOS / Linux

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

选择包含 `skill.md` 的目录。

### 3. 扫描文件

点击：

```text
扫描文件
```

左侧会列出所有找到的 `skill.md`。

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

---

## 项目结构

```text
src/main/java/com/song/easyreadskill/
 App.java / Launcher.java
 config/          配置模型与读写
 model/           数据模型
 service/         扫描、解析、翻译、记录、翻译记忆
    factory/     多服务商翻译实现
 skin/            主题、设计令牌、CSS 生成
    animation/   动画配置与工具
    control/     带动画的 UI 控件
    windows/     Windows 原生标题栏适配
 ui/              主界面与弹窗
    config/      按提供商拆分的配置面板
 util/            工具类
```

---

## 许可

请根据你的实际发布需求补充 License。

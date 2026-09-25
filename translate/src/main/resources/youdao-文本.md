# 有道智云 - 文本翻译（NMT） API 文档

[有道智云文本翻译（NMT） API](https://ai.youdao.com/DOCSIRMA/html/trans/api/wbfy/) 提供通用文本翻译及领域化翻译能力。将输入的源语言文本转换成目标语言文本，支持自动语种识别及多国语言互译。

---

## 1. 协议与基础信息

| 配置项 | 详细描述 |
| :--- | :--- |
| **请求地址** | `https://openapi.youdao.com/api` |
| **传输方式** | HTTPS |
| **请求方式** | GET / POST |
| **字符编码** | UTF-8 |
| **请求格式** | 表单 (`application/x-www-form-urlencoded`) |
| **响应格式** | JSON |

---

## 2. 请求参数 (Request Parameters)

| 参数名称 | 类型 | 是否必填 | 含义 | 示例 / 描述 |
| :--- | :--- | :--- | :--- | :--- |
| `q` | string | **是** | 待翻译文本 | UTF-8 编码。单次查询限制 **5000** 字符（libcurl 等库需进行 URL 编码） |
| `from` | string | **是** | 源语言代码 | 详见**支持语种表**，可填 `auto`（自动识别） |
| `to` | string | **是** | 目标语言代码 | 详见**支持语种表** |
| `appKey` | string | **是** | 应用 ID | 可在控制台「应用管理」查看 |
| `salt` | string | **是** | 随机字符串 | 推荐使用 UUID，用于防重放 |
| `sign` | string | **是** | 签名认证 | 签名算法详见下文 **签名生成规则** |
| `signType` | string | **是** | 签名算法类型 | 固定填 `v3` |
| `curtime` | string | **是** | 时间戳 | 当前 UTC 时间戳（单位：秒） |
| `ext` | string | 否 | 翻译结果音频格式 | 支持 `mp3` |
| `voice` | string | 否 | 翻译发音选择 | `0` 为女声（默认），`1` 为男声（无男声时默认输出女声） |
| `strict` | string | 否 | 严格语言识别模式 | `true` / `false`（默认）。若为 `false`，则自动进行中英互译降级兼容 |
| `vocabId` | string | 否 | 用户自定义术语表 ID | 控制台创建的术语表 ID (`out_id`)，支持英中互译 |
| `domain` | string | 否 | 领域化翻译类型 | 默认为 `general`。可选：`computers` (计算机)、`medicine` (医学)、`finance` (金融经济)、`game` (游戏)。仅在中英互译且控制台开通后生效 |
| `rejectFallback` | string | 否 | 拒绝领域化降级 | `true` / `false`（默认）。为 `true` 时若领域化翻译失败则直接报错，不降级为通用翻译 |

> **注意事项：**
> 
> 1. `voice` 发音功能需要在控制台创建 TTS（语音合成）实例并绑定对应应用，否则会返回错误码 `110`。
> 2. 接口通过 `salt` + `curtime` 防重放攻击，同一个请求不可重放调用。

---

## 3. 签名生成规则 (v3)

签名计算公式如下：

$$
sign = sha256(appKey + input + salt + curtime + 应用密钥)
$$

其中 `input` 的计算规则如下：

1. 当待翻译文本 `q` 的字符长度 **$\le 20$** 时：
   
   $$
   input = q
   $$
2. 当待翻译文本 `q` 的字符长度 **$> 20$** 时：
   
   $$
   input = q[前10个字符] + len(q) + q[后10个字符]
   $$

---

## 4. 响应参数 (Response Fields)

| 字段名 | 类型 | 是否必有 | 含义及说明 |
| :--- | :--- | :--- | :--- |
| `errorCode` | string | **是** | 错误码，`0` 表示成功 |
| `query` | string | 否 | 源语言查询文本（请求成功时返回） |
| `translation` | Array[string] | 否 | 翻译结果列表（请求成功时返回） |
| `l` | string | **是** | 源语言和目标语言组合，格式如 `EN2zh-CHS` 或 `zh-CHS2ja` |
| `dict` | object | 否 | 词典 DeepLink 链接信息 |
| `webdict` | object | 否 | Web 端词典 DeepLink 链接信息 |
| `tSpeakUrl` | string | 否 | 翻译结果的发音音频地址（需绑定 TTS 服务） |
| `speakUrl` | string | 否 | 查询文本的发音音频地址（需绑定 TTS 服务） |
| `isDomainSupport` | string | 否 | 是否使用了领域翻译 (`true` / `false`)，仅在开通领域翻译时返回 |

### 响应示例

#### 1. 中英互译成功响应

```json
{
  "errorCode": "0",
  "query": "good",
  "isDomainSupport": "true",
  "translation": [
    "好"
  ],
  "dict": {
    "url": "yddict://[m.youdao.com/dict?le=eng&q=good](https://m.youdao.com/dict?le=eng&q=good)"
  },
  "webdict": {
    "url": "[http://m.youdao.com/dict?le=eng&q=good](http://m.youdao.com/dict?le=eng&q=good)"
  },
  "l": "EN2zh-CHS",
  "tSpeakUrl": "[https://openapi.youdao.com/ttsapi](https://openapi.youdao.com/ttsapi)?...",
  "speakUrl": "[https://openapi.youdao.com/ttsapi](https://openapi.youdao.com/ttsapi)?..."
}
```

#### 2. 小语种（如中日）翻译成功响应

**JSON**

```
{
  "errorCode": "0",
  "translation": [
    "大丈夫です"
  ],
  "dict": {
    "url": "yddict://[m.youdao.com/dict?le=jap&q=%E6%B2%A1%E5%85%B3%E7%B3%BB%E3%80%82](https://m.youdao.com/dict?le=jap&q=%E6%B2%A1%E5%85%B3%E7%B3%BB%E3%80%82)"
  },
  "webdict": {
    "url": "[http://m.youdao.com/dict?le=jap&q=%E6%B2%A1%E5%85%B3%E7%B3%BB%E3%80%82](http://m.youdao.com/dict?le=jap&q=%E6%B2%A1%E5%85%B3%E7%B3%BB%E3%80%82)"
  },
  "l": "zh-CHS2ja",
  "tSpeakUrl": "[https://openapi.youdao.com/ttsapi](https://openapi.youdao.com/ttsapi)?...",
  "speakUrl": "[https://openapi.youdao.com/ttsapi](https://openapi.youdao.com/ttsapi)?..."
}
```

---

## 5. 支持语种列表（部分常用）

| **语言** | **代码** | **支持自动识别** | **语言** | **代码** | **支持自动识别** |
| ---------------- | ---------------- | ------------------------ | ---------------- | ---------------- | ------------------------ |
| **自动识别**  | `auto`     | -                      | **简体中文**  | `zh-CHS`   | Y                      |
| **繁体中文**  | `zh-CHT`   | Y                      | **英语**      | `en`       | Y                      |
| **日语**      | `ja`       | Y                      | **韩语**      | `ko`       | Y                      |
| **法语**      | `fr`       | Y                      | **德语**      | `de`       | Y                      |
| **俄语**      | `ru`       | Y                      | **西班牙语**  | `es`       | Y                      |
| **葡萄牙语**  | `pt`       | Y                      | **意大利语**  | `it`       | Y                      |
| **泰语**      | `th`       | Y                      | **越南语**    | `vi`       | Y                      |
| **阿拉伯语**  | `ar`       | Y                      | **印地语**    | `hi`       | Y                      |
| **粤语**      | `yue`      | N                      | **印尼语**    | `id`       | Y                      |

---

## 6. JavaScript 请求示例 (Front-end)

**JavaScript**

```
var appKey = 'YOUR_APP_KEY';
var key = 'YOUR_APP_SECRET'; // 请注意：前端暴露 AppSecret 存在安全风险
var salt = (new Date()).getTime();
var curtime = Math.round(new Date().getTime() / 1000);
var query = '您好，欢迎使用有道智云文本翻译API';
var from = 'zh-CHS';
var to = 'en';

function truncate(q) {
  var len = q.length;
  if (len <= 20) return q;
  return q.substring(0, 10) + len + q.substring(len - 10, len);
}

var signStr = appKey + truncate(query) + salt + curtime + key;
var sign = CryptoJS.SHA256(signStr).toString(CryptoJS.enc.Hex);

$.ajax({
  url: '[https://openapi.youdao.com/api](https://openapi.youdao.com/api)',
  type: 'post',
  dataType: 'jsonp',
  data: {
    q: query,
    appKey: appKey,
    salt: salt,
    from: from,
    to: to,
    sign: sign,
    signType: "v3",
    curtime: curtime
  },
  success: function (data) {
    console.log(data);
  }
});
```

---

## 7. 常见错误代码表

| **错误码** | **含义及解决建议**                                              |
| ------------------ | ----------------------------------------------------------------------- |
| **0**           | **成功**                                                             |
| **101**         | 缺少必填参数。检查参数名称拼写及大小写是否完整正确                    |
| **102**         | 不支持的语言类型                                                      |
| **103**         | 翻译文本过长（单次最大支持 5000 字符）                                |
| **105**         | 不支持的签名类型（请确认`signType=v3`）                           |
| **108**         | 应用 ID 无效。请确认应用 ID 和密钥是否匹配及是否已创建                |
| **110**         | 无相关服务的有效应用。应用未绑定文本翻译或 TTS 发音服务               |
| **202**         | 签名检验失败。请检查密钥是否正确、待翻译文本`q`是否为 UTF-8 编码  |
| **206**         | 时间戳无效导致签名校验失败（校验本地与服务器 UTC 时间戳是否同步）     |
| **207**         | 重放请求（`salt`+`curtime`冲突，建议`salt`生成使用 UUID） |
| **310**         | 未开通领域翻译服务                                                    |
| **401**         | 账户欠费，请前往有道智云平台充值                                      |
| **411**         | 访问频率受限，请降低请求频率后重试                                    |



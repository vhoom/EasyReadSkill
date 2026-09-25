# 有道智云 - 大模型文本翻译 API 文档

输入一段文本，根据指定模型和目标语种，返回翻译结果。翻译结果以 SSE（Server-Sent Events）方式流式返回。

## 1. 接口协议与基础信息

| 配置项 | 详细描述 |
| :--- | :--- |
| **请求地址** | `https://openapi.youdao.com/proxy/http/llm-trans` |
| **传输方式** | HTTPS |
| **请求方式** | POST |
| **字符编码** | UTF-8 |
| **请求格式** | `application/json` 或 `application/x-www-form-urlencoded` |
| **响应格式** | `text/event-stream` |
| **QPS 限制** | 10 |

---

## 2. 请求参数 (Request Parameters)

| 参数名称 | 类型 | 是否必填 | 含义 | 示例/描述 |
| :--- | :--- | :--- | :--- | :--- |
| `appKey` | string | **是** | 应用 ID | 可在控制台「应用管理」查看 |
| `salt` | string | **是** | 随机值 | 推荐使用 UUID 等唯一随机字符串 |
| `curtime` | string | **是** | 时间戳（单位：秒） | 例如：`1757560399` |
| `sign` | string | **是** | 签名认证 | 签名算法详见下文 **签名生成规则** |
| `i` | string | **是** | 待翻译文本 | UTF-8 编码，限制 **5000** 字符 |
| `from` | string | **是** | 源语言代码 | 参考下表语言代码，可填 `auto` 自动识别 |
| `to` | string | **是** | 目标语言代码 | 参考下表语言代码，可填 `auto` 自动识别 |
| `prompt` | string | 否 | 提示词 | 限制 **1200** 字符、400 单词（仅对 `handleOption=0/3` 生效） |
| `streamType` | string | 否 | 流式返回类型 | `increment`（增量，默认） / `full`（全量） / `all`（增量+全量） |
| `handleOption` | string/int | 否 | 模型选择 | `0`：子曰翻译 pro 版 (14B)`3`：子曰翻译 lite 版 (1.5B) |
| `vocabId` | string | 否 | 用户自定义术语表 | 填入控制台创建的术语表 ID（`out_id`） |
| `enableInterTrans` | boolean | 否 | 开启指定语种双向互译 | `true` / `false` |

---

## 3. 签名生成规则 (v3)

签名签名逻辑公式如下：

$$
sign = sha256(appKey + input + salt + curtime + 应用密钥)
$$

其中 `input` 计算方式如下：

1. 当待翻译文本 `i` 的字符长度 **$\le 20$** 时：
   $$
   input = i
   $$
2. 当待翻译文本 `i` 的字符长度 **$> 20$** 时：
   $$
   input = i[前10位] + len(i) + i[后10位]
   $$

---

## 4. 支持语种说明

常见的 `from` 与 `to` 语言代码如下（完整列表详见官方文档）：

| 语言 | 代码 | 语言 | 代码 | 语言 | 代码 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **自动识别** | `auto` | **中文 (简体)** | `zh-CHS` | **英语** | `en` |
| **日语** | `ja` | **韩语** | `ko` | **法语** | `fr` |
| **德语** | `de` | **俄语** | `ru` | **西班牙语** | `es` |
| **葡萄牙语** | `pt` | **意大利语** | `it` | **繁体中文** | `zh-CHT` |
| **粤语** | `yue` | **泰语** | `th` | **越南语** | `vi` |

> **自动识别规则：**
> 
> - 当 `from=auto` 时，平台将自动检测源语言。
> - 当 `to=auto` 时，默认翻译为中文；若源语言本身为中文，则默认翻译为英文。

---

## 5. 响应结果 (Response)

响应结果通过 SSE (`text/event-stream`) 逐条推送 JSON 字符串，直至翻译结束。

### 响应字段说明

| 字段 | 类型 | 含义 |
| :--- | :--- | :--- |
| `code` | string | 状态码，`0` 表示成功 |
| `message` | string | 错误信息说明 |
| `requestId` | string | 本次请求的唯一标识符 |
| `successful` | boolean | 本条消息是否成功获取 |
| `data` | object | 翻译数据载体 |
| `data.transIncre` | string | **增量**翻译结果（当 `streamType` 为 `increment` 或 `all` 时返回） |
| `data.transFull` | string | **全量**翻译结果（当 `streamType` 为 `full` 或 `all` 时返回） |
| `data.langType` | string | 识别或翻译目标的语种 |

### 响应示例

#### 增量返回示例 (`streamType=increment`)

```
json
{"code":"0","message":"success","data":{"transIncre":"Hello","langType":"en"},"requestId":"1762951633928-57553213756784383-607","successful":true}
{"code":"0","message":"success","data":{"transIncre":",","langType":"en"},"requestId":"1762951633928-57553213756784383-607","successful":true}
{"code":"0","message":"success","data":{"transIncre":" I'm","langType":"en"},"requestId":"1762951633928-57553213756784383-607","successful":true}
```

#### 全量返回示例 (`streamType=full`)

**JSON**

```
{"code":"0","message":"success","data":{"transFull":"Hi","langType":"en"},"requestId":"1762951947136-700704342024750-326","successful":true}
{"code":"0","message":"success","data":{"transFull":"Hi, nice to meet you!","langType":"en"},"requestId":"1762951947136-700704342024750-326","successful":true}
```

#### 错误返回示例

**JSON**

```
{"code":"400","message":"'i'不能为空;","requestId":"1762952113361-700870567400125-470","successful":false}
```

---

## 6. 请求示例 (cURL)

**Bash**

```
curl --location --request POST '[https://openapi.youdao.com/proxy/http/llm-trans](https://openapi.youdao.com/proxy/http/llm-trans)'
--header 'Accept: */*'
--header 'Connection: keep-alive'
--header 'Content-Type: application/x-www-form-urlencoded'
--data-urlencode 'appKey=YOUR_APP_KEY'
--data-urlencode 'salt=09d703a3-e79d-41b9-ab92-2a19f9426cec'
--data-urlencode 'signType=v3'
--data-urlencode 'sign=66215f51c43fe4d357a74cd73b3af6792a08a5d5734de88563f5f072b9c2bee0'
--data-urlencode 'curtime=1762952138'
--data-urlencode 'i=你好，很高兴认识你！'
--data-urlencode 'handleOption=0'
--data-urlencode 'from=auto'
--data-urlencode 'to=en'
--data-urlencode 'streamType=full'
```

---

## 7. 错误代码列表

| **状态码** | **含义及排查建议**                                     |
| ------------------ | -------------------------------------------------------------- |
| `0`          | **成功**                                                    |
| `1`          | 未知错误，请联系客服                                         |
| `101`        | 缺少必填参数。请检查参数是否齐全及名称大小写拼写             |
| `108`        | 应用 ID 无效。请登录后台确认应用 ID 和密钥是否匹配           |
| `110`        | 当前应用 ID 无权限访问此服务。需要在控制台开通大模型翻译服务 |
| `112`        | 请求的服务不存在                                             |
| `202`        | 签名检验失败                                                 |
| `206`        | 时间戳无效导致签名校验失败（请检查本地与 UTC 时间偏差）      |
| `207`        | 重放请求                                                     |
| `902000`     | 大模型翻译底层调用失败                                       |

```

```


# 百度翻译开放平台 - 领域翻译 API 接入文档

[领域翻译 API](https://fanyi-api.baidu.com/doc/24) 针对特定领域的翻译展开优化，翻译结果相较于通用翻译 API 结果更为准确，句式更加符合该行业特点。现已开通学术论文、生物医药、信息技术、金融财经、机械制造、网络文学、新闻资讯、人文社科、航空航天、法律法规、合同共 11 个垂直领域，且领域在不断扩展中。

---

## 1. 产品简介

* **优化场景**：针对垂直行业深度优化，专业词汇与句式翻译更精准。
* **覆盖领域**：学术论文、生物医药、信息技术、金融财经、机械制造、网络文学、新闻资讯、人文社科、航空航天、法律法规、合同。

---

## 2. 接入步骤

1. 使用百度账号登录 [百度翻译开放平台](https://fanyi-api.baidu.com/)。
2. 完成注册，并在 [开发者信息](https://fanyi-api.baidu.com/manage/developer) 页面获取 **APPID** 与 **密钥**。
3. 进行开发者认证（推荐）。
4. 开通领域翻译 API 服务（[开通链接](https://fanyi-api.baidu.com/product/20)）。
5. 参考技术文档和 Demo 编写代码。
 

---

## 4. 接入技术指南

领域翻译 API 通过 HTTP 接口对外提供多语种互译服务。您只需要通过调用领域翻译 API，传入待翻译的内容，并指定要翻译的源语言（支持源语言语种自动检测）和目标语言种类，即可得到相应的翻译结果。

### 4.1 请求地址与协议

* **HTTPS 请求地址**：`https://fanyi-api.baidu.com/api/trans/vip/fieldtranslate`
* **HTTP 请求方式**：支持 `GET` 或 `POST`
  * 若使用 `POST` 方式，Header 中的 `Content-Type` 请指定为 `application/x-www-form-urlencoded`
* **字符编码**：统一采用 `UTF-8` 编码格式
* **文本长度限制**：为保证翻译质量，请将单次请求长度控制在 **6,000 字节** 以内（汉字约为 2,000 个）。

---

### 4.2 输入参数说明

| 字段名 | 类型 | 是否必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| **q** | text | 是 | 请求翻译 query | UTF-8 编码 |
| **from** | text | 是 | 翻译源语言 | 可设置为 `auto`（自动检测） |
| **to** | text | 是 | 翻译目标语言 | **不可**设置为 `auto` |
| **appid** | text | 是 | APPID | 可在 [管理控制台](https://fanyi-api.baidu.com/manage/developer) 查看 |
| **salt** | text | 是 | 随机数 | 可为字母或数字的字符串 |
| **domain** | text | 是 | 翻译领域类型 | 对应领域标识（具体见下表） |
| **sign** | text | 是 | 签名 | `appid + q + salt + domain + 密钥` 的 MD5 值 |
| **needIntervene** | integer | 否 | 是否使用自定义术语干预 API | `1` - 是，`0` - 否 |

---

### 4.3 `domain` 领域支持范围及语言方向

| 支持传入值 | 描述 | 支持语言方向 |
| :--- | :--- | :--- |
| **it** | 信息技术领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |
| **finance** | 金融财经领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |
| **machinery** | 机械制造领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |
| **senimed** | 生物医药领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |
| **novel** | 网络文学领域 | 中文（简） $\rightarrow$ 英语 |
| **academic** | 学术论文领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |
| **aerospace** | 航空航天领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |
| **wiki** | 人文社科领域 | 中文（简） $\rightarrow$ 英语 |
| **news** | 新闻资讯领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |
| **law** | 法律法规领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |
| **contract** | 合同领域 | 中文（简） $\rightarrow$ 英语、英语 $\rightarrow$ 中文（简） |

> **提示**：科技电子、水利机械、网络文学三个领域仅支持中到英，如设置语言方向为英到中，则默认输出通用翻译结果；生物医药、金融财经领域支持中到英和英到中两种语言方向。

---

### 4.4 签名生成方法（sign）

签名是为了保证调用安全，使用 MD5 算法生成的一段字符串，生成的签名长度为 32 位，签名中的英文字符均为小写格式。

#### 生成步骤：

1. **Step 1. 拼接字符串 1**：
   
   按 `appid + q + salt + domain + 密钥` 的固定顺序拼接字符串。
   
   * **注意**：待翻译文本 `q` 需为 UTF-8 编码；**在拼接 sign 前，`q` 不需要做 URL encode**。
2. **Step 2. 计算签名**：
   
   对字符串 1 进行 MD5 加密，得到 32 位小写的 `sign`。
3. **Step 3. 发送请求**：
   
   在生成签名之后、发送 HTTP 请求之前，才需要对待翻译文本字段 `q` 进行 **URL encode**。

> **常见报错解析**：如遇到报 `54001` 签名错误，请检查签名生成方法是否正确。很多开发者报错均是因为在拼接 sign 前就对 `q` 做了 URL encode。

---

### 4.5 输出参数

返回的结果为 **JSON** 格式，包含以下字段：

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| **from** | string | 源语言 | 返回用户指定的语言，或自动检测出的语种 |
| **to** | string | 目标语言 | 返回用户指定的目标语言 |
| **trans_result** | list | 翻译结果数组 | 包含 `src`（原文）与 `dst`（译文） |
| **trans_result.*.src** | string | 原文 | 请求示例中的 `amyotrophic lateral sclerosis` |
| **trans_result.*.dst** | string | 译文 | 请求示例中的 `肌萎缩性侧束硬化症` |
| **error_code** | integer | 错误码 | 仅当出现错误时显示 |

---

## 5. 接入举例

例如：将英文单词 `amyotrophic lateral sclerosis` 翻译成 `肌萎缩性侧束硬化症`（生物医药领域 `medicine`）。

### 1. 请求参数：

* `q` = `amyotrophic lateral sclerosis`
* `from` = `en`
* `to` = `zh`
* `appid` = `2015063000000001`（请替换为您的 appid）
* `salt` = `1435660288`（随机码）
* `domain` = `medicine`
* `密钥` = `12345678`（平台分配的密钥）

### 2. 生成签名 sign：

* **Step 1**：拼接字符串 1 $\rightarrow$ `2015063000000001amyotrophic lateral sclerosis1435660288medicine12345678`
* **Step 2**：计算签名 $\rightarrow$ `sign = md5("2015063000000001amyotrophic lateral sclerosis1435660288medicine12345678")`，得到 `sign = a649f9a644b25d717beee5ce600b40ae`

### 3. 拼接完整请求：

```http
GET [https://fanyi-api.baidu.com/api/trans/vip/fieldtranslate?q=amyotrophic+lateral+sclerosis&from=en&to=zh&appid=2015063000000001&salt=1435660288&domain=medicine&sign=a649f9a644b25d717beee5ce600b40ae](https://fanyi-api.baidu.com/api/trans/vip/fieldtranslate?q=amyotrophic+lateral+sclerosis&from=en&to=zh&appid=2015063000000001&salt=1435660288&domain=medicine&sign=a649f9a644b25d717beee5ce600b40ae)
```

### 4. 输出示例：

#### 正确情况 JSON：

**JSON**

```
{
    "from": "en",
    "to": "zh",
    "trans_result": [
        {
            "src": "amyotrophic lateral sclerosis",
            "dst": "肌萎缩性侧束硬化症"
        }
    ]
}
```

#### 异常情况 JSON：

**JSON**

```
{
    "error_code": "54001",
    "error_msg": "Invalid Sign"
}
```

---

## 6. 语种列表

源语言语种不确定时可设置为 `auto`，目标语言语种不可设置为 `auto`。但对于非常用语种，语种自动检测可能存在误差。

| **语言简写** | **名称** |
| -------------------- | ---------------- |
| **auto**          | 自动检测       |
| **zh**            | 中文           |
| **en**            | 英文           |

---

## 7. 错误码列表

当翻译结果无法正常返回时，请参考下表处理：

| **错误码** | **含义**       | **解决方案**                                                                                                                                |
| ------------------ | ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| **52000**       | 成功                 | -                                                                                                                                                 |
| **52001**       | 请求超时             | 检查传入的`q`参数是否是正常文本，以及`from`或`to`参数是否在支持的语种列表中                                                           |
| **52002**       | 系统错误             | 请重试                                                                                                                                            |
| **52003**       | 未授权用户           | 请检查`appid`是否正确，或是否已开通对应服务                                                                                                   |
| **54000**       | 必填参数为空         | 请检查是否漏传、误传参数                                                                                                                          |
| **54001**       | 签名错误             | 请检查签名生成方法是否有误（注意拼接 sign 前`q`不需要 URL encode）                                                                            |
| **54003**       | 访问频率受限         | 请降低您的调用频率，或在[管理控制台](https://fanyi-api.baidu.com/manage/developer?utm_source=gemini)进行身份认证后切换为高级版/尊享版                |
| **54004**       | 账户余额不足         | 请前往[管理控制台](https://fanyi-api.baidu.com/manage/developer?utm_source=gemini)为账户充值。如后台显示还有余额，说明当天用量计费金额已超过账户余额 |
| **54005**       | 长 query 请求频繁    | 请降低长度大于 1 万字节 query 的发送频率，3 秒后再试                                                                                              |
| **58000**       | 客户端 IP 非法       | 检查开发者信息页面填写的对应服务器 IP 地址是否正确，如服务器为动态 IP，建议留空不填                                                               |
| **58001**       | 译文语言方向不支持   | 检查译文语言是否在语言列表里                                                                                                                      |
| **58002**       | 服务当前已关闭       | 请前往[管理控制台](https://fanyi-api.baidu.com/manage/developer?utm_source=gemini)开启服务                                                           |
| **58003**       | 此 IP 已被封禁       | 同一 IP 当日使用多个 APPID 发送翻译请求，则该 IP 将被封禁当日请求权限，次日解封。请勿将 APPID 和密钥填写到第三方软件中                            |
| **90107**       | 认证未通过或未生效   | 请前往[我的认证](https://www.google.com/search?q=https://fanyi-api.baidu.com/my/auth&utm_source=gemini)查看认证进度                                  |
| **20003**       | 请求内容存在安全风险 | 请检查请求文本是否涉及反动、暴力等相关内容                                                                                                        |

```

```


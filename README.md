<p align = "center">
<img alt="Sym" src="https://images.gitee.com/uploads/images/2019/0301/103718_0b0df56e_1088279.png">
<br><br>
新一代的量化系统，为交易而构建
<br><br>
<a title="Build Status" target="_blank"><img src="https://img.shields.io/badge/build-passing-brightgreen.svg"></a>
<a title="Code Size" target="_blank"><img src="https://img.shields.io/badge/code%20size-7M-important.svg"></a>
<a title="AGPLv3" target="_blank"><img src="https://img.shields.io/badge/license-AGPLv3-green.svg"></a>
<a title="Releases" target="_blank"><img src="https://img.shields.io/badge/release-V0.1-yellow.svg"></a>
<a title="Downloads" target="_blank"><img src="https://img.shields.io/badge/downloads-168total-blue.svg"></a>
</p>

## 简介
GOAi 是一款基于JAVA的开源量化交易系统，起源于公司自研量化系统。目标从初期的交易所API接口封装，一步步成长为一套全功能量化交易平台。目前仅支持数字资产量化交易，未来会陆续支持A股、港股、美股、期货、期权等多种交易标的。
## 动机
#### 商业闭源平台问题：
* 不要把交易所密钥存储在第三方平台，重要的事情说三遍。
* 策略的保密性，策略是自己辛辛苦苦研究成果。平台无法确保存储的绝对安全。
* 如果代跑策略，用第三方平台并不方便。
* 商业平台的闭源性质，你并不清楚框架做了什么。
* 熊市本来就不赚钱，还要支出一笔额外费用。
#### 其它开源平台：
* 部分项目产品化做的不够好，偏技术，易用性较差。
* 部分项目个人业余时间进行维护，文档、bug修复、新增功能不够及时。
## 我们的优势
* 交易引擎100%开源
* 公司持续化维护运营，配备专门的开发、设计、测试人员。
* 产品优化会根据使用者的反馈第一时间做出调整。
* 定期举行线上线下的交流会。
## 功能
#### 现有功能：
* 支持主流交易所现货http接口（okexv3、火币、币安、bitfinex）。
* 支持交易所WebSocket连接方式（okexv3 ws接口）。
* 支持电报发消息通知。
* 接口的模板化封装。
#### 准备实现功能：
* 更多主流现货交易的支持。
* okex合约的支持。
* bitmex交易所支持。
* 回测系统的实现。
* 图表及收益曲线的支持。
* 多语言开发的支持 如 python、javascript 等。
## 界面

**登录页**

![index](https://images.gitee.com/uploads/images/2019/0307/164233_cff8e1f0_2076727.png)

**首页**

![index](https://images.gitee.com/uploads/images/2019/0307/164231_1a8ea26c_2076727.png)

**实例管理**

> 在线配置参数实例

![index](https://images.gitee.com/uploads/images/2019/0307/180525_1ce47b16_2076727.png)

> jar包本地获取配置参数实例

![index](https://images.gitee.com/uploads/images/2019/0307/164232_753a4090_2076727.png)

**策略管理**

![index](https://images.gitee.com/uploads/images/2019/0307/164231_51d0a7f1_2076727.png)

**交易所管理**

![index](https://images.gitee.com/uploads/images/2019/0307/164232_bc8a0ca2_2076727.png)


## 环境要求

**GOAi运行环境基本要求**
> 只需启动运行GOAi服务(策略实例、GOAi后台管理系统Web服务)情况
+ JDK(1.8及以上)
+ Linux(Centos7) 或 Windows

**GOAi策略开发环境基本要求**
> 策略开发调试基本环境，一般是在本地开发调试策略时所需要的环境要求
+ JDK(1.8及以上)
+ Gradle(4.8及以上)
+ Linux(Centos7) 或 Windows


## 一键启动方式

**Windows**

进入strategy目录，点击运行 `run_goai.bat` 脚本文件即可

**Linux**

进入strategy目录，点击运行 `run_goai.sh` 脚本文件即可

## 文档
* [API 接口文档](https://github.com/goaiquant/GOAi/wiki/GOAi-API-接口文档)
* [telegram 通知配置说明](https://github.com/goaiquant/GOAi/wiki/电报通知配置方法)

## 社群
* 公众号：了解最新产品动态，听策略大师、开发小哥、设计妹子吐槽吹水。
> ![index](https://images.gitee.com/uploads/images/2019/0307/164309_109bf364_2076727.png)
* GOAi官方QQ交流群：689620375  （群附件有GOAi使用的视频教程）

## 授权
引擎使用 AGPLV3 开源，须严格遵守 AGPLV3 的相关条款。

<h1 align="center"> 视频直播房 </h>

<p align="center">

<a href="https://github.com/rongcloud-community/rongcloud-scene-radio-room-android">
<img src="https://img.shields.io/cocoapods/l/RCSceneChatroomKit.svg?style=flat">
</a>

<a href="https://github.com/rongcloud-community/rongcloud-scene-radio-room-android">
<img src="https://img.shields.io/badge/%20in-java%2011-orange.svg">
</a>

</p>

## 简介

本仓库是基于 IM + RTC 实现语音电台的开源项目，音频合流采用的是 CDN 模式订阅，主要功能有语音直播、房间管理、消息、礼物等

## 集成

- 方式一：克隆项目到本地，该项目包含了子工程，执行 `git clone` 时在最后添加 `--recurse-submodules`

  `git clone https://github.com/rongcloud-community/rongcloud-scene-radio-room-android.git --recurse-submodules`

- 方式二：下载项目

	1. [下载主项目源码](https://github.com/rongcloud-community/rongcloud-scene-radio-room-android.git)
	2. [下载公共组件仓库源码](https://github.com/rongcloud-community/rongcloud-scene-common-android)
	3. 将公共仓库源码放到主项目 `scene-common` 文件夹下

## 功能

模块           |  简介 |  示图
:-------------------------:|:-------------------------:|:-------------------------:
<span style="width:200px">语音直播</span> | 主播说话，观众收听，聊天室消息发送和展示  |  <img width ="200" src="https://tva1.sinaimg.cn/large/e6c9d24ely1h188hyxtk4j20af0ijq3v.jpg">
房间音乐 | 基于 Hifive 实现音乐播放，需开通相关业务  |  <img width="200" src="https://tva1.sinaimg.cn/large/e6c9d24ely1h182xszyydj20af0ijq3v.jpg">
赠送礼物 | 支持单人、全服礼物发送，需二次开发对接业务  |  <img width ="200" src="https://tva1.sinaimg.cn/large/e6c9d24ely1h182u9yw13j20af0ij0tq.jpg">
房间设置 | 包含常见的房间信息管理  |  <img width ="200" src="https://tva1.sinaimg.cn/large/e6c9d24ely1h188ifbdndj20af0ij3z4.jpg">

## 架构

![](https://tva1.sinaimg.cn/large/e6c9d24ely1h1m0sex60tj21j20u0q54.jpg)

## 其他

如有任何疑问请提交 issue


# DanceCubeBot
一个基于Mirai的舞立方机器人

*目前只测试在**舞小铃**的账号上*，  
~~如果你看到了这个机器人，就说明它的框架是这个b写的~~

自用机器人，相当的屎山（为什么有人敢fork？🤔  
没时间维护代码和帮助文档，自己凑合着看吧💦  
因为是学生比较~~懒~~忙，不介意pr项目

## 一点提醒
如果真的有人敢fork，以下是一些注意事项：  

- 首先要在**运行jar目录之外(mcl文件夹并列)**的地方
创建一个文件夹 'DcConfig'，放入一个文件叫做 'UserTokens.json'
用于写入token
- 需要在 [制谱网站](https://danceweb.shenghuayule.com/MusicMaker/#/) 上
抓包找到一个类似心跳的东西，然后多复制几个它的key，写入DcConfig里面
某个文件（草我忘了，可以看一眼AbstractHandler类
- 其它注意的不清楚了，github简介的联系方式找我~~可能~~有办法
- 请不要高频http请求

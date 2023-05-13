package com.mirai.command;

import com.dancecube.api.UserInfo;
import com.dancecube.image.UserInfoImage;
import com.dancecube.token.Token;
import com.dancecube.token.TokenBuilder;
import com.mirai.MiraiBot;
import com.mirai.tools.HttpUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import okhttp3.Response;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mirai.config.AbstractConfig.*;

@SuppressWarnings("unused")
public class DefinedCommands {

    @BotCommand("菜单")
    public static final Command msgMenu = new CommandBuilder()
            .regex("菜单")
            .onCall(Scope.GLOBAL, (event, contact, qq) -> {
                String menu = """
                        舞小铃有以下功能哦！
                        1. 登录
                        -登录才能和舞小铃玩！
                        2. 个人信息
                        -查询舞立方资料
                        3. 机台登录 | 扫码
                        -拍照即可扫码舞立方机台！
                        4. 添加指令 [名称]
                        -换个方式查看信息！
                        5. 查找舞立方 [地名]
                        越详细地名越精确！
                        ❤️其它问题请联系铃酱!！""";
                contact.sendMessage(menu);
            }).build();

    @BotCommand("个人信息")
    public static final Command msgUserInfo = new CommandBuilder()
            .regex("个人信息")
            .onCall(Scope.GLOBAL, (event, contact, qq) -> {
                Token token = loginDetect(contact, qq);
                if(token==null) return;
                else if(!token.isAvailable()) {
                    contact.sendMessage("由于不可抗因素（bushi) 身份过期了💦\n重新私信登录即可恢复💦");
                    return;
                }
                InputStream inputStream = UserInfoImage.generate(token);
                if(inputStream!=null) {
                    Image image = HttpUtil.getImageFromStream(inputStream, contact);
                    contact.sendMessage(image);
                }
            }).build();

    @BotCommand("舞立方登录")
    public static final Command dcLogin = new CommandBuilder()
            .regex("登录|舞立方登录")
            .onCall(Scope.GLOBAL, (event, contact, qq) -> {
                // 限私聊
                if(contact instanceof Group) {
                    contact.sendMessage("私信才可以登录哦( •̀ ω •́ )/");
                    return;
                }
                // 正在登录检测
                if(logStatus.contains(qq)) {
                    contact.sendMessage("(´。＿。｀)不要重复登录啊喂！");
                    return;
                }
                logStatus.add(qq);
                TokenBuilder builder = new TokenBuilder();
                Image image = HttpUtil.getImageFromURL(builder.getQrcodeUrl(), contact);

                contact.sendMessage(new PlainText("快快用微信扫码，在五分钟内登录上吧~").plus(image));

                Token token = builder.getToken();

                if(token==null) {
                    contact.sendMessage("超时啦~ 请重试一下吧！");
                } else {
                    contact.sendMessage("登录成功啦~(●'◡'●)\n你的ID是：%s".formatted(token.getUserId()));
                    userTokensMap.put(qq, builder.getToken());  // 重复登录只会覆盖新的token
                    TokenBuilder.tokensToFile(userTokensMap, configPath + "UserTokens.json");
                }
                logStatus.remove(qq);
            }).build();


    @BotCommand("舞立方机台登录")
    public static final Command machineLogin = new CommandBuilder()
            .regex("机台登录|扫码")
            .onCall(Scope.GLOBAL, (event, contact, qq) -> {
                Token token = loginDetect(contact, qq);
                if(token==null) return;
                MessageChain messageChain = event.getMessage();
                EventChannel<Event> channel = GlobalEventChannel.INSTANCE.parentScope(MiraiBot.INSTANCE);
                CompletableFuture<MessageEvent> future = new CompletableFuture<>();
                channel.subscribeOnce(MessageEvent.class, future::complete);

                contact.sendMessage(new QuoteReply(messageChain).plus(new PlainText("请在3分钟之内发送机台二维码图片哦！\n一定要清楚才好！")));
                SingleMessage message;
                try {
                    MessageChain nextMessage = future.get(3, TimeUnit.MINUTES).getMessage();
                    List<SingleMessage> messageList = nextMessage.stream().filter(m -> m instanceof Image).toList();
                    if(messageList.size()!=1) {
                        contact.sendMessage(new QuoteReply(nextMessage).plus(new PlainText("这个不是图片吧...重新发送“机台登录”吧")));
                    } else {  // 第一个信息
                        message = messageList.get(0);
                        String imageUrl = Image.queryUrl((Image) message);
                        String qrUrl = HttpUtil.qrDecodeTencent(imageUrl);
                        if(qrUrl==null) {  // 若扫码失败
                            contact.sendMessage(new QuoteReply((MessageChain) message).plus(new PlainText("没有扫出来！再试一次吧！")));
                            return;
                        }
                        String url = "https://dancedemo.shenghuayule.com/Dance/api/Machine/AppLogin?qrCode=" + URLEncoder.encode(qrUrl, StandardCharsets.UTF_8);
                        try(Response response = HttpUtil.httpApi(url, Map.of("Authorization", "Bearer " + token.getAccessToken()))) {
                            //401 404
                            if(response!=null && response.code()==200) {
                                contact.sendMessage("登录成功辣，快来出勤吧！");
                            } else {
                                contact.sendMessage("二维码失效了，换一个试试看吧");
                            }
                        }
                    }
                } catch(InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                } catch(TimeoutException e) {
                    e.printStackTrace();
                    contact.sendMessage(new QuoteReply(messageChain).plus("超时啦，请重新发送吧~"));
                }
            }).build();


    //    @BotCommand("舞立方自制谱兑换")
    public static final Command gainMusicByCode = new CommandBuilder()
            .regex("[a-zA-Z0-9]{15}", false)
            .onCall(Scope.GLOBAL, (event, contact, qq) -> {
                Token token = loginDetect(contact, qq);
                if(token==null) return;

                String message = event.getMessage().contentToString();
                Matcher matcher = Pattern.compile("[a-zA-Z0-9]{15}").matcher(message);
                if(matcher.find()) {
                    matcher.group();
                }
            }).build();

    public static final Command msgUserInfoLegacy = new CommandBuilder()
            .regex("个人信息-l")
            .onCall(Scope.GLOBAL, (event, contact, qq) -> {
                loginDetect(contact, qq);
                Token token = userTokensMap.get(qq);
                UserInfo user = new UserInfo(token);
                Image image = HttpUtil.getImageFromURL(user.getHeadimgURL(), contact);
                String info = "昵称：%s\n战队：%s\n积分：%d\n金币：%d\n全国排名：%d".formatted(user.getUserName(), user.getTeamName(), user.getMusicScore(), user.getGold(), user.getRankNation());
                contact.sendMessage(image.plus(info));
            }).build();


    public static Token loginDetect(Contact contact, Long qq) {
        Token token = userTokensMap.get(qq);
        if(token==null) {
            // 登录检测
            contact.sendMessage("好像还没有登录欸(´。＿。｀)\n私信发送\"登录\"一起来玩吧！");
            userInfoCommands.put(qq, new HashSet<>());
            return null;
        }
        return token;
    }
}

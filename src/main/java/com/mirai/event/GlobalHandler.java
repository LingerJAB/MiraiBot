package com.mirai.event;

import com.dancecube.api.MachineList;
import com.dancecube.api.UserInfo;
import com.dancecube.token.Token;
import com.dancecube.token.TokenBuilder;
import com.mirai.HttpUtils;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// 不过滤通道
public class GlobalHandler extends Handler {
    @EventHandler
    public static void eventCenter(MessageEvent event) {
        String content = event.getMessage().contentToString();
        long qq = event.getSender().getId();
        Contact contact = event.getSubject();

        // 发送消息检测
        switch(content) {
            case "个人信息" -> msgUserInfo(contact, qq);
            case "登录" -> dcLogin(contact);
            case "#save" -> saveTokens(contact);
            case "#load" -> loadTokens(contact);
            default -> {  // 带参指令 & 自定义指令
                ArrayList<String> params = new ArrayList<>(Arrays.asList(content.strip().split(" ")));
                String prefix = params.remove(0);

                switch(prefix) {  //TODO 多匹配指令前缀 (List)
                    case "查找舞立方" -> msgMachineList(contact, params.get(0));
                    case "查找" -> msgMachineList(contact, params.get(0));
                }

            }
        }

    }

    // 个人信息 全局
    public static void msgUserInfo(Contact contact, long qq) {
        Token token = userMap.get(qq);
        if(token==null) {
            // 登录检测
            contact.sendMessage("好像还没有登录欸(´。＿。｀)\n私信发送\"舞立方登录\"一起来玩吧！");
            return;
        }

        token.refresh();

        UserInfo user;
        ExternalResource ex;
        try {
            user = UserInfo.get(token);
            ex = HttpUtils.getExResByURL(new URL(user.HeadimgURL));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        Image image = ExternalResource.uploadAsImage(ex, contact);

        String info = "昵称：%s\n战队：%s\n积分：%d\n全国排名：%d"
                .formatted(user.UserName, user.TeamName, user.MusicScore, user.RankNation);

        contact.sendMessage(image.plus(info));
    }

    // 机台检测 全局
    public static void msgMachineList(Contact contact, String region) {
        StringBuilder list = new StringBuilder("舞立方机台列表：");
        List<MachineList> lists = MachineList.get(region);
        if(lists==null) return;
        for(MachineList machine : lists) {
            String online = machine.Online ? "在线" : "离线";
            String singleInfo = "店名：%s *%s\n地址：%s"
                    .formatted(machine.PlaceName, online, machine.Address);
            list.append("\n\n").append(singleInfo);
        }

        contact.sendMessage(list.toString());
    }

    // 舞立方登录 好友
    public static void dcLogin(Contact contact) {
        // 限私聊
        if(contact instanceof Group) {
            contact.sendMessage("私信才可以登录哦( •̀ ω •́ )/");
            return;
        }
        long qq = contact.getId();
        // 正在登录检测
        if(logStatus.contains(qq)) {
            contact.sendMessage("(´。＿。｀)不要重复登录啊喂！");
            return;
        }
        logStatus.add(qq);

        try {

            TokenBuilder builder = new TokenBuilder();
            ExternalResource ex = HttpUtils.getExResByURL(new URL(builder.getQrcodeUrl()));
            Image image = ExternalResource.uploadAsImage(ex, contact);

            contact.sendMessage(new PlainText("快快用微信扫码，在五分钟内登录上吧~").plus(image));
            ex.close();
            Token token = builder.getToken();

            if(token==null) {
                contact.sendMessage("超时啦~ 请重试一下吧！");
            } else {
                contact.sendMessage("登录成功啦~(●'◡'●)\n你的ID是：%s".formatted(token.getUserId()));
                userMap.put(qq, builder.getToken());  // 重复登录只会覆盖新的token
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        logStatus.remove(qq);
    }

    // #save 高级
    public static void saveTokens(Contact contact) {
        String path = rootPath + "/DcConfig/UserToken.json";
        TokenBuilder.tokensToFile(userMap, path);
        contact.sendMessage("保存成功！共%d条".formatted(userMap.size()));
    }

    // #load 高级
    public static void loadTokens(Contact contact) {
        String path = rootPath + "/DcConfig/UserToken.json";
        userMap = TokenBuilder.tokensFromFile(path, true);
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<Long, Token> entry : userMap.entrySet()) {
            Long qq = entry.getKey();
            Token token = entry.getValue();
            sb.append("qq: %d , id: %s;\n".formatted(qq, token.getUserId()));
        }
        contact.sendMessage("加载成功！共%d条".formatted(userMap.size()));
        contact.sendMessage(sb.toString());
    }
}

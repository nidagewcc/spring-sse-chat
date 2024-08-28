package org.example.chatdemo.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseClient {

    public static final Map<String, SseEmitter> sseMap = new ConcurrentHashMap<>();
    public static final Map<String, Boolean> runningMap = new ConcurrentHashMap<>();

    public SseEmitter createSee(String uid) {
        //默认30秒超时,设置为0L则永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        //完成后回调
        sseEmitter.onCompletion(() -> {
            log.info("[{}]结束连接...................", uid);
            sseMap.remove(uid);
            runningMap.remove(uid);
        });
        //超时回调
        sseEmitter.onTimeout(() -> {
            log.info("[{}]连接超时...................", uid);
        });
        //异常回调
        sseEmitter.onError(throwable -> {
                    try {
                        log.error("[{}]连接异常,{}", uid, throwable.toString());
                        sseEmitter.send(SseEmitter.event()
                                .id(uid)
                                .name("发生异常！")
                                .data("发生异常请重试！")
                                .reconnectTime(3000));
                        sseMap.put(uid, sseEmitter);
                        runningMap.put(uid, true);
                    } catch (IOException e) {
                        log.error("sse发送重连event时发生异常：", e);
                    }
                }
        );
        try {
            sseEmitter.send(SseEmitter.event().reconnectTime(5000));
        } catch (IOException e) {
            log.error("sse发送event时发生异常：", e);
        }
        sseMap.put(uid, sseEmitter);
        runningMap.put(uid, true);
        log.info("[{}]创建sse连接成功！", uid);
        return sseEmitter;
    }

    /**
     * 给指定用户发送消息
     */
    public boolean sendMessage(String uid, String messageId, String message) {
        if (StringUtils.isBlank(message)) {
            log.warn("参数异常uid:[{}]，msg为null", uid);
            return false;
        }
        SseEmitter sseEmitter = sseMap.get(uid);
        if (sseEmitter == null) {
            log.warn("消息推送失败uid:[{}],没有创建连接，请重试。", uid);
            return false;
        }
        try {
            sseEmitter.send(SseEmitter.event().id(messageId).reconnectTime(60 * 1000L).data(message));
            log.info("用户{},消息id:{},推送成功:{}", uid, messageId, message);
            return true;
        } catch (Exception e) {
            sseMap.remove(uid);
            runningMap.remove(uid);
            log.error("用户{},消息id:{},推送异常:{}", uid, messageId, e.getMessage());
            sseEmitter.complete();
            return false;
        }
    }

    /**
     * 断开
     *
     * @param uid
     */
    public void closeSse(String uid) {
        if (sseMap.containsKey(uid)) {
            SseEmitter sseEmitter = sseMap.get(uid);
            sseEmitter.complete();
            sseMap.remove(uid);
            runningMap.remove(uid);
        } else {
            log.info("用户{} 连接已关闭", uid);
        }
    }


}

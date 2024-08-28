package org.example.chatdemo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatdemo.component.SseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SseClient sseClient;

    @GetMapping(value = "/createSse")
    public SseEmitter createSse(@RequestParam String myAccount, @RequestParam(required = false) String targetAccount) {
        sseClient.sendMessage(targetAccount, UUID.randomUUID().toString(), String.format("用户[%s]上线了，我们开始聊天吧", myAccount));
        return sseClient.createSee(myAccount);
    }


    @GetMapping(value = "/send", produces = "text/event-stream")
    public String sendMsg(@RequestParam String msg, @RequestParam String targetAccount) {
        sseClient.sendMessage(targetAccount, UUID.randomUUID().toString(), msg);
        return "ok";
    }

}

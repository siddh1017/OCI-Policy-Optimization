package com.springAI.controller;

import com.springAI.tools.OciDataFetchTool;
import com.springAI.tools.OciPolicyOptimizerTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OciDataFetchTool ociDataFetchTool;
    private final OciPolicyOptimizerTool ociPolicyOptimizerTool;
    private final ChatClient ollamaChatClient;
    private final ChatClient openAiChatClient;

    @Value("classpath:/prompts/system-prompt.st")
    private Resource systemMessage;

    ChatController(
            OciDataFetchTool ociDataFetchTool, OciPolicyOptimizerTool ociPolicyOptimizerTool,
            @Qualifier("ollamaChatClient") ChatClient ollamaChatClient,
            @Qualifier("openAiChatClient") ChatClient openAiChatClient) {
        this.ociDataFetchTool = ociDataFetchTool;
        this.ociPolicyOptimizerTool = ociPolicyOptimizerTool;
        this.ollamaChatClient = ollamaChatClient;
        this.openAiChatClient = openAiChatClient;
    }

    @GetMapping
    public ResponseEntity<Flux<String>> askModel(
            @RequestParam(value = "q", defaultValue = "Hi") String query) {
        System.out.println(query);
        Flux<String> modelResponse = openAiChatClient
                .prompt(query)
                .system(systemMessage)
                .tools(ociDataFetchTool, ociPolicyOptimizerTool)
                .stream()
                .content();

        System.out.println(modelResponse);
        return ResponseEntity.ok(modelResponse);
    }
}

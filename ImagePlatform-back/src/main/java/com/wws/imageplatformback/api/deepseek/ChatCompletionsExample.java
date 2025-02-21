package com.wws.imageplatformback.api.deepseek;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import com.wws.imageplatformback.api.deepseek.model.DeepSeekResultResponse;
import java.util.ArrayList;
import java.util.List;

/** 这是一个示例类，展示了如何使用ArkService来完成聊天功能。 */
public class ChatCompletionsExample {

  public static void main(String[] args) {
    ChatCompletionsExample example = new ChatCompletionsExample();
    System.out.println(example.getDeepSeekResult("你好"));
  }

  public DeepSeekResultResponse getDeepSeekResult(String search) {
    // 从环境变量中获取API密钥
    String apiKey = "apiKey";
    // 创建ArkService实例
    ArkService arkService = ArkService.builder().apiKey(apiKey).build();
    // 初始化消息列表
    List<ChatMessage> chatMessages = new ArrayList<>();
    // 创建用户消息
    ChatMessage userMessage =
        ChatMessage.builder()
            .role(ChatMessageRole.USER) // 设置消息角色为用户
            .content(search) // 设置消息内容
            .build();
    // 将用户消息添加到消息列表
    chatMessages.add(userMessage);
    // 创建聊天完成请求
    ChatCompletionRequest chatCompletionRequest =
        ChatCompletionRequest.builder()
            .model("deepseek-r1-250120") // 需要替换为模型的Model ID
            .messages(chatMessages) // 设置消息列表
            .build();
    // 发送聊天完成请求并打印响应
    DeepSeekResultResponse deepSeekResultResponse = new DeepSeekResultResponse();
    deepSeekResultResponse.setReasoningContent("");
    deepSeekResultResponse.setContent("");
    try {
      // 获取响应并打印每个选择的消息内容
      arkService
          .streamChatCompletion(chatCompletionRequest)
          .doOnError(Throwable::printStackTrace)
          .blockingForEach(
              choice -> {
                if (choice.getChoices().size() > 0) {
                  String preReasoningContent = "";
                  String preContent = "";
                  ChatMessage message = choice.getChoices().get(0).getMessage();
                  // 判断是否触发深度推理，触发则打印模型输出的思维链内容
                  if (message.getReasoningContent() != null
                      && !message.getReasoningContent().isEmpty()) {
                    // System.out.print(message.getReasoningContent());
                    String reasoningContent =
                        deepSeekResultResponse.getReasoningContent()
                            + message.getReasoningContent();
                    deepSeekResultResponse.setReasoningContent(reasoningContent);
                  }
                  String content = deepSeekResultResponse.getContent() + message.getContent();
                  deepSeekResultResponse.setContent(content);
                  // 打印模型输出的回答内容
                  // System.out.print(message.getContent());
                }
              });
    } catch (Exception e) {
      System.out.println("请求失败: " + e.getMessage());
    } finally {
      // 关闭服务执行器
      arkService.shutdownExecutor();
    }
    return deepSeekResultResponse;
  }
}

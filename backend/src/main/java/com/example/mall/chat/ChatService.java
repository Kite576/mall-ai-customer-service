package com.example.mall.chat;

import com.example.mall.order.Order;
import com.example.mall.order.OrderService;
import com.example.mall.order.ShipmentTracking;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            你是一个专业、友好、高效的电商平台智能客服，名字叫“小智”。
            你的任务是回答用户关于购物的问题，包括商品咨询、订单查询、物流信息、退换货政策等。
            你只能回答与电商购物相关的内容，对于不相关的问题请礼貌拒绝并引导回购物主题。
            回复要简洁、分段，使用“亲”等礼貌用语。
            不要编造任何信息，不确定时请让用户提供订单号或联系人工客服。

            【商品知识库】
            1. 无线蓝牙耳机 Pro - 价格299元，规格：续航8小时，主动降噪，蓝牙5.3，有货。
            2. 便携充电宝 20000mAh - 价格159元，规格：65W快充，可上飞机，含Type-C线，剩余100件。
            3. 智能手表 S3 - 价格899元，规格：血氧检测，GPS定位，心率监测，1.8寸AMOLED屏，有货。

            【售后政策】
            - 7天无理由退换（保持商品完好）
            - 15天质量问题免费退换
            - 1年质保
            """;

    private final String apiKey;
    private final String model;
    private final WebClient webClient;
    private final OrderService orderService;
    private final Map<String, List<ChatMessage>> sessions = new ConcurrentHashMap<>();

    public ChatService(@Value("${mall.ai.api-url}") String apiUrl,
                       @Value("${mall.ai.api-key}") String apiKey,
                       @Value("${mall.ai.model}") String model,
                       WebClient.Builder webClientBuilder,
                       OrderService orderService) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = webClientBuilder.baseUrl(apiUrl).build();
        this.orderService = orderService;
    }

    public ChatResponse send(ChatRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        String enrichedMessage = enrichWithOrderContext(request.message());
        List<ChatMessage> messages = buildMessages(sessionId, enrichedMessage);
        String reply = callAi(messages, false);
        saveHistory(sessionId, request.message(), reply);
        return new ChatResponse(sessionId, reply);
    }

    public Flux<String> stream(ChatRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        ChatResponse response = send(new ChatRequest(request.message(), sessionId));
        return Flux.fromArray(response.reply().split(""));
    }

    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    private List<ChatMessage> buildMessages(String sessionId, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        List<ChatMessage> history = sessions.getOrDefault(sessionId, List.of());
        int fromIndex = Math.max(0, history.size() - 10);
        messages.addAll(history.subList(fromIndex, history.size()));
        messages.add(new ChatMessage("user", userMessage));
        return messages;
    }

    private void saveHistory(String sessionId, String userMessage, String reply) {
        List<ChatMessage> history = sessions.computeIfAbsent(sessionId, key -> new ArrayList<>());
        history.add(new ChatMessage("user", userMessage));
        history.add(new ChatMessage("assistant", reply));
    }

    private String callAi(List<ChatMessage> messages, boolean stream) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackReply(messages.get(messages.size() - 1).content());
        }
        AiChatRequest request = new AiChatRequest(model, messages, 0.3, 1000, stream);
        AiChatResponse response = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(apiKey))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiChatResponse.class)
                .block();
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return "亲，暂时没有获取到回复，建议稍后再试或联系人工客服。";
        }
        return response.choices().get(0).message().content();
    }

    private String enrichWithOrderContext(String message) {
        String orderNo = extractOrderNo(message);
        if (orderNo == null) {
            return message;
        }
        try {
            Order order = orderService.detailByOrderNo(orderNo);
            List<ShipmentTracking> trackingList = orderService.tracking(order.getId());
            return message + "\n\n【系统查询到的订单信息】订单号：" + order.getOrderNo()
                    + "，状态：" + order.getStatus()
                    + "，实付金额：" + order.getTotalAmount()
                    + "，物流节点：" + trackingList.stream().map(ShipmentTracking::getDescription).toList();
        } catch (IllegalArgumentException exception) {
            return message + "\n\n【系统提示】未查询到该订单号，请引导用户核对订单号。";
        }
    }

    private String extractOrderNo(String message) {
        for (String token : message.split("[\\s，。,.：:]+")) {
            if (token.startsWith("MO") && token.length() >= 10) {
                return token;
            }
        }
        return null;
    }

    private String fallbackReply(String message) {
        if (message.contains("耳机")) {
            return "亲，无线蓝牙耳机 Pro 价格是299元。\n支持主动降噪、蓝牙5.3，续航约8小时，目前有货。";
        }
        if (message.contains("充电宝")) {
            return "亲，便携充电宝 20000mAh 价格是159元。\n支持65W快充，可上飞机，含Type-C充电线，目前剩余100件。";
        }
        if (message.contains("手表")) {
            return "亲，智能手表 S3 价格是899元。\n支持血氧检测、GPS定位、心率监测，配备1.8寸AMOLED屏，目前有货。";
        }
        if (message.contains("退") || message.contains("换") || message.contains("售后")) {
            return "亲，平台支持7天无理由退换货，15天内质量问题免费退换，质量问题享1年质保。\n如需申请售后，请提供订单号或商品名称，我帮您继续处理。";
        }
        if (message.contains("订单") || message.contains("物流")) {
            return "亲，请您提供订单号，我帮您查询订单状态和物流进度。";
        }
        return "亲，这个问题我需要核实一下，请您提供订单号/商品ID，我帮您查询确认。";
    }
}

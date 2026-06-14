package com.example.mall.chat;

import com.example.mall.order.Order;
import com.example.mall.order.OrderItem;
import com.example.mall.order.OrderService;
import com.example.mall.order.ShipmentTracking;
import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            你是电商购物平台的智能客服“小智”，正在和真实顾客进行在线对话。

            工作方式：
            1. 需要商品、价格、库存、规格、补贴、订单或物流信息时，必须先调用工具查询后端数据。
            2. 商品推荐必须基于 search_products 或 list_subsidy_products 的结果，不能编造不存在的商品、价格、库存或优惠。
            3. 用户描述需求时，例如“预算 200 左右的蓝牙耳机”“办公用电脑”“有国补的家电”，你要主动检索匹配商品，再给出对比和建议。
            4. 如果工具没有查到合适数据，要直接说明当前没查到，并询问用户是否放宽预算、品类或规格。
            5. 订单、物流和售后必须优先使用工具返回的数据；缺少订单号时请用户补充订单号。
            6. 必须使用中文，语气像真实客服，简洁、明确、可执行。
            7. 不要输出复杂 Markdown，不要输出广告话术，不要强行推荐无关商品。

            平台售后规则：
            - 商品完整时支持 7 天无理由退换货。
            - 15 天内质量问题可免费退换。
            - 质量问题享 1 年质保。
            - 申请售后通常需要订单号、商品状态、退换原因和商品/配件/包装完整情况。

            国补/补贴规则：
            - 带有“国补”“国补贴”“国家补贴”“以旧换新”标签的商品可参与补贴活动。
            - 是否可用和具体补贴金额以结算页为准。
            - 不同地区、品类、库存和用户资格可能不同。
            """;

    private static final String AFTER_SALE_POLICY = """
            商品完整时支持 7 天无理由退换货。
            15 天内质量问题可免费退换。
            质量问题享 1 年质保。
            申请售后通常需要订单号、商品状态、退换原因和商品/配件/包装完整情况。
            """;

    private static final String SUBSIDY_POLICY = """
            带有“国补”“国补贴”“国家补贴”“以旧换新”标签的商品可参与补贴活动。
            是否可用和具体补贴金额以结算页为准。
            不同地区、品类、库存和用户资格可能不同。
            """;

    private final String apiKey;
    private final String model;
    private final WebClient webClient;
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, List<ChatMessage>> sessions = new ConcurrentHashMap<>();

    public ChatService(@Value("${mall.ai.api-url}") String apiUrl,
                       @Value("${mall.ai.api-key}") String apiKey,
                       @Value("${mall.ai.model}") String model,
                       WebClient.Builder webClientBuilder,
                       OrderService orderService,
                       ProductRepository productRepository,
                       ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = webClientBuilder.baseUrl(apiUrl).build();
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    public ChatResponse send(ChatRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        List<ChatMessage> messages = buildMessages(sessionId, request);
        String reply = callAiWithTools(messages);
        saveHistory(sessionId, request.message(), reply);
        return new ChatResponse(sessionId, reply);
    }

    public SseEmitter stream(ChatRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        List<ChatMessage> messages = buildMessages(sessionId, request);
        SseEmitter emitter = new SseEmitter(60_000L);
        Thread worker = new Thread(() -> {
            String reply = callAiWithTools(messages);
            saveHistory(sessionId, request.message(), reply);
            streamText(emitter, reply);
        });
        worker.setDaemon(true);
        worker.start();
        return emitter;
    }

    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }

    private String callAiWithTools(List<ChatMessage> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackReply(messages);
        }
        List<ChatMessage> conversation = new ArrayList<>(messages);
        for (int round = 0; round < 3; round++) {
            AiChatResponse response = callDeepSeek(conversation, true);
            ChatMessage assistantMessage = firstMessage(response);
            if (assistantMessage == null) {
                return fallbackReply(messages);
            }
            List<ChatMessage.ToolCall> toolCalls = assistantMessage.toolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                return blankToFallback(assistantMessage.content(), messages);
            }
            conversation.add(assistantMessage);
            for (ChatMessage.ToolCall toolCall : toolCalls) {
                conversation.add(ChatMessage.toolResult(toolCall.id(), executeTool(toolCall)));
            }
        }

        AiChatResponse finalResponse = callDeepSeek(conversation, false);
        ChatMessage finalMessage = firstMessage(finalResponse);
        if (finalMessage == null) {
            return fallbackReply(messages);
        }
        return blankToFallback(finalMessage.content(), messages);
    }

    private AiChatResponse callDeepSeek(List<ChatMessage> messages, boolean enableTools) {
        AiChatRequest request = new AiChatRequest(
                model,
                messages,
                0.2,
                1200,
                false,
                enableTools ? tools() : null,
                enableTools ? "auto" : null);
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(apiKey))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiChatResponse.class)
                .onErrorReturn(new AiChatResponse(List.of()))
                .block();
    }

    private ChatMessage firstMessage(AiChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return null;
        }
        return response.choices().get(0).message();
    }

    private String blankToFallback(String reply, List<ChatMessage> messages) {
        if (reply == null || reply.isBlank()) {
            return fallbackReply(messages);
        }
        return reply.trim();
    }

    private List<AiChatRequest.Tool> tools() {
        return List.of(
                functionTool(
                        "search_products",
                        "按顾客自然语言需求搜索平台商品。支持品类、关键词、预算、国补、库存等条件。",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query", Map.of("type", "string", "description", "顾客原始需求或提炼后的商品需求"),
                                        "category", Map.of("type", "string", "description", "商品品类，例如耳机、电脑办公、智能家居，可为空"),
                                        "max_price", Map.of("type", "number", "description", "顾客可接受的最高价格，可为空"),
                                        "only_in_stock", Map.of("type", "boolean", "description", "是否只返回有库存商品"),
                                        "need_subsidy", Map.of("type", "boolean", "description", "是否只返回带国补/补贴标签商品")),
                                "required", List.of("query"))),
                functionTool(
                        "list_subsidy_products",
                        "查询当前平台所有带国补、补贴或以旧换新标签的商品。",
                        Map.of("type", "object", "properties", Map.of())),
                functionTool(
                        "get_order_detail",
                        "根据订单号查询订单、商品明细和物流轨迹。",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "order_no", Map.of("type", "string", "description", "订单号，例如 MO202605290001")),
                                "required", List.of("order_no"))),
                functionTool(
                        "get_after_sale_policy",
                        "查询平台退货、换货、退款、质保等售后政策。",
                        Map.of("type", "object", "properties", Map.of()))
        );
    }

    private AiChatRequest.Tool functionTool(String name, String description, Map<String, Object> parameters) {
        return new AiChatRequest.Tool("function", new AiChatRequest.Function(name, description, parameters));
    }

    private String executeTool(ChatMessage.ToolCall toolCall) {
        String name = toolCall.function() == null ? "" : toolCall.function().name();
        JsonNode arguments = parseArguments(toolCall.function() == null ? "{}" : toolCall.function().arguments());
        Map<String, Object> result = switch (name) {
            case "search_products" -> searchProductsTool(arguments);
            case "list_subsidy_products" -> listSubsidyProductsTool();
            case "get_order_detail" -> getOrderDetailTool(arguments.path("order_no").asText(""));
            case "get_after_sale_policy" -> Map.of("policy", AFTER_SALE_POLICY.strip());
            default -> Map.of("error", "未知工具：" + name);
        };
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"工具结果序列化失败\"}";
        }
    }

    private JsonNode parseArguments(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private Map<String, Object> searchProductsTool(JsonNode arguments) {
        String query = arguments.path("query").asText("");
        String category = arguments.path("category").asText("");
        BigDecimal maxPrice = arguments.hasNonNull("max_price")
                ? BigDecimal.valueOf(arguments.path("max_price").asDouble())
                : null;
        boolean onlyInStock = !arguments.has("only_in_stock") || arguments.path("only_in_stock").asBoolean(true);
        boolean needSubsidy = arguments.path("need_subsidy").asBoolean(false);
        boolean broadCatalogQuestion = isBroadCatalogQuestion(query) && category.isBlank() && maxPrice == null && !needSubsidy;

        List<ProductScore> scoredProducts = productRepository.findAll().stream()
                .filter(this::isActive)
                .filter(product -> !onlyInStock || product.getStock() != null && product.getStock() > 0)
                .filter(product -> maxPrice == null || product.getPrice().compareTo(maxPrice) <= 0)
                .filter(product -> !needSubsidy || hasSubsidyTag(product))
                .filter(product -> matchesRequiredCategory(product, query + " " + category))
                .map(product -> new ProductScore(product, scoreProduct(product, query, category, needSubsidy)))
                .filter(score -> score.score() > 0 || broadCatalogQuestion)
                .sorted(Comparator.comparingInt(ProductScore::score).reversed()
                        .thenComparing(score -> score.product().getPrice()))
                .limit(8)
                .toList();

        List<Product> products = scoredProducts.stream().map(ProductScore::product).toList();
        return Map.of(
                "query", query,
                "matched_count", products.size(),
                "products", products.stream().map(this::productMap).toList(),
                "note", products.isEmpty() ? "当前没有查到符合条件的商品。" : "价格、库存和标签来自平台数据库。");
    }

    private Map<String, Object> listSubsidyProductsTool() {
        List<Product> products = productRepository.findAll().stream()
                .filter(this::isActive)
                .filter(this::hasSubsidyTag)
                .sorted(Comparator.comparing(Product::getId))
                .toList();
        return Map.of(
                "policy", SUBSIDY_POLICY.strip(),
                "matched_count", products.size(),
                "products", products.stream().map(this::productMap).toList());
    }

    private Map<String, Object> getOrderDetailTool(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Map.of("error", "缺少订单号");
        }
        try {
            Order order = orderService.detailByOrderNo(orderNo);
            List<ShipmentTracking> trackingList = orderService.tracking(order.getId());
            return Map.of(
                    "order_no", order.getOrderNo(),
                    "status", statusLabel(String.valueOf(order.getStatus())),
                    "total_amount", order.getTotalAmount(),
                    "items", order.getItems().stream().map(this::orderItemMap).toList(),
                    "tracking", trackingList.stream().map(this::trackingMap).toList());
        } catch (IllegalArgumentException exception) {
            return Map.of("error", "没有查询到该订单，请用户核对订单号。", "order_no", orderNo);
        }
    }

    private Map<String, Object> productMap(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("price", product.getPrice());
        map.put("stock", product.getStock());
        map.put("specs", nullToEmpty(product.getSpecs()));
        map.put("tags", splitTags(product.getTags()));
        map.put("has_subsidy", hasSubsidyTag(product));
        return map;
    }

    private Map<String, Object> orderItemMap(OrderItem item) {
        return Map.of(
                "product_id", item.getProductId(),
                "product_name", item.getProductName(),
                "price", item.getPrice(),
                "quantity", item.getQuantity());
    }

    private Map<String, Object> trackingMap(ShipmentTracking tracking) {
        return Map.of(
                "status", tracking.getStatus(),
                "description", tracking.getDescription(),
                "occur_time", tracking.getOccurTime());
    }

    private int scoreProduct(Product product, String query, String category, boolean needSubsidy) {
        String normalizedQuery = normalize(query + " " + category);
        String searchable = normalize(String.join(" ",
                nullToEmpty(product.getName()),
                nullToEmpty(product.getSpecs()),
                nullToEmpty(product.getTags())));
        Set<String> keywords = extractKeywords(normalizedQuery);
        int score = 0;
        for (String keyword : keywords) {
            if (searchable.contains(keyword)) {
                score += keyword.length() >= 3 ? 4 : 2;
            }
        }
        for (String synonym : expandQuerySynonyms(normalizedQuery)) {
            if (searchable.contains(synonym)) {
                score += 3;
            }
        }
        if (needSubsidy && hasSubsidyTag(product)) {
            score += 5;
        }
        return score;
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : text.split("[\\s,，.。:：;；!！?？、/]+")) {
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }
        for (String keyword : List.of("耳机", "蓝牙", "降噪", "运动", "充电", "充电宝", "移动电源", "电脑", "笔记本",
                "办公", "显示器", "键盘", "鼠标", "手表", "手环", "穿戴", "家电", "家居", "扫地", "门锁", "台灯",
                "音箱", "手机", "保护壳", "钢化膜", "数据线", "相机", "国补", "补贴", "以旧换新", "便宜", "低价")) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }

    private Set<String> expandQuerySynonyms(String text) {
        Set<String> synonyms = new LinkedHashSet<>();
        Map<String, List<String>> dictionary = Map.ofEntries(
                Map.entry("耳机", List.of("耳机", "蓝牙", "降噪", "运动")),
                Map.entry("蓝牙", List.of("耳机", "蓝牙")),
                Map.entry("充电", List.of("充电", "充电宝", "移动电源", "数据线", "无线充")),
                Map.entry("电脑", List.of("电脑", "笔记本", "显示器", "键盘", "鼠标", "办公")),
                Map.entry("办公", List.of("办公", "笔记本", "显示器", "键盘", "鼠标")),
                Map.entry("手机", List.of("手机", "保护壳", "钢化膜", "数据线")),
                Map.entry("穿戴", List.of("手表", "手环", "穿戴", "健康")),
                Map.entry("手表", List.of("手表", "手环", "穿戴", "健康")),
                Map.entry("家电", List.of("扫地机器人", "智能门锁", "智能音箱", "台灯", "智能家居")),
                Map.entry("家居", List.of("扫地机器人", "智能门锁", "智能音箱", "台灯", "智能家居")),
                Map.entry("国补", List.of("国补", "国补贴", "国家补贴", "补贴", "以旧换新")),
                Map.entry("补贴", List.of("国补", "国补贴", "国家补贴", "补贴", "以旧换新"))
        );
        for (Map.Entry<String, List<String>> entry : dictionary.entrySet()) {
            if (text.contains(entry.getKey())) {
                synonyms.addAll(entry.getValue().stream().map(this::normalize).toList());
            }
        }
        return synonyms;
    }

    private boolean matchesRequiredCategory(Product product, String query) {
        String text = normalize(query);
        String searchable = normalize(String.join(" ",
                nullToEmpty(product.getName()),
                nullToEmpty(product.getSpecs()),
                nullToEmpty(product.getTags())));
        if (text.contains("耳机")) {
            return searchable.contains("耳机");
        }
        if (text.contains("充电宝") || text.contains("移动电源")) {
            return searchable.contains("充电宝") || searchable.contains("移动电源");
        }
        if (text.contains("笔记本")) {
            return searchable.contains("笔记本");
        }
        if (text.contains("显示器")) {
            return searchable.contains("显示器");
        }
        if (text.contains("鼠标")) {
            return searchable.contains("鼠标");
        }
        if (text.contains("键盘")) {
            return searchable.contains("键盘");
        }
        if (text.contains("手表")) {
            return searchable.contains("手表");
        }
        if (text.contains("手环")) {
            return searchable.contains("手环");
        }
        if (text.contains("扫地")) {
            return searchable.contains("扫地");
        }
        if (text.contains("门锁")) {
            return searchable.contains("门锁");
        }
        if (text.contains("台灯")) {
            return searchable.contains("台灯");
        }
        if (text.contains("音箱")) {
            return searchable.contains("音箱");
        }
        if (text.contains("相机")) {
            return searchable.contains("相机");
        }
        return true;
    }

    private String fallbackReply(List<ChatMessage> messages) {
        String message = latestUserText(messages);
        String orderNo = extractOrderNo(message);
        if (hasLogisticsIntent(message) || orderNo != null) {
            if (orderNo == null) {
                return "请提供订单号，我会帮您查看发货状态、最新物流节点和预计送达时间。";
            }
            Map<String, Object> order = getOrderDetailTool(orderNo);
            if (order.containsKey("error")) {
                return "没有查询到订单 " + orderNo + "，请核对订单号是否完整。";
            }
            return "已查到订单 " + orderNo + "，当前状态：" + order.get("status")
                    + "，实付金额：" + order.get("total_amount") + "元。物流信息：" + order.get("tracking");
        }
        if (hasAfterSaleIntent(message)) {
            return "平台支持7天无理由退换货，15天内质量问题免费退换，质量问题享1年质保。如需申请售后，请提供订单号、商品状态和退换原因。";
        }

        ObjectNode args = objectMapper.createObjectNode()
                .put("query", message)
                .put("only_in_stock", true);
        BigDecimal maxPrice = extractMaxPrice(message);
        if (maxPrice != null) {
            args.put("max_price", maxPrice);
        }
        if (hasSubsidyIntent(message)) {
            args.put("need_subsidy", true);
        }
        Map<String, Object> result = hasSubsidyIntent(message) ? listSubsidyProductsTool() : searchProductsTool(args);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
        if (products == null || products.isEmpty()) {
            return "当前没有查到完全匹配的商品。您可以补充预算、品类、用途或关键规格，我再帮您筛选。";
        }
        return "我查到这些商品比较符合：\n" + products.stream()
                .limit(5)
                .map(product -> product.get("name") + "，价格" + product.get("price") + "元，库存"
                        + product.get("stock") + "，规格：" + product.get("specs"))
                .collect(Collectors.joining("\n"));
    }

    private String latestUserText(List<ChatMessage> messages) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if ("user".equals(message.role()) && message.content() != null) {
                return message.content();
            }
        }
        return "";
    }

    private List<ChatMessage> buildMessages(String sessionId, ChatRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        List<ChatMessage> context = sanitizeContext(request.context());
        if (context.isEmpty()) {
            List<ChatMessage> history = sessions.getOrDefault(sessionId, List.of());
            int fromIndex = Math.max(0, history.size() - 10);
            messages.addAll(history.subList(fromIndex, history.size()));
        } else {
            messages.addAll(context);
        }
        messages.add(new ChatMessage("user", request.message()));
        return messages;
    }

    private List<ChatMessage> sanitizeContext(List<ChatMessage> context) {
        if (context == null || context.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(0, context.size() - 10);
        return context.subList(fromIndex, context.size()).stream()
                .filter(message -> message != null
                        && ("user".equals(message.role()) || "assistant".equals(message.role()))
                        && message.content() != null
                        && !message.content().isBlank())
                .map(message -> new ChatMessage(message.role(), limitText(message.content(), 800)))
                .toList();
    }

    private void streamText(SseEmitter emitter, String reply) {
        try {
            for (int offset = 0; offset < reply.length(); ) {
                int codePoint = reply.codePointAt(offset);
                emitter.send(SseEmitter.event().data(new String(Character.toChars(codePoint))));
                offset += Character.charCount(codePoint);
                Thread.sleep(14L);
            }
            safeComplete(emitter);
        } catch (IOException | IllegalStateException exception) {
            safeComplete(emitter);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            safeComplete(emitter);
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException exception) {
            // Client may have already closed the SSE connection.
        }
    }

    private void saveHistory(String sessionId, String userMessage, String reply) {
        List<ChatMessage> history = sessions.computeIfAbsent(sessionId, key -> new ArrayList<>());
        history.add(new ChatMessage("user", userMessage));
        history.add(new ChatMessage("assistant", reply));
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    private String limitText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private boolean isActive(Product product) {
        return product.getStatus() != null && product.getStatus() == 1;
    }

    private boolean hasSubsidyTag(Product product) {
        return mentions(product.getTags(), "国补", "国补贴", "国家补贴", "补贴", "以旧换新");
    }

    private boolean isGeneralProductQuestion(String message) {
        return mentions(message, "商品", "产品", "推荐", "买", "哪些", "有什么", "多少钱", "有货", "适合", "预算", "左右", "以内");
    }

    private boolean isBroadCatalogQuestion(String message) {
        return mentions(message, "所有商品", "全部商品", "有什么商品", "有哪些商品", "商品列表")
                && !mentions(message, "耳机", "蓝牙", "充电", "电脑", "笔记本", "办公", "手机", "手表", "手环",
                "穿戴", "家电", "家居", "扫地", "门锁", "台灯", "音箱", "相机", "国补", "补贴");
    }

    private boolean hasAfterSaleIntent(String text) {
        return mentions(text, "退货", "退款", "退换", "换货", "售后", "不要了", "退掉", "return", "refund");
    }

    private boolean hasLogisticsIntent(String text) {
        return mentions(text, "物流", "快递", "配送", "运输", "到哪", "到哪里", "查订单", "查一下订单", "tracking", "shipping", "delivery");
    }

    private boolean hasSubsidyIntent(String text) {
        return mentions(text, "国补", "国家补贴", "补贴", "以旧换新", "subsidy");
    }

    private String extractOrderNo(String message) {
        for (String token : message.split("[\\s,，.。:：;；!！?？、]+")) {
            if (token.startsWith("MO") && token.length() >= 10) {
                return token;
            }
        }
        return null;
    }

    private BigDecimal extractMaxPrice(String message) {
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:\\s*)(?:元|块|以内|以下|之内|左右|预算)").matcher(message);
        if (matcher.find() && mentions(message, "预算", "以内", "以下", "之内", "不超过", "低于", "便宜", "左右")) {
            return new BigDecimal(matcher.group(1));
        }
        matcher = Pattern.compile("(?:预算|不超过|低于|小于|控制在)(?:\\s*)(\\d+(?:\\.\\d+)?)").matcher(message);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return null;
    }

    private boolean mentions(String source, String... keywords) {
        String text = source == null ? "" : source.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String source) {
        return source == null ? "" : source.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split("[,，、]+"));
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PENDING_PAYMENT" -> "待付款";
            case "PENDING_SHIPMENT" -> "待发货";
            case "SHIPPED" -> "已发货";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private record ProductScore(Product product, int score) {
    }
}

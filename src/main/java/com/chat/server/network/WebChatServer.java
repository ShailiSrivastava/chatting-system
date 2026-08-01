package com.chat.server.network;

import com.chat.common.model.*;
import com.chat.common.protocol.Packet;
import com.chat.common.protocol.PacketFactory;
import com.chat.common.protocol.PacketType;
import com.chat.common.util.LoggerUtil;
import com.chat.server.service.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WebChatServer {

    private HttpServer httpServer;

    private final AuthService authService = new AuthService();
    private final UserService userService = new UserService();
    private final MessageService messageService = new MessageService();
    private final GroupService groupService = new GroupService();

    private static final Map<Long, Map<String, Integer>> messageReactions = new HashMap<>();
    private static final List<String> systemActivityLogs = new ArrayList<>();

    private int getEffectivePort() {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }

    public void start() {
        int webPort = getEffectivePort();
        try {
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", webPort), 0);

            httpServer.createContext("/", new StaticWebHandler());
            httpServer.createContext("/api/register", new RegisterHandler());
            httpServer.createContext("/api/login", new LoginHandler());
            httpServer.createContext("/api/users", new UsersHandler());
            httpServer.createContext("/api/messages", new MessagesHandler());
            httpServer.createContext("/api/groups", new GroupsHandler());
            httpServer.createContext("/api/react", new ReactionHandler());
            httpServer.createContext("/api/metrics", new MetricsHandler());
            httpServer.createContext("/api/logs", new SystemLogsHandler());

            httpServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            httpServer.start();

            logActivity("WebChatServer started on http://0.0.0.0:" + webPort);

            LoggerUtil.info("==================================================");
            LoggerUtil.info("   EXTRAORDINARY INDUSTRIAL PLATFORM ACTIVE       ");
            LoggerUtil.info("   Web Listening: http://0.0.0.0:" + webPort + "/   ");
            LoggerUtil.info("==================================================");
        } catch (IOException e) {
            LoggerUtil.error("Failed to start WebChatServer on port " + webPort, e);
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private static synchronized void logActivity(String msg) {
        String logEntry = String.format("[%tF %tT] %s", new Date(), new Date(), msg);
        systemActivityLogs.add(logEntry);
        if (systemActivityLogs.size() > 50) {
            systemActivityLogs.remove(0);
        }
    }

    private class StaticWebHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = getUltimateWebUIHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                Map<String, String> params = parseJsonSimple(body);
                try {
                    User u = authService.register(params.get("username"), params.get("email"), params.get("password"));
                    logActivity("Registered user: " + u.getUsername());
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Registered successfully!\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                Map<String, String> params = parseJsonSimple(body);
                try {
                    String token = authService.login(params.get("usernameOrEmail"), params.get("password"));
                    User u = authService.validateSession(token);
                    logActivity("User logged in: " + u.getUsername() + " via Web API");
                    String resp = String.format("{\"success\":true,\"token\":\"%s\",\"user\":{\"id\":%d,\"username\":\"%s\",\"email\":\"%s\"}}",
                            token, u.getId(), escapeJson(u.getUsername()), escapeJson(u.getEmail()));
                    sendJsonResponse(exchange, 200, resp);
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<User> users = userService.getAllUsers();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                boolean online = ClientManager.getInstance().isUserOnline(u.getId());
                sb.append(String.format("{\"id\":%d,\"username\":\"%s\",\"email\":\"%s\",\"status\":\"%s\",\"bio\":\"%s\"}",
                        u.getId(), escapeJson(u.getUsername()), escapeJson(u.getEmail()), online ? "ONLINE" : "OFFLINE", escapeJson(u.getBio())));
                if (i < users.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class MessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> qp = parseQueryParams(query);
                Long u1 = qp.containsKey("u1") ? Long.parseLong(qp.get("u1")) : 0L;
                Long u2 = qp.containsKey("u2") ? Long.parseLong(qp.get("u2")) : 0L;
                Long gId = qp.containsKey("gId") ? Long.parseLong(qp.get("gId")) : 0L;

                List<Message> history;
                if (gId > 0) {
                    history = messageService.getGroupChatHistory(gId);
                } else {
                    history = messageService.getPrivateChatHistory(u1, u2);
                }

                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < history.size(); i++) {
                    Message m = history.get(i);
                    Map<String, Integer> reactMap = messageReactions.getOrDefault(m.getId(), Collections.emptyMap());
                    StringBuilder rJson = new StringBuilder("{");
                    int rc = 0;
                    for (Map.Entry<String, Integer> entry : reactMap.entrySet()) {
                        rJson.append(String.format("\"%s\":%d", entry.getKey(), entry.getValue()));
                        if (++rc < reactMap.size()) rJson.append(",");
                    }
                    rJson.append("}");

                    sb.append(String.format("{\"id\":%d,\"senderId\":%d,\"senderUsername\":\"%s\",\"content\":\"%s\",\"status\":\"%s\",\"messageType\":\"%s\",\"isEncrypted\":%b,\"timestamp\":\"%s\",\"reactions\":%s}",
                            m.getId(), m.getSenderId(), escapeJson(m.getSenderUsername()), escapeJson(m.getContent()), m.getStatus().name(), m.getMessageType().name(), m.isEncrypted(), m.getTimestamp(), rJson));
                    if (i < history.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(exchange);
                Map<String, String> params = parseJsonSimple(body);

                Message msg = new Message();
                msg.setSenderId(Long.parseLong(params.get("senderId")));
                msg.setSenderUsername(params.get("senderUsername"));
                if (params.containsKey("receiverId") && !params.get("receiverId").isEmpty()) {
                    msg.setReceiverId(Long.parseLong(params.get("receiverId")));
                }
                if (params.containsKey("groupId") && !params.get("groupId").isEmpty()) {
                    msg.setGroupId(Long.parseLong(params.get("groupId")));
                }
                msg.setContent(params.get("content"));
                msg.setEncrypted(Boolean.parseBoolean(params.getOrDefault("isEncrypted", "false")));
                msg.setMessageType(params.containsKey("isAudio") && Boolean.parseBoolean(params.get("isAudio")) ? MessageType.FILE : MessageType.TEXT);

                Message saved = messageService.sendMessage(msg);
                if (saved != null) {
                    logActivity("Message dispatched from " + saved.getSenderUsername());
                    Packet p = PacketFactory.createSuccessPacket(PacketType.CHAT_MESSAGE, saved);
                    if (saved.getReceiverId() != null) {
                        ClientManager.getInstance().sendDirectPacket(saved.getReceiverId(), p);
                    }
                    sendJsonResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendJsonResponse(exchange, 500, "{\"success\":false}");
                }
            }
        }
    }

    private class ReactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                Map<String, String> params = parseJsonSimple(body);
                Long messageId = Long.parseLong(params.get("messageId"));
                String emoji = params.get("emoji");

                synchronized (messageReactions) {
                    Map<String, Integer> map = messageReactions.computeIfAbsent(messageId, k -> new HashMap<>());
                    map.put(emoji, map.getOrDefault(emoji, 0) + 1);
                }
                sendJsonResponse(exchange, 200, "{\"success\":true}");
            }
        }
    }

    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedHeapMB = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            long maxHeapMB = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
            int activeTcpClients = ClientManager.getInstance().getActiveCount();
            int activeThreads = Thread.activeCount();

            String metricsJson = String.format("{" +
                            "\"activeTcpClients\":%d," +
                            "\"activeThreads\":%d," +
                            "\"usedHeapMB\":%d," +
                            "\"maxHeapMB\":%d," +
                            "\"uptimeSeconds\":%d" +
                            "}",
                    activeTcpClients,
                    activeThreads,
                    usedHeapMB,
                    maxHeapMB,
                    ManagementFactory.getRuntimeMXBean().getUptime() / 1000
            );
            sendJsonResponse(exchange, 200, metricsJson);
        }
    }

    private class SystemLogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("[");
            synchronized (systemActivityLogs) {
                for (int i = 0; i < systemActivityLogs.size(); i++) {
                    sb.append("\"").append(escapeJson(systemActivityLogs.get(i))).append("\"");
                    if (i < systemActivityLogs.size() - 1) sb.append(",");
                }
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class GroupsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> qp = parseQueryParams(query);
                Long uId = qp.containsKey("uId") ? Long.parseLong(qp.get("uId")) : 0L;

                List<ChatGroup> groups = groupService.getUserGroups(uId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < groups.size(); i++) {
                    ChatGroup g = groups.get(i);
                    sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"description\":\"%s\"}",
                            g.getId(), escapeJson(g.getName()), escapeJson(g.getDescription())));
                    if (i < groups.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());
            }
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseJsonSimple(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;
        body = body.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length() - 1);

        String[] pairs = body.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim().replace("\"", "");
                map.put(k, v);
            }
        }
        return map;
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                map.put(pair[0], pair[1]);
            }
        }
        return map;
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String getUltimateWebUIHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\" data-theme=\"midnight\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Antigravity Enterprise Industrial Real-Time Platform</title>\n" +
                "  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n" +
                "  <link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n" +
                "  <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n" +
                "  <style>\n" +
                "    :root {\n" +
                "      --bg-main: linear-gradient(135deg, #090d16 0%, #111827 50%, #1e1b4b 100%);\n" +
                "      --card-glass: rgba(17, 24, 39, 0.75);\n" +
                "      --glass-border: rgba(255, 255, 255, 0.1);\n" +
                "      --accent-indigo: #6366f1;\n" +
                "      --accent-purple: #a855f7;\n" +
                "      --accent-cyan: #06b6d4;\n" +
                "      --online-emerald: #10b981;\n" +
                "      --text-heading: #f8fafc;\n" +
                "      --text-sub: #94a3b8;\n" +
                "      --sent-gradient: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);\n" +
                "      --recv-glass: rgba(31, 41, 55, 0.85);\n" +
                "    }\n" +
                "    [data-theme=\"neon\"] {\n" +
                "      --bg-main: linear-gradient(135deg, #180828 0%, #2e0854 50%, #4c0519 100%);\n" +
                "      --accent-indigo: #f43f5e;\n" +
                "      --accent-purple: #fb923c;\n" +
                "      --sent-gradient: linear-gradient(135deg, #e11d48 0%, #f97316 100%);\n" +
                "    }\n" +
                "    [data-theme=\"emerald\"] {\n" +
                "      --bg-main: linear-gradient(135deg, #022c22 0%, #064e3b 50%, #0f172a 100%);\n" +
                "      --accent-indigo: #10b981;\n" +
                "      --accent-purple: #34d399;\n" +
                "      --sent-gradient: linear-gradient(135deg, #059669 0%, #10b981 100%);\n" +
                "    }\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; transition: all 0.25s ease; }\n" +
                "    body { background: var(--bg-main); color: var(--text-heading); height: 100vh; display: flex; align-items: center; justify-content: center; overflow: hidden; }\n" +
                "    \n" +
                "    /* Animated Orbs */\n" +
                "    .mesh-orb { position: absolute; border-radius: 50%; filter: blur(90px); opacity: 0.45; animation: float 14s infinite alternate ease-in-out; }\n" +
                "    .orb-a { width: 420px; height: 420px; background: var(--accent-indigo); top: -100px; left: -100px; }\n" +
                "    .orb-b { width: 450px; height: 450px; background: var(--accent-purple); bottom: -120px; right: -120px; animation-delay: -7s; }\n" +
                "    @keyframes float { 0% { transform: translate(0, 0) scale(1); } 100% { transform: translate(70px, 50px) scale(1.15); } }\n" +
                "    \n" +
                "    .app-card {\n" +
                "      position: relative; z-index: 10; width: 1150px; height: 740px;\n" +
                "      background: var(--card-glass); backdrop-filter: blur(25px); -webkit-backdrop-filter: blur(25px);\n" +
                "      border: 1px solid var(--glass-border); border-radius: 28px;\n" +
                "      display: flex; overflow: hidden; box-shadow: 0 30px 60px rgba(0,0,0,0.6);\n" +
                "    }\n" +
                "    \n" +
                "    .sidebar { width: 350px; border-right: 1px solid var(--glass-border); display: flex; flex-direction: column; background: rgba(15, 23, 42, 0.45); }\n" +
                "    .sidebar-header { padding: 20px 24px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--glass-border); }\n" +
                "    .avatar-icon {\n" +
                "      width: 44px; height: 44px; border-radius: 16px;\n" +
                "      background: linear-gradient(135deg, var(--accent-indigo), var(--accent-purple));\n" +
                "      display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 18px; color: #fff;\n" +
                "      box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);\n" +
                "    }\n" +
                "    \n" +
                "    .search-box { padding: 14px 20px; }\n" +
                "    .search-box input { width: 100%; padding: 12px 18px; background: rgba(255, 255, 255, 0.05); border: 1px solid var(--glass-border); border-radius: 14px; color: #fff; outline: none; font-size: 14px; }\n" +
                "    \n" +
                "    .contact-list { flex: 1; overflow-y: auto; padding: 10px 14px; }\n" +
                "    .contact-card {\n" +
                "      padding: 14px 16px; border-radius: 16px; margin-bottom: 8px; cursor: pointer; display: flex; align-items: center; justify-content: space-between;\n" +
                "      background: rgba(255, 255, 255, 0.02); border: 1px solid transparent;\n" +
                "    }\n" +
                "    .contact-card:hover, .contact-card.active {\n" +
                "      background: rgba(99, 102, 241, 0.15); border-color: rgba(99, 102, 241, 0.3); transform: translateX(4px);\n" +
                "    }\n" +
                "    .status-ring { width: 12px; height: 12px; border-radius: 50%; border: 2px solid #090d16; }\n" +
                "    .status-ring.online { background: var(--online-emerald); box-shadow: 0 0 12px var(--online-emerald); }\n" +
                "    .status-ring.offline { background: #64748b; }\n" +
                "    \n" +
                "    /* Chat View */\n" +
                "    .chat-view { flex: 1; display: flex; flex-direction: column; background: rgba(15, 23, 42, 0.2); position: relative; }\n" +
                "    .chat-topbar { padding: 20px 28px; border-bottom: 1px solid var(--glass-border); display: flex; align-items: center; justify-content: space-between; background: rgba(31, 41, 55, 0.4); }\n" +
                "    .chat-messages { flex: 1; padding: 28px; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; }\n" +
                "    \n" +
                "    .msg-group { display: flex; flex-direction: column; max-width: 68%; animation: msgPop 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28); position: relative; }\n" +
                "    @keyframes msgPop { from { opacity: 0; transform: translateY(16px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }\n" +
                "    .msg-group.sent { align-self: flex-end; }\n" +
                "    .msg-group.recv { align-self: flex-start; }\n" +
                "    \n" +
                "    .msg-bubble { padding: 14px 20px; border-radius: 20px; font-size: 14px; line-height: 1.5; box-shadow: 0 6px 20px rgba(0,0,0,0.25); position: relative; }\n" +
                "    .msg-group.sent .msg-bubble { background: var(--sent-gradient); color: #fff; border-bottom-right-radius: 4px; }\n" +
                "    .msg-group.recv .msg-bubble { background: var(--recv-glass); color: var(--text-heading); border-bottom-left-radius: 4px; border: 1px solid var(--glass-border); }\n" +
                "    \n" +
                "    /* Interactive Voice Note Player */\n" +
                "    .voice-player { display: flex; align-items: center; gap: 12px; min-width: 220px; }\n" +
                "    .play-btn { width: 36px; height: 36px; border-radius: 50%; background: rgba(255,255,255,0.2); border: none; color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 14px; }\n" +
                "    .eq-bars { display: flex; align-items: center; gap: 3px; flex: 1; height: 24px; }\n" +
                "    .eq-bar { flex: 1; background: rgba(255,255,255,0.5); border-radius: 2px; animation: eqPulse 0.8s infinite alternate ease-in-out; }\n" +
                "    @keyframes eqPulse { 0% { height: 20%; } 100% { height: 100%; } }\n" +
                "    \n" +
                "    /* Reactions & Tools */\n" +
                "    .reaction-bar { display: flex; gap: 4px; position: absolute; top: -14px; right: 10px; background: rgba(15, 23, 42, 0.9); border: 1px solid var(--glass-border); border-radius: 20px; padding: 2px 8px; font-size: 12px; cursor: pointer; opacity: 0; transition: opacity 0.2s; }\n" +
                "    .msg-group:hover .reaction-bar { opacity: 1; }\n" +
                "    .reaction-pill { background: rgba(255, 255, 255, 0.1); border-radius: 10px; padding: 2px 6px; font-size: 11px; margin-top: 4px; display: inline-flex; align-items: center; gap: 4px; }\n" +
                "    \n" +
                "    .chat-bottom { padding: 18px 28px; background: rgba(31, 41, 55, 0.5); border-top: 1px solid var(--glass-border); display: flex; flex-direction: column; gap: 10px; }\n" +
                "    .quick-toolbar { display: flex; align-items: center; justify-content: space-between; font-size: 13px; color: var(--text-sub); }\n" +
                "    .emoji-row span { font-size: 20px; cursor: pointer; padding: 0 4px; transition: transform 0.15s; }\n" +
                "    .emoji-row span:hover { transform: scale(1.35); }\n" +
                "    \n" +
                "    .input-box-wrapper { display: flex; align-items: center; gap: 12px; }\n" +
                "    .input-box-wrapper input { flex: 1; padding: 14px 20px; background: rgba(255, 255, 255, 0.06); border: 1px solid var(--glass-border); border-radius: 16px; color: #fff; outline: none; font-size: 15px; }\n" +
                "    .input-box-wrapper input:focus { border-color: var(--accent-indigo); box-shadow: 0 0 20px rgba(99, 102, 241, 0.35); }\n" +
                "    .btn-action { padding: 14px 28px; background: linear-gradient(135deg, var(--accent-indigo) 0%, var(--accent-purple) 100%); border: none; border-radius: 16px; color: #fff; font-weight: 600; cursor: pointer; box-shadow: 0 4px 18px rgba(99, 102, 241, 0.4); }\n" +
                "    .btn-action:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(99, 102, 241, 0.6); }\n" +
                "    \n" +
                "    /* Modals */\n" +
                "    .modal-overlay { position: fixed; inset: 0; background: rgba(9, 13, 22, 0.85); backdrop-filter: blur(20px); display: flex; align-items: center; justify-content: center; z-index: 100; opacity: 0; pointer-events: none; transition: opacity 0.3s; }\n" +
                "    .modal-overlay.active { opacity: 1; pointer-events: auto; }\n" +
                "    .modal-card { width: 500px; padding: 32px; background: rgba(31, 41, 55, 0.95); border: 1px solid var(--glass-border); border-radius: 28px; box-shadow: 0 25px 60px rgba(0,0,0,0.6); display: flex; flex-direction: column; gap: 16px; }\n" +
                "    .modal-card input { padding: 12px 18px; background: rgba(255, 255, 255, 0.06); border: 1px solid var(--glass-border); border-radius: 14px; color: #fff; outline: none; }\n" +
                "    \n" +
                "    .terminal-logs { background: #050811; border: 1px solid var(--glass-border); border-radius: 14px; padding: 12px; font-family: monospace; font-size: 11px; color: #38bdf8; height: 160px; overflow-y: auto; white-space: pre-wrap; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"mesh-orb orb-a\"></div>\n" +
                "  <div class=\"mesh-orb orb-b\"></div>\n" +
                "\n" +
                "  <div class=\"app-card\">\n" +
                "    <!-- Auth Modal -->\n" +
                "    <div id=\"authOverlay\" class=\"modal-overlay active\">\n" +
                "      <div class=\"modal-card\">\n" +
                "        <h2 style=\"font-size: 26px; color: var(--accent-indigo); text-align: center;\">Antigravity Enterprise</h2>\n" +
                "        <p style=\"font-size: 13px; color: var(--text-sub); text-align: center;\">Industry Placement-Ready Real-Time Platform</p>\n" +
                "        <input id=\"authUsername\" type=\"text\" placeholder=\"Username\" value=\"alice\">\n" +
                "        <input id=\"authEmail\" type=\"email\" placeholder=\"Email (for register)\" value=\"alice@chat.com\">\n" +
                "        <input id=\"authPassword\" type=\"password\" placeholder=\"Password\" value=\"pass123\">\n" +
                "        <button class=\"btn-action\" onclick=\"doLogin()\">Sign In</button>\n" +
                "        <button class=\"btn-action\" onclick=\"doRegister()\" style=\"background: rgba(255,255,255,0.08); border: 1px solid var(--glass-border);\">Create Account</button>\n" +
                "        <div id=\"authErr\" style=\"color: #f87171; font-size: 12px; text-align: center;\"></div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Logs & Metrics Modal -->\n" +
                "    <div id=\"metricsOverlay\" class=\"modal-overlay\">\n" +
                "      <div class=\"modal-card\">\n" +
                "        <div style=\"display:flex; justify-content:space-between; align-items:center;\">\n" +
                "          <h3 style=\"color: var(--accent-cyan);\">⚡ Real-Time System & Server Inspector</h3>\n" +
                "          <span onclick=\"toggleMetrics()\" style=\"cursor:pointer; font-size:18px;\">✕</span>\n" +
                "        </div>\n" +
                "        <div style=\"display:flex; gap:10px;\">\n" +
                "          <div style=\"flex:1; background:rgba(255,255,255,0.04); padding:12px; border-radius:14px; text-align:center;\">Active TCP Sockets<div id=\"mTcpClients\" style=\"font-size:20px; font-weight:700; color:var(--accent-cyan);\">0</div></div>\n" +
                "          <div style=\"flex:1; background:rgba(255,255,255,0.04); padding:12px; border-radius:14px; text-align:center;\">Threads<div id=\"mThreads\" style=\"font-size:20px; font-weight:700; color:var(--accent-cyan);\">0</div></div>\n" +
                "          <div style=\"flex:1; background:rgba(255,255,255,0.04); padding:12px; border-radius:14px; text-align:center;\">Heap Memory<div id=\"mHeap\" style=\"font-size:20px; font-weight:700; color:var(--accent-cyan);\">0 MB</div></div>\n" +
                "        </div>\n" +
                "        <div style=\"font-size:12px; color:var(--text-sub); margin-top:6px;\">Live Server Event Stream Logs:</div>\n" +
                "        <div id=\"logStream\" class=\"terminal-logs\">Fetching server logs...</div>\n" +
                "        <button class=\"btn-action\" onclick=\"toggleMetrics()\">Close Inspector</button>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Sidebar -->\n" +
                "    <div class=\"sidebar\">\n" +
                "      <div class=\"sidebar-header\">\n" +
                "        <div class=\"user-profile\">\n" +
                "          <div id=\"myAvatar\" class=\"avatar-icon\">A</div>\n" +
                "          <div>\n" +
                "            <div id=\"myUserLabel\" style=\"font-weight:600;\">User</div>\n" +
                "            <div style=\"font-size:12px; color:var(--online-emerald);\">● Active Online</div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        <div style=\"display:flex; gap:6px;\">\n" +
                "          <select onchange=\"changeTheme(this.value)\" style=\"background:rgba(255,255,255,0.08); border:none; color:#fff; border-radius:8px; padding:4px;\">\n" +
                "            <option value=\"midnight\">Midnight</option>\n" +
                "            <option value=\"neon\">Neon</option>\n" +
                "            <option value=\"emerald\">Emerald</option>\n" +
                "          </select>\n" +
                "          <button onclick=\"toggleMetrics()\" title=\"Server Performance Inspector\" style=\"background:rgba(255,255,255,0.08); border:none; color:var(--accent-cyan); padding:8px 10px; border-radius:12px; cursor:pointer;\">📊</button>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <div class=\"search-box\">\n" +
                "        <input type=\"text\" placeholder=\"🔍 Search contacts...\" onkeyup=\"filterContacts(this.value)\">\n" +
                "      </div>\n" +
                "      <div id=\"userList\" class=\"contact-list\"></div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Main Chat Area -->\n" +
                "    <div class=\"chat-view\">\n" +
                "      <div class=\"chat-topbar\">\n" +
                "        <div>\n" +
                "          <div id=\"chatTitle\" style=\"font-weight:600; font-size:17px;\">Select a contact</div>\n" +
                "          <div id=\"chatSubtitle\" style=\"font-size:12px; color:var(--text-sub);\">Real-time Socket & Web Gateway Protocol</div>\n" +
                "        </div>\n" +
                "        <div style=\"display:flex; gap:8px;\">\n" +
                "          <button onclick=\"sendVoiceNote()\" style=\"background:rgba(99, 102, 241, 0.2); border:1px solid var(--accent-indigo); padding:8px 14px; border-radius:12px; color:#fff; cursor:pointer;\">🎤 Voice Note</button>\n" +
                "          <button onclick=\"playNotificationChime()\" style=\"background:rgba(255,255,255,0.08); border:1px solid var(--glass-border); padding:8px 14px; border-radius:12px; color:#fff; cursor:pointer;\">🔔 Chime</button>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <div id=\"chatMessages\" class=\"chat-messages\">\n" +
                "        <div style=\"text-align:center; color:var(--text-sub); margin-top:60px;\">Select a user from the sidebar to open conversation</div>\n" +
                "      </div>\n" +
                "      <div class=\"chat-bottom\">\n" +
                "        <div class=\"quick-toolbar\">\n" +
                "          <div class=\"emoji-row\">\n" +
                "            <span onclick=\"addEmoji('😀')\">😀</span>\n" +
                "            <span onclick=\"addEmoji('😂')\">😂</span>\n" +
                "            <span onclick=\"addEmoji('😍')\">😍</span>\n" +
                "            <span onclick=\"addEmoji('👍')\">👍</span>\n" +
                "            <span onclick=\"addEmoji('❤️')\">❤️</span>\n" +
                "            <span onclick=\"addEmoji('🔥')\">🔥</span>\n" +
                "            <span onclick=\"addEmoji('🚀')\">🚀</span>\n" +
                "            <span onclick=\"addEmoji('🎉')\">🎉</span>\n" +
                "          </div>\n" +
                "          <label style=\"cursor:pointer; display:flex; align-items:center; gap:6px;\">\n" +
                "            <input id=\"aesEncryptCheck\" type=\"checkbox\"> 🔐 Encrypt Payload\n" +
                "          </label>\n" +
                "        </div>\n" +
                "        <div class=\"input-box-wrapper\">\n" +
                "          <input id=\"msgInput\" type=\"text\" placeholder=\"Type a message...\" onkeydown=\"if(event.key==='Enter') sendMsg()\">\n" +
                "          <button class=\"btn-action\" onclick=\"sendMsg()\">Send</button>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <script>\n" +
                "    let currentUser = null;\n" +
                "    let activeTarget = null;\n" +
                "    let allContacts = [];\n" +
                "    let lastMsgCount = 0;\n" +
                "\n" +
                "    async function doLogin() {\n" +
                "      const u = document.getElementById('authUsername').value;\n" +
                "      const p = document.getElementById('authPassword').value;\n" +
                "      try {\n" +
                "        const res = await fetch('/api/login', { method:'POST', body: JSON.stringify({usernameOrEmail:u, password:p}) });\n" +
                "        const data = await res.json();\n" +
                "        if(data.success) {\n" +
                "          currentUser = data.user;\n" +
                "          document.getElementById('authOverlay').classList.remove('active');\n" +
                "          document.getElementById('myUserLabel').innerText = currentUser.username;\n" +
                "          document.getElementById('myAvatar').innerText = currentUser.username.charAt(0).toUpperCase();\n" +
                "          loadUsers();\n" +
                "          setInterval(loadMessages, 1200);\n" +
                "          setInterval(loadUsers, 4000);\n" +
                "          setInterval(fetchMetrics, 3000);\n" +
                "        } else {\n" +
                "          document.getElementById('authErr').innerText = data.message;\n" +
                "        }\n" +
                "      } catch(e) {\n" +
                "        document.getElementById('authErr').innerText = 'Server connection failed.';\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    async function doRegister() {\n" +
                "      const u = document.getElementById('authUsername').value;\n" +
                "      const e = document.getElementById('authEmail').value;\n" +
                "      const p = document.getElementById('authPassword').value;\n" +
                "      const res = await fetch('/api/register', { method:'POST', body: JSON.stringify({username:u, email:e, password:p}) });\n" +
                "      const data = await res.json();\n" +
                "      if(data.success) alert('Account registered! Click Sign In.'); else document.getElementById('authErr').innerText = data.message;\n" +
                "    }\n" +
                "\n" +
                "    async function loadUsers() {\n" +
                "      const res = await fetch('/api/users');\n" +
                "      allContacts = await res.json();\n" +
                "      renderContacts(allContacts);\n" +
                "    }\n" +
                "\n" +
                "    function renderContacts(users) {\n" +
                "      const list = document.getElementById('userList');\n" +
                "      list.innerHTML = '';\n" +
                "      users.forEach(u => {\n" +
                "        if(currentUser && u.id === currentUser.id) return;\n" +
                "        const isOnline = u.status === 'ONLINE';\n" +
                "        const div = document.createElement('div');\n" +
                "        div.className = 'contact-card' + (activeTarget && activeTarget.id === u.id ? ' active' : '');\n" +
                "        div.innerHTML = `\n" +
                "          <div style=\"display:flex; align-items:center; gap:12px;\">\n" +
                "            <div class=\"avatar-icon\" style=\"width:40px; height:40px; font-size:15px;\">${u.username.charAt(0).toUpperCase()}</div>\n" +
                "            <div>\n" +
                "              <div style=\"font-weight:600; font-size:14px;\">${u.username}</div>\n" +
                "              <div style=\"font-size:11px; color:var(--text-sub);\">${u.email}</div>\n" +
                "            </div>\n" +
                "          </div>\n" +
                "          <div class=\"status-ring ${isOnline ? 'online' : 'offline'}\"></div>\n" +
                "        `;\n" +
                "        div.onclick = () => selectUser(u);\n" +
                "        list.appendChild(div);\n" +
                "      });\n" +
                "    }\n" +
                "\n" +
                "    function filterContacts(query) {\n" +
                "      const filtered = allContacts.filter(c => c.username.toLowerCase().includes(query.toLowerCase()));\n" +
                "      renderContacts(filtered);\n" +
                "    }\n" +
                "\n" +
                "    function selectUser(user) {\n" +
                "      activeTarget = user;\n" +
                "      document.getElementById('chatTitle').innerText = user.username;\n" +
                "      document.getElementById('chatSubtitle').innerText = user.status === 'ONLINE' ? '● Online Now' : '○ Offline';\n" +
                "      loadUsers();\n" +
                "      loadMessages();\n" +
                "    }\n" +
                "\n" +
                "    async function loadMessages() {\n" +
                "      if(!activeTarget || !currentUser) return;\n" +
                "      const res = await fetch(`/api/messages?u1=${currentUser.id}&u2=${activeTarget.id}`);\n" +
                "      const msgs = await res.json();\n" +
                "      \n" +
                "      if(msgs.length > lastMsgCount && lastMsgCount !== 0) {\n" +
                "        playNotificationChime();\n" +
                "      }\n" +
                "      lastMsgCount = msgs.length;\n" +
                "\n" +
                "      const box = document.getElementById('chatMessages');\n" +
                "      box.innerHTML = '';\n" +
                "      msgs.forEach(m => {\n" +
                "        const isMe = m.senderId === currentUser.id;\n" +
                "        const group = document.createElement('div');\n" +
                "        group.className = 'msg-group ' + (isMe ? 'sent' : 'recv');\n" +
                "        \n" +
                "        let reactionsHtml = '';\n" +
                "        if(m.reactions) {\n" +
                "          for(const [emoji, count] of Object.entries(m.reactions)) {\n" +
                "            reactionsHtml += `<span class=\"reaction-pill\">${emoji} ${count}</span>`;\n" +
                "          }\n" +
                "        }\n" +
                "\n" +
                "        let bubbleContent = escapeHtml(m.content);\n" +
                "        if(m.content.startsWith('[Voice Note]')) {\n" +
                "          bubbleContent = `\n" +
                "            <div class=\"voice-player\">\n" +
                "              <button class=\"play-btn\" onclick=\"playNotificationChime()\">▶</button>\n" +
                "              <div class=\"eq-bars\">\n" +
                "                <div class=\"eq-bar\" style=\"animation-delay:-0.2s;\"></div>\n" +
                "                <div class=\"eq-bar\" style=\"animation-delay:-0.4s;\"></div>\n" +
                "                <div class=\"eq-bar\" style=\"animation-delay:-0.1s;\"></div>\n" +
                "                <div class=\"eq-bar\" style=\"animation-delay:-0.5s;\"></div>\n" +
                "              </div>\n" +
                "              <span style=\"font-size:12px; opacity:0.8;\">0:14</span>\n" +
                "            </div>\n" +
                "          `;\n" +
                "        }\n" +
                "\n" +
                "        group.innerHTML = `\n" +
                "          <div class=\"reaction-bar\">\n" +
                "            <span onclick=\"reactMsg(${m.id}, '👍')\">👍</span>\n" +
                "            <span onclick=\"reactMsg(${m.id}, '❤️')\">❤️</span>\n" +
                "            <span onclick=\"reactMsg(${m.id}, '🔥')\">🔥</span>\n" +
                "            <span onclick=\"reactMsg(${m.id}, '😂')\">😂</span>\n" +
                "          </div>\n" +
                "          <div class=\"msg-bubble\">${bubbleContent}</div>\n" +
                "          <div>${reactionsHtml}</div>\n" +
                "          <div class=\"msg-footer\">\n" +
                "            <span>${m.timestamp ? m.timestamp.substring(11, 16) : ''}</span>\n" +
                "            ${isMe ? '<span style=\"color:#818cf8;\">✓✓</span>' : ''}\n" +
                "            ${m.isEncrypted ? '🔐' : ''}\n" +
                "          </div>\n" +
                "        `;\n" +
                "        box.appendChild(group);\n" +
                "      });\n" +
                "      box.scrollTop = box.scrollHeight;\n" +
                "    }\n" +
                "\n" +
                "    async function sendVoiceNote() {\n" +
                "      if(!activeTarget) return;\n" +
                "      await fetch('/api/messages', { method:'POST', body: JSON.stringify({\n" +
                "        senderId: currentUser.id, \n" +
                "        senderUsername: currentUser.username, \n" +
                "        receiverId: activeTarget.id, \n" +
                "        content: '[Voice Note] 0:14 Audio Message',\n" +
                "        isAudio: true\n" +
                "      }) });\n" +
                "      loadMessages();\n" +
                "    }\n" +
                "\n" +
                "    async function reactMsg(msgId, emoji) {\n" +
                "      await fetch('/api/react', { method:'POST', body: JSON.stringify({messageId: msgId, emoji: emoji}) });\n" +
                "      loadMessages();\n" +
                "    }\n" +
                "\n" +
                "    async function sendMsg() {\n" +
                "      const inp = document.getElementById('msgInput');\n" +
                "      const txt = inp.value.trim();\n" +
                "      if(!txt || !activeTarget) return;\n" +
                "      const isEncrypted = document.getElementById('aesEncryptCheck').checked;\n" +
                "      await fetch('/api/messages', { method:'POST', body: JSON.stringify({\n" +
                "        senderId: currentUser.id, \n" +
                "        senderUsername: currentUser.username, \n" +
                "        receiverId: activeTarget.id, \n" +
                "        content: txt,\n" +
                "        isEncrypted: isEncrypted\n" +
                "      }) });\n" +
                "      inp.value = '';\n" +
                "      loadMessages();\n" +
                "    }\n" +
                "\n" +
                "    async function fetchMetrics() {\n" +
                "      try {\n" +
                "        const res = await fetch('/api/metrics');\n" +
                "        const data = await res.json();\n" +
                "        document.getElementById('mTcpClients').innerText = data.activeTcpClients;\n" +
                "        document.getElementById('mThreads').innerText = data.activeThreads;\n" +
                "        document.getElementById('mHeap').innerText = data.usedHeapMB + ' MB';\n" +
                "        \n" +
                "        const logsRes = await fetch('/api/logs');\n" +
                "        const logs = await logsRes.json();\n" +
                "        document.getElementById('logStream').innerText = logs.join('\\n');\n" +
                "      } catch(e) {}\n" +
                "    }\n" +
                "\n" +
                "    function toggleMetrics() {\n" +
                "      const m = document.getElementById('metricsOverlay');\n" +
                "      m.classList.toggle('active');\n" +
                "      if(m.classList.contains('active')) fetchMetrics();\n" +
                "    }\n" +
                "\n" +
                "    function changeTheme(themeName) {\n" +
                "      document.documentElement.setAttribute('data-theme', themeName);\n" +
                "    }\n" +
                "\n" +
                "    function playNotificationChime() {\n" +
                "      try {\n" +
                "        const ctx = new (window.AudioContext || window.webkitAudioContext)();\n" +
                "        const osc = ctx.createOscillator();\n" +
                "        const gain = ctx.createGain();\n" +
                "        osc.type = 'sine';\n" +
                "        osc.frequency.setValueAtTime(587.33, ctx.currentTime);\n" +
                "        osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.15);\n" +
                "        gain.gain.setValueAtTime(0.15, ctx.currentTime);\n" +
                "        gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.3);\n" +
                "        osc.connect(gain);\n" +
                "        gain.connect(ctx.destination);\n" +
                "        osc.start();\n" +
                "        osc.stop(ctx.currentTime + 0.3);\n" +
                "      } catch(e) {}\n" +
                "    }\n" +
                "\n" +
                "    function addEmoji(emoji) {\n" +
                "      const inp = document.getElementById('msgInput');\n" +
                "      inp.value += emoji;\n" +
                "      inp.focus();\n" +
                "    }\n" +
                "\n" +
                "    function escapeHtml(str) {\n" +
                "      return str.replace(/&/g, \"&amp;\").replace(/</g, \"&lt;\").replace(/>/g, \"&gt;\");\n" +
                "    }\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}

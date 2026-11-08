package ru.migrenenet.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.migrenenet.model.Subscriber;
import ru.migrenenet.repo.SubscriberRepository;
import ru.migrenenet.service.AlertService;

/** Telegram-бот: подписка, кнопка Mini App, утреннее напоминание, сводка врачу. Работает на long polling. */
@Component
public class TelegramBot {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final String token, webAppUrl, doctorChat;
    private final ObjectMapper om;
    private final SubscriberRepository subs;
    private final AlertService alerts;
    private final HttpClient http = HttpClient.newHttpClient();

    public TelegramBot(@Value("${telegram.token:}") String token,
                       @Value("${telegram.webapp-url:}") String webAppUrl,
                       @Value("${doctor.chat-id:}") String doctorChat,
                       ObjectMapper om, SubscriberRepository subs, AlertService alerts) {
        this.token = token; this.webAppUrl = webAppUrl; this.doctorChat = doctorChat;
        this.om = om; this.subs = subs; this.alerts = alerts;
    }

    @PostConstruct
    void start() {
        if (token.isBlank()) { log.info("Telegram-бот выключен: TELEGRAM_TOKEN не задан"); return; }
        Thread t = new Thread(this::poll, "telegram-poll");
        t.setDaemon(true);
        t.start();
    }

    private void poll() {
        long offset = 0;
        while (true) {
            try {
                JsonNode r = call("getUpdates", Map.of("offset", offset, "timeout", 30));
                for (JsonNode u : r.path("result")) {
                    offset = u.path("update_id").asLong() + 1;
                    handle(u.path("message"));
                }
            } catch (Exception e) {
                log.warn("getUpdates: {}", e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ie) { return; }
            }
        }
    }

    private void handle(JsonNode msg) {
        if (msg.isMissingNode()) return;
        long chat = msg.path("chat").path("id").asLong();
        String text = msg.path("text").asText("").trim();
        if (text.startsWith("/start")) {
            Subscriber s = subs.findById(chat).orElseGet(() -> new Subscriber(chat));
            s.setActive(true);
            subs.save(s);
            send(chat, "Здравствуйте! Я помогаю вести дневник мигрени. Каждое утро в 08:00 напомню заполнить его. "
                + "Если боль внезапная и очень сильная, есть слабость, нарушение речи или зрения, звоните 103 / 112.", diaryButton());
        } else if (text.startsWith("/stop")) {
            subs.findById(chat).ifPresent(s -> { s.setActive(false); subs.save(s); });
            send(chat, "Напоминания выключены. Вернуть их можно командой /start.", null);
        } else if (text.startsWith("/diary")) {
            send(chat, "Откройте дневник за прошедшие сутки:", diaryButton());
        } else {
            send(chat, "Команды: /diary — открыть дневник, /stop — выключить напоминания.", null);
        }
    }

    @Scheduled(cron = "${reminder.cron:0 0 8 * * *}", zone = "${reminder.zone:Europe/Moscow}")
    void morningReminder() {
        if (token.isBlank()) return;
        for (Subscriber s : subs.findByActiveTrue())
            send(s.getChatId(), "Доброе утро! Заполните дневник за прошедшие сутки, это займёт минуту.", diaryButton());
    }

    @Scheduled(cron = "${doctor.cron:0 30 11 * * *}", zone = "${reminder.zone:Europe/Moscow}")
    void doctorDigest() {
        if (token.isBlank() || doctorChat.isBlank()) return;
        List<AlertService.Alert> list = alerts.all(LocalDate.now());
        StringBuilder sb = new StringBuilder("Сводка отклонений:\n");
        if (list.isEmpty()) sb.append("Отклонений нет.");
        for (var a : list) {
            String mark = switch (a.level()) { case RED -> "🔴"; case YELLOW -> "🟡"; case GREEN -> "🟢"; };
            sb.append("\n").append(mark).append(" Пациент ").append(a.patientId()).append(": ").append(a.message());
        }
        send(Long.parseLong(doctorChat), sb.toString(), null);
    }

    private Map<String, Object> diaryButton() {
        if (webAppUrl.isBlank()) return null;
        return Map.of("inline_keyboard", List.of(List.of(Map.of("text", "Заполнить дневник", "web_app", Map.of("url", webAppUrl)))));
    }

    private void send(long chat, String text, Map<String, Object> markup) {
        try {
            var body = new java.util.HashMap<String, Object>(Map.of("chat_id", chat, "text", text));
            if (markup != null) body.put("reply_markup", markup);
            call("sendMessage", body);
        } catch (Exception e) {
            log.warn("sendMessage {}: {}", chat, e.getMessage());
        }
    }

    private JsonNode call(String method, Map<String, Object> body) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + token + "/" + method))
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
            .build();
        return om.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
    }
}

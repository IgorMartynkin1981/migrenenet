package ru.migrenenet.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Проверка подписи initData из Telegram Mini App (HMAC-SHA256 по токену бота). */
@Component
public class TelegramAuth {
    private final String token;
    private final ObjectMapper om;

    public TelegramAuth(@Value("${telegram.token:}") String token, ObjectMapper om) {
        this.token = token; this.om = om;
    }

    public boolean enabled() { return !token.isBlank(); }

    public Optional<String> userId(String initData) {
        try {
            if (!enabled() || initData == null || initData.isBlank()) return Optional.empty();
            Map<String, String> p = new TreeMap<>();
            for (String pair : initData.split("&")) {
                int i = pair.indexOf('=');
                if (i > 0) p.put(pair.substring(0, i), URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
            }
            String hash = p.remove("hash");
            if (hash == null) return Optional.empty();
            StringBuilder sb = new StringBuilder();
            p.forEach((k, v) -> { if (sb.length() > 0) sb.append('\n'); sb.append(k).append('=').append(v); });
            byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
            String calc = hex(hmac(secret, sb.toString().getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(calc.getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8)))
                return Optional.empty();
            long age = System.currentTimeMillis() / 1000 - Long.parseLong(p.getOrDefault("auth_date", "0"));
            if (age > 86_400) return Optional.empty();
            return Optional.of(om.readTree(p.get("user")).path("id").asText());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static byte[] hmac(byte[] key, byte[] msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(msg);
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x));
        return s.toString();
    }
}

package io.github.oljc.arcoserve.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class SignUtils {
    private SignUtils(){}
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final BitSet URL_ENCODER = new BitSet(256);
    private static final Set<String> SIGNED_HEADERS = Set.of("accept", "x-date", "x-fingerprint", "access-token");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
                             .withZone(ZoneOffset.UTC);

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (Exception e) { throw new RuntimeException(e); }
    });

    private static final ThreadLocal<Mac> HMAC = ThreadLocal.withInitial(() -> {
        try { return Mac.getInstance("HmacSHA256"); }
        catch (Exception e) { throw new RuntimeException(e); }
    });

    static {
        int i;
        for (i = 97; i <= 122; ++i) URL_ENCODER.set(i);
        for (i = 65; i <= 90; ++i) URL_ENCODER.set(i);
        for (i = 48; i <= 57; ++i) URL_ENCODER.set(i);
        URL_ENCODER.set('-');
        URL_ENCODER.set('_');
        URL_ENCODER.set('.');
        URL_ENCODER.set('~');
    }

    /**
     * 验证签名
     * @param req 请求
     * @param accessId 访问ID
     * @param secretKey 密钥
     * @param age 有效期
     */
    public static void verify(HttpServletRequest req, String accessId, String secretKey, long age) throws IOException {
        final String date = req.getHeader("x-date");
        final String fingerprint = req.getHeader("x-fingerprint");
        final String signature = req.getHeader("authorization");

        if (date == null || fingerprint == null || signature == null) {
            throw new SecurityException("签名错误");
        }

        Instant ts = FORMATTER.parse(date, Instant::from);
        if (Instant.now().isAfter(ts.plusSeconds(age))) {
            throw new SecurityException("签名过期");
        }

        final String queryString = buildQuery(req);
        final String[] headerResult = buildHeaders(req);
        final String signedHeaders = headerResult[0];
        final String canonicalHeaders = headerResult[1];
        final String hashBody = buildHashBody(req);
        final String canonical = String.join("\n",
            req.getMethod().toUpperCase(),
            req.getRequestURI(),
            queryString,
            canonicalHeaders,
            signedHeaders,
            hashBody
        );

        final String shortDate = date.substring(0, 8);
        final String credential = String.join("/", accessId, shortDate, fingerprint, "oljc");
        final String stringToSign = String.join("\n",
            "LJC-HMAC-SHA256",
            date,
            credential,
            hashHex(canonical)
        );

        final String kDate = hmacHex(secretKey, date);
        final String kFingerprint = hmacHex(kDate, fingerprint);
        final String kSigning = hmacHex(kFingerprint, "oljc");
        final String kSign = hmacHex(kSigning, stringToSign);

        final String result = String.join(" ",
            "LJC-HMAC-SHA256",
            "Credential=" + credential + ",",
            "SignedHeaders=" + signedHeaders + ",",
            "Signature=" + kSign
        );

        if (!result.equals(signature)) {
            throw new SecurityException("签名错误");
        }
    }

    private static String[] buildHeaders(HttpServletRequest req) {
        var keys = Collections.list(req.getHeaderNames()).stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .filter(SIGNED_HEADERS::contains)
                .filter(h -> {
                    String v = req.getHeader(h);
                    return v != null && !v.isBlank();
                })
                .sorted()
                .toList();

        StringJoiner signedKeys = new StringJoiner(";");
        StringJoiner canonical = new StringJoiner("\n");

        for (String k : keys) {
            signedKeys.add(k);
            canonical.add(k + ":" + normalize(req.getHeader(k)));
        }

        return new String[]{signedKeys.toString(), canonical.toString()};
    }

    private static String buildQuery(HttpServletRequest req) {
        Map<String, String[]> params = req.getParameterMap();

        if (params == null || params.isEmpty()) return "";

        List<String> pairs = new ArrayList<>(params.size() * 2);
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String key = uriEscape(e.getKey());
            String[] values = e.getValue();

            if (values == null || values.length == 0) {
                pairs.add(uriEscape(key) + "=");
            } else {
                Arrays.sort(values);
                for (String v : values) {
                    pairs.add(uriEscape(key) + "=" + uriEscape(v != null ? v : ""));
                }
            }
        }
        pairs.sort(SignUtils::compareKeyValuePairs);
        return String.join("&", pairs);
    }

    private static int compareKeyValuePairs(String a, String b) {
        int aEqIndex = a.indexOf('=');
        int bEqIndex = b.indexOf('=');
        int keyCompare = a.substring(0, aEqIndex).compareTo(b.substring(0, bEqIndex));
        if (keyCompare != 0) return keyCompare;
        return a.substring(aEqIndex + 1).compareTo(b.substring(bEqIndex + 1));
    }

    private static String buildHashBody(HttpServletRequest req) throws IOException {
        byte[] body = req.getInputStream().readAllBytes();
        if (body.length == 0) return hashHex("");

        String contentType = req.getContentType();
        String bodyString = new String(body, StandardCharsets.UTF_8);
        if (contentType != null && contentType.toLowerCase().contains("application/x-www-form-urlencoded")) {
            return hashFormData(bodyString);
        }
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            return hashJsonData(bodyString);
        }

        return hashHex(bodyString);
    }

    private static String hashFormData(String formData) {
        if (formData == null || formData.trim().isEmpty()) {
            return hashHex("");
        }

        List<String> pairs = new ArrayList<>();
        String[] parts = formData.split("&");

        for (String part : parts) {
            if (part.trim().isEmpty()) continue;

            String[] keyValue = part.split("=", 2);
            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";

            try {
                key = URLDecoder.decode(key, StandardCharsets.UTF_8);
                value = URLDecoder.decode(value, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // 如果解码失败，使用原始值
            }

            pairs.add(uriEscape(key) + "=" + uriEscape(value));
        }

        Collections.sort(pairs);
        return hashHex(String.join("&", pairs));
    }

    private static String hashJsonData(String jsonData) {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            return hashHex("");
        }

        try {
            Object obj = OBJECT_MAPPER.readValue(jsonData, Object.class);
            String normalizedJson = stableJsonStringify(obj);
            return hashHex(normalizedJson);
        } catch (JsonProcessingException e) {
            return hashHex(jsonData);
        }
    }

    @SuppressWarnings("unchecked")
    private static String stableJsonStringify(Object obj) throws JsonProcessingException {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Map<String, Object> sortedMap = new TreeMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sortedMap.put(entry.getKey(), stableJsonValue(entry.getValue()));
            }
            return OBJECT_MAPPER.writeValueAsString(sortedMap);
        }

        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    @SuppressWarnings("unchecked")
    private static Object stableJsonValue(Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, Object> sortedMap = new TreeMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sortedMap.put(entry.getKey(), stableJsonValue(entry.getValue()));
            }
            return sortedMap;
        }
        return value;
    }

    private static String normalize(String v) {
        return v == null ? "" : v.trim().replaceAll("\\s+", " ");
    }

    private static String uriEscape(String str) {
        if (str == null) return "";
        StringBuilder buf = new StringBuilder(str.length());
        ByteBuffer bb = StandardCharsets.UTF_8.encode(str);
        while (bb.hasRemaining()) {
            int b = bb.get() & 255;
            if (URL_ENCODER.get(b)) {
                buf.append((char) b);
            } else if (b == 32) {
                buf.append("%20");
            } else {
                buf.append("%");
                char hex1 = HEX[b >> 4];
                char hex2 = HEX[b & 15];
                buf.append(hex1);
                buf.append(hex2);
            }
        }

        return buf.toString();
    }

    /**
     * SHA256 哈希
     */
    private static String hashHex(String data) {
        MessageDigest digest = SHA256.get();
        digest.reset();
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * HMAC-SHA256
     */
    private static String hmacHex(String key, String data) {
        try {
            Mac mac = HMAC.get();
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("加签失败", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] c = new char[bytes.length<<1];
        for(int i=0,j=0;i<bytes.length;i++){
            int v = bytes[i]&0xFF;
            c[j++] = HEX[v>>>4];
            c[j++] = HEX[v&0x0F];
        }
        return new String(c);
    }
}

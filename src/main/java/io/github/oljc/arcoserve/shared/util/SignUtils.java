package io.github.oljc.arcoserve.shared.util;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

/**
 * 现代简洁高性能签名验证工具
 */
public final class SignUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private static final Set<String> IGNORED_HEADERS = Set.of(
            "authorization", "content-type", "content-length", "user-agent", "accept-encoding",
            "accept-language", "cache-control", "connection", "host", "origin", "pragma", "referer",
            "sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform", "sec-fetch-dest", "sec-fetch-mode",
            "sec-fetch-site", "expect"
    );

    public record SignatureData(String date, String shortDate, String fingerprint, String signature) {}
    public record HeaderData(String signedHeaders, String canonicalHeaders) {}
    public record RequestData(String method, String uri, Map<String, String[]> query, byte[] body, String bodyHash) {}

    private SignUtils() {}

    /**
     * 验签入口
     *
     * @param request 请求
     * @param secretKey 签名密钥（Base64或明文）
     * @throws SecurityException 验签失败异常
     */
    public static void verify(HttpServletRequest request, String secretKey) {
        try {
            var sigData = extractSignatureData(request);
            var headers = getHeadersMap(request);
            var headerData = buildHeaderData(headers);
            var requestData = buildRequestData(request);

            var canonicalRequest = buildCanonicalRequest(requestData, headerData);
            var hashedCanonicalRequest = hashSHA256(canonicalRequest.getBytes(UTF_8));

            var scope = "%s/%s/oljc".formatted(sigData.shortDate(), sigData.fingerprint());
            var stringToSign = String.join("\n",
                    "LJC-HMAC-SHA256",
                    sigData.date(),
                    scope,
                    hashedCanonicalRequest);

            var expectedSignature = calculateSignature(secretKey, sigData, stringToSign);

            if (!expectedSignature.equals(sigData.signature())) {
                throw new SecurityException("签名不匹配");
            }
        } catch (Exception e) {
            if (e instanceof SecurityException se) throw se;
            throw new SecurityException("验签异常", e);
        }
    }

    private static SignatureData extractSignatureData(HttpServletRequest request) {
        var date = Objects.requireNonNull(request.getHeader("x-date"), "缺少x-date");
        var shortDate = date.substring(0, 8);
        var fingerprint = Objects.requireNonNull(request.getHeader("x-fingerprint"), "缺少x-fingerprint");
        var authorization = Objects.requireNonNull(request.getHeader("authorization"), "缺少authorization");
        var signature = extractSignatureOnly(authorization);
        return new SignatureData(date, shortDate, fingerprint, signature);
    }

    private static RequestData buildRequestData(HttpServletRequest request) throws Exception {
        var method = request.getMethod();
        var uri = Optional.ofNullable(request.getRequestURI()).orElse("/");
        var query = request.getParameterMap();
        var body = request.getInputStream().readAllBytes();
        var contentType = Optional.ofNullable(request.getContentType()).orElse("");
        var bodyHash = getHexEncodedBodyHash(body, contentType);
        return new RequestData(method, uri, query, body, bodyHash);
    }

    private static String buildCanonicalRequest(RequestData req, HeaderData headers) {
        var canonicalQueryString = canonicalizeQueryMap(req.query());
        var uri = req.uri().startsWith("/") ? req.uri() : "/" + req.uri();

        return String.join("\n",
                req.method().toUpperCase(),
                uri,
                canonicalQueryString,
                headers.canonicalHeaders(),
                "",
                headers.signedHeaders(),
                req.bodyHash());
    }

    private static String calculateSignature(String secretKey, SignatureData sigData, String stringToSign) throws Exception {
        var kDate = hmacSHA256(secretKey.getBytes(UTF_8), sigData.date().getBytes(UTF_8));
        var kFingerprint = hmacSHA256(kDate.getBytes(UTF_8), sigData.fingerprint().getBytes(UTF_8));
        var kSigning = hmacSHA256(kFingerprint.getBytes(UTF_8), "oljc".getBytes(UTF_8));
        return hmacSHA256(kSigning.getBytes(UTF_8), stringToSign.getBytes(UTF_8));
    }

    private static String canonicalizeQueryMap(Map<String, String[]> queryParams) {
        if (queryParams.isEmpty()) return "";
        return queryParams.entrySet().stream()
                .flatMap(e -> {
                    var key = uriEscape(e.getKey());
                    return Arrays.stream(e.getValue())
                            .filter(Objects::nonNull)
                            .map(SignUtils::uriEscape)
                            .sorted()
                            .map(val -> key + "=" + val);
                })
                .sorted()
                .collect(joining("&"));
    }

    private static String uriEscape(String str) {
        try {
            return URLEncoder.encode(str, UTF_8)
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            return "";
        }
    }

    private static String extractSignatureOnly(String authorization) {
        if (!authorization.startsWith("LJC-HMAC-SHA256 ")) {
            throw new SecurityException("无效的Authorization格式");
        }
        var sigKey = "Signature=";
        var idx = authorization.indexOf(sigKey);
        if (idx == -1) {
            throw new SecurityException("Authorization缺少Signature");
        }
        var sig = authorization.substring(idx + sigKey.length());
        return sig.endsWith(",") ? sig.substring(0, sig.length() - 1).trim() : sig.trim();
    }

    private static String getHexEncodedBodyHash(byte[] body, String contentType) {
        try {
            if (body == null || body.length == 0) return hashSHA256(new byte[0]);
            var lowerType = contentType.toLowerCase(Locale.ROOT);

            if (lowerType.contains("application/json")) {
                var normalized = normalizeJsonString(new String(body, UTF_8));
                return hashSHA256(normalized.getBytes(UTF_8));
            }
            if (lowerType.contains("application/x-www-form-urlencoded")) {
                var queryMap = parseQueryString(new String(body, UTF_8));
                return hashSHA256(canonicalizeQueryMap(queryMap).getBytes(UTF_8));
            }
            return hashSHA256(body);
        } catch (Exception e) {
            throw new RuntimeException("计算请求体Hash失败", e);
        }
    }

    private static Map<String, String[]> parseQueryString(String query) {
        var map = new TreeMap<String, List<String>>();
        for (var pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            var kv = pair.split("=", 2);
            var key = uriEscape(kv[0]);
            var val = kv.length > 1 ? uriEscape(kv[1]) : "";
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
        }
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(String[]::new), (a,b)->a, TreeMap::new));
    }

    private static String normalizeJsonString(String json) {
        var trimmed = json.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                ? trimmed.replaceAll("\\s+", " ")
                : json;
    }

    public static String hashSHA256(byte[] data) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("SHA256计算失败", e);
        }
    }

    public static String hmacSHA256(byte[] key, byte[] message) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message));
        } catch (Exception e) {
            throw new RuntimeException("HMAC计算失败", e);
        }
    }

    private static HeaderData buildHeaderData(Map<String, String> headers) {
        var filtered = headers.keySet().stream()
                .filter(k -> !IGNORED_HEADERS.contains(k.toLowerCase(Locale.ROOT)))
                .map(String::toLowerCase)
                .sorted()
                .toList();

        var signedHeaders = String.join(";", filtered);

        var canonicalHeaders = filtered.stream()
                .map(k -> k + ":" + trimHeader(headers.get(k)))
                .collect(joining("\n"));

        return new HeaderData(signedHeaders, canonicalHeaders);
    }

    private static String trimHeader(String header) {
        return Objects.toString(header, "").trim().replaceAll("\\s+", " ");
    }

    public static Map<String, String> getHeadersMap(HttpServletRequest req) {
        var map = new HashMap<String, String>();
        var names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            var name = names.nextElement();
            map.put(name, req.getHeader(name));
        }
        return map;
    }

    public static boolean isExpired(String xDate, long maxAgeSeconds) {
        var time = parseDate(xDate);
        return time == null || Instant.now().isAfter(time.plusSeconds(maxAgeSeconds));
    }

    private static Instant parseDate(String xDate) {
        try {
            return Instant.from(FORMATTER.parse(xDate));
        } catch (Exception e) {
            return null;
        }
    }
}

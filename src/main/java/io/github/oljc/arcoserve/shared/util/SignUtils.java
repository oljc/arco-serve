package io.github.oljc.arcoserve.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Java 21原生高性能签名工具类，零外部依赖，充分利用JDK内置能力
 */
public interface SignUtils {

    Logger log = LoggerFactory.getLogger(SignUtils.class);

    // 算法常量
    String HMAC_SHA256 = "HmacSHA256";
    String SHA256 = "SHA-256";
    DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    // 字符集
    String UTF8 = StandardCharsets.UTF_8.name();

    // GET请求空Body的SHA256哈希值
    String EMPTY_BODY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    /**
     * 生成完整签名信息
     *
     * @param method HTTP方法
     * @param host 请求主机
     * @param path 请求路径
     * @param queryParams 查询参数
     * @param headers 请求头
     * @param body 请求体
     * @param contentType 请求体类型
     * @param secretKey 密钥
     * @return 签名结果记录
     */
    static SignatureResult generateSignature(
        String method,
        String host,
        String path,
        Map<String, String> queryParams,
        Map<String, String> headers,
        String body,
        String contentType,
        String secretKey
    ) {
        try {
            // 1. 准备时间戳
            String timestamp = ISO_FORMATTER.format(Instant.now());

            // 2. 构建标准请求
            String canonicalRequest = buildCanonicalRequest(
                method, path, queryParams, headers, body, contentType, host, timestamp
            );

            // 3. 计算签名
            String signature = hmacSha256Hex(canonicalRequest, secretKey);

            return new SignatureResult(signature, timestamp, canonicalRequest);
        } catch (Exception e) {
            log.error("签名生成失败", e);
            throw new RuntimeException("签名生成失败", e);
        }
    }

    /**
     * 验证签名
     *
     * @param expectedSignature 期望签名
     * @param method HTTP方法
     * @param host 请求主机
     * @param path 请求路径
     * @param queryParams 查询参数
     * @param headers 请求头
     * @param body 请求体
     * @param contentType 请求体类型
     * @param timestamp 时间戳
     * @param secretKey 密钥
     * @return 验证结果
     */
    static boolean verifySignature(
        String expectedSignature,
        String method,
        String host,
        String path,
        Map<String, String> queryParams,
        Map<String, String> headers,
        String body,
        String contentType,
        String timestamp,
        String secretKey
    ) {
        try {
            // 重新构建标准请求
            String canonicalRequest = buildCanonicalRequest(
                method, path, queryParams, headers, body, contentType, host, timestamp
            );

            // 计算签名
            String actualSignature = hmacSha256Hex(canonicalRequest, secretKey);

            // 安全比较
            return constantTimeEquals(expectedSignature, actualSignature);
        } catch (Exception e) {
            log.error("签名验证失败", e);
            return false;
        }
    }

    /**
     * 构建标准请求字符串
     */
    private static String buildCanonicalRequest(
        String method,
        String path,
        Map<String, String> queryParams,
        Map<String, String> headers,
        String body,
        String contentType,
        String host,
        String timestamp
    ) {
        // 1. HTTP方法（大写）
        String httpMethod = method.toUpperCase();

        // 2. 规范URI
        String canonicalURI = path.startsWith("/") ? path : "/" + path;

        // 3. 规范查询字符串
        String canonicalQueryString = buildCanonicalQueryString(queryParams);

        // 4. 规范请求头
        Map<String, String> canonicalHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        canonicalHeaders.put("host", host.toLowerCase());
        canonicalHeaders.put("content-type", contentType);
        canonicalHeaders.put("x-timestamp", timestamp);

        // 添加额外头部（如果有）
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (key != null && value != null) {
                    canonicalHeaders.put(key.toLowerCase(), value.trim());
                }
            });
        }

        // 5. 构建规范头部字符串
        String canonicalHeadersString = canonicalHeaders.entrySet().stream()
            .map(entry -> entry.getKey() + ":" + entry.getValue())
            .collect(Collectors.joining("\n"));

        // 6. 签名头部列表
        String signedHeaders = String.join(";", canonicalHeaders.keySet());

        // 7. 负载哈希
        String payloadHash = "GET".equals(httpMethod) && (body == null || body.isEmpty()) ?
            EMPTY_BODY_HASH : sha256Hex(body != null ? body : "");

        // 8. 构建完整的标准请求
        return String.join("\n",
            httpMethod,
            canonicalURI,
            canonicalQueryString,
            canonicalHeadersString,
            signedHeaders,
            payloadHash
        );
    }

    /**
     * 构建标准查询字符串
     */
    private static String buildCanonicalQueryString(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }

        return queryParams.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
            .sorted()
            .collect(Collectors.joining("&"));
    }

    /**
     * URL编码 - 符合RFC 3986
     */
    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, UTF8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
        } catch (Exception e) {
            log.error("URL编码失败", e);
            return value;
        }
    }

    /**
     * 常量时间字符串比较 - 防止时序攻击
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * 创建Authorization头部
     */
    static String createAuthorizationHeader(
        String algorithm,
        String accessKey,
        String signedHeaders,
        String signature
    ) {
        return String.format("%s AccessKey=%s, SignedHeaders=%s, Signature=%s",
            algorithm, accessKey, signedHeaders, signature);
    }

    /**
     * 解析Authorization头部
     */
    static Map<String, String> parseAuthorizationHeader(String authorization) {
        Map<String, String> result = new TreeMap<>();

        if (authorization == null || authorization.trim().isEmpty()) {
            return result;
        }

        try {
            String[] parts = authorization.split("\\s+", 2);
            if (parts.length != 2) {
                return result;
            }

            result.put("algorithm", parts[0]);

            String[] params = parts[1].split(",");
            for (String param : params) {
                String[] kv = param.trim().split("=", 2);
                if (kv.length == 2) {
                    result.put(kv[0].trim(), kv[1].trim());
                }
            }
        } catch (Exception e) {
            log.error("解析Authorization头部失败", e);
        }

        return result;
    }

    /**
     * 验证时间戳有效性
     */
    static boolean isTimestampValid(String timestamp, long maxAgeSeconds) {
        try {
            Instant requestTime = Instant.from(ISO_FORMATTER.parse(timestamp));
            Instant now = Instant.now();
            long ageSeconds = now.getEpochSecond() - requestTime.getEpochSecond();
            return Math.abs(ageSeconds) <= maxAgeSeconds;
        } catch (Exception e) {
            log.error("时间戳验证失败", e);
            return false;
        }
    }

    /**
     * HMAC-SHA256计算并转换为十六进制
     */
    private static String hmacSha256Hex(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            log.error("HMAC-SHA256计算失败", e);
            throw new RuntimeException("HMAC-SHA256计算失败", e);
        }
    }

    /**
     * SHA256哈希并转换为十六进制
     */
    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            log.error("SHA256计算失败", e);
            throw new RuntimeException("SHA256计算失败", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 签名结果记录
     */
    record SignatureResult(
        String signature,
        String timestamp,
        String canonicalRequest
    ) {}
}

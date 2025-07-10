package io.github.oljc.arcoserve.shared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StreamUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class SignUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final String ACCESS_KEY_ID = "LJCDEMOYmE1MTU5OTBmNDg5ODlhNTQzMGUwY2YLJCDEMO";
    private static final String SECRET_KEY = "LJCVd05qVXdObU0yT0RaaU5HWTJORGs0TURNek5HTTJZakV6WTJNNE9XVQ";
    private static final Set<String> IGNORED_HEADERS = Set.of(
        "authorization", "content-type", "content-length", "user-agent", "expect", "presigned-expires"
    );

    private SignUtils() {}

    /**
     * 验签
     */
    public static void verify(HttpServletRequest request) throws Exception {
        String date = request.getHeader("x-date");
        String shortDate = date.substring(0, 8);
        String fingerprint = request.getHeader("x-fingerprint");
        String auth = request.getHeader("authorization");

        String signature = extractSignatureOnly(auth);

        var signHeaders = getRequiredSignHeaders(request);
        String canonicalHeaders = signHeaders.canonicalHeaders();
        String signedHeaders = signHeaders.signedHeaders();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query == null) query = "";

        // 读取请求体
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        String bodyHash = getHexEncodedBodyHash(body, request.getContentType());

        // 规范化查询参数
        String canonicalQuery = canonicalizeQueryString(query);

        // 构造canonical request
        String canonicalRequest = method + "\n" +
                (uri.startsWith("/") ? uri : "/" + uri) + "\n" +
                canonicalQuery + "\n" +
                canonicalHeaders + "\n" +
                signedHeaders + "\n" +
                bodyHash;

        String hashedCanonicalRequest = hashSHA256(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        String scope = shortDate + "/" + fingerprint + "/oljc";
        String credential = ACCESS_KEY_ID + "/" + scope;
        String stringToSign = "LJC-HMAC-SHA256\n" + date + "\n" + scope + "\n" + hashedCanonicalRequest;

        // 计算期望的签名
        byte[] kDate = hmacSHA256(SECRET_KEY.getBytes(StandardCharsets.UTF_8), date);
        byte[] kFingerprint = hmacSHA256(kDate, fingerprint);
        byte[] kSigning = hmacSHA256(kFingerprint, "oljc");
        byte[] sign = hmacSHA256(kSigning, stringToSign);
        String expectedSignature = HexFormat.of().formatHex(sign);

        // 比较签名
        if (!expectedSignature.equals(signature)) {
            throw new SecurityException("Signature mismatch");
        }
    }

    /**
     * 验证是否过期
     */
    public static boolean isExpired(String xDate, long maxAge) {
        try {
            Instant time = Instant.from(FORMATTER.parse(xDate));
            return Instant.now().isAfter(time.plusSeconds(maxAge));
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 仅提取签名部分，不信任其他字段
     */
    private static String extractSignatureOnly(String authorization) {
        if (!authorization.startsWith("LJC-HMAC-SHA256 ")) {
            throw new SecurityException("Invalid authorization format");
        }

        // 查找Signature=部分
        String signaturePart = "Signature=";
        int signatureIndex = authorization.indexOf(signaturePart);
        if (signatureIndex == -1) {
            throw new SecurityException("Missing signature in authorization");
        }

        String signature = authorization.substring(signatureIndex + signaturePart.length());
        // 移除可能的尾部逗号
        if (signature.endsWith(",")) {
            signature = signature.substring(0, signature.length() - 1);
        }

        return signature.trim();
    }

    /**
     * 获取服务端要求的签名头部 - 使用安全策略，不信任前端
     */
    private static SignHeaders getRequiredSignHeaders(HttpServletRequest request) {

        // 获取所有可签名的头部（排除被忽略的）
        Map<String, String> availableHeaders = Collections.list(request.getHeaderNames())
                .stream()
                .filter(name -> !IGNORED_HEADERS.contains(name.toLowerCase()))
                .collect(Collectors.toMap(
                        name -> name.toLowerCase(),
                        request::getHeader,
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));

        // 构造规范头部
        String canonicalHeaders = availableHeaders.entrySet()
                .stream()
                .map(entry -> entry.getKey() + ":" + trimHeader(entry.getValue()))
                .collect(Collectors.joining("\n"));

        String signedHeaders = String.join(";", availableHeaders.keySet());

        return new SignHeaders(canonicalHeaders, signedHeaders);
    }



    /**
     * 清理头部值
     */
    private static String trimHeader(String header) {
        return header == null ? "" : header.trim().replaceAll("\\s+", " ");
    }

    /**
     * 规范化查询参数 - 匹配前端uriEscape实现
     */
    private static String canonicalizeQueryString(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }

        Map<String, List<String>> params = new TreeMap<>();
        Arrays.stream(query.split("&"))
                .filter(param -> !param.isEmpty())
                .forEach(param -> {
                    String[] pair = param.split("=", 2);
                    String key = uriEscape(pair[0]);
                    String value = pair.length > 1 ? uriEscape(pair[1]) : "";

                    if (!key.isEmpty()) {
                        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                    }
                });

        return params.entrySet()
                .stream()
                .map(entry -> {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();

                    if (values.size() == 1) {
                        return key + "=" + values.get(0);
                    } else {
                        // 对于多值参数，按值排序后用&key=格式连接
                        values.sort(String::compareTo);
                        return values.stream()
                                .map(value -> key + "=" + value)
                                .collect(Collectors.joining("&"));
                    }
                })
                .collect(Collectors.joining("&"));
    }

    /**
     * URI转义 - 匹配前端uriEscape实现
     */
    private static String uriEscape(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8)
                    .replace("+", "%20")  // 空格用%20而不是+
                    .replace("*", "%2A")
                    .replace("%7E", "~"); // 保留~字符不编码
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 计算请求体哈希 - 匹配前端hexEncodedBodyHash实现
     */
    private static String getHexEncodedBodyHash(byte[] body, String contentType) throws Exception {
        if (body == null || body.length == 0) {
            return hashSHA256(new byte[0]);
        }

        String bodyString = new String(body, StandardCharsets.UTF_8);

        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();

            if (lowerContentType.contains("application/json")) {
                // 对于JSON数据，尝试重新排序键以确保一致性
                try {
                    return hashSHA256(normalizeJsonString(bodyString).getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    // 如果JSON解析失败，使用原始数据
                    return hashSHA256(body);
                }
            }

            if (lowerContentType.contains("application/x-www-form-urlencoded")) {
                // 对于表单数据，规范化参数顺序
                return hashSHA256(canonicalizeQueryString(bodyString).getBytes(StandardCharsets.UTF_8));
            }
        }

        return hashSHA256(body);
    }

    /**
     * 规范化JSON字符串（简化版本，仅排序顶级键）
     */
    private static String normalizeJsonString(String jsonString) {
        try {
            // 这里使用简单的字符串处理，实际项目中可能需要使用JSON库
            // 为了避免引入额外依赖，这里做简化处理
            jsonString = jsonString.trim();
            if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                // 简单处理：移除多余空格
                return jsonString.replaceAll("\\s+", " ");
            }
            return jsonString;
        } catch (Exception e) {
            return jsonString;
        }
    }

    /**
     * SHA-256哈希
     */
    public static String hashSHA256(byte[] data) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new Exception("SHA-256 hash failed", e);
        }
    }

    public static byte[] hmacSHA256(byte[] key, String data) throws Exception {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new Exception("HMAC-SHA256 failed", e);
        }
    }

    /**
     * 签名头部信息记录
     */
    private record SignHeaders(String canonicalHeaders, String signedHeaders) {}
}

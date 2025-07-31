package io.github.oljc.arcoserve.modules.auth;

import cn.hutool.captcha.*;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String NUMBERS = "0123456789";
    private static final CaptchaType[] RANDOM_TYPES = {CaptchaType.LINE, CaptchaType.CIRCLE, CaptchaType.SHEAR};

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.captcha.width:200}")
    private int width;

    @Value("${app.captcha.height:100}")
    private int height;

    @Value("${app.captcha.code-count:5}")
    private int codeCount;

    @Value("${app.captcha.interfere-count:20}")
    private int interfereCount;

    @Value("${app.captcha.expiration-minutes:5}")
    private int expirationMinutes;

    public enum CaptchaType {
        LINE, CIRCLE, SHEAR, MATH, RANDOM, NUMBER
    }

    public record CaptchaResponse(String id, String captcha, CaptchaType type, long ttlSeconds) {}

    public record Result(boolean success, String message) {
        public static Result ok() {
            return new Result(true, "验证成功");
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }

    /**
     * 生成验证码
     */
    public CaptchaResponse create(CaptchaType type) {
        AbstractCaptcha captcha = createCaptcha(type);
        String captchaId = IdUtil.fastSimpleUUID();
        String captchaCode = captcha.getCode();

        // 异步存储到Redis
        String redisKey = CAPTCHA_PREFIX + captchaId;
        redisTemplate.opsForValue().set(redisKey, captchaCode, Duration.ofMinutes(expirationMinutes));
        return new CaptchaResponse(captchaId, captcha.getImageBase64Data(), type, expirationMinutes * 60L);
    }

    /**
     * 验证验证码
     */
    public Result verify(String captchaId, String captchaCode) {
        if (StrUtil.hasBlank(captchaId, captchaCode)) {
            return Result.fail("参数不能为空");
        }

        try {
            String redisKey = CAPTCHA_PREFIX + captchaId;
            String storedCode = redisTemplate.opsForValue().getAndDelete(redisKey);

            if (storedCode == null) {
                return Result.fail("验证码不存在或已过期");
            }

            boolean isValid = storedCode.equalsIgnoreCase(captchaCode.trim());
            return isValid ? Result.ok() : Result.fail("验证码错误");

        } catch (Exception e) {
            return Result.fail("系统异常");
        }
    }

    /**
     * 创建验证码实例
     */
    private AbstractCaptcha createCaptcha(CaptchaType type) {
        return switch (type) {
            case LINE -> CaptchaUtil.createLineCaptcha(width, height, codeCount, interfereCount);
            case CIRCLE -> CaptchaUtil.createCircleCaptcha(width, height, codeCount, interfereCount);
            case SHEAR -> CaptchaUtil.createShearCaptcha(width, height, codeCount, 4);
            case MATH -> createMathCaptcha();
            case NUMBER -> createNumberCaptcha();
            case RANDOM -> createCaptcha(RANDOM_TYPES[ThreadLocalRandom.current().nextInt(RANDOM_TYPES.length)]);
        };
    }

    private AbstractCaptcha createMathCaptcha() {
        var captcha = CaptchaUtil.createLineCaptcha(width, height);
        captcha.setGenerator(new MathGenerator(1));
        return captcha;
    }

    private AbstractCaptcha createNumberCaptcha() {
        var captcha = CaptchaUtil.createLineCaptcha(width, height, codeCount, interfereCount);
        captcha.setGenerator(new RandomGenerator(NUMBERS, codeCount));
        return captcha;
    }
}

package io.github.oljc.arcoserve.modules.auth;

import cn.hutool.captcha.*;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.oljc.arcoserve.shared.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String PREFIX = "captcha:";
    private static final String NUMBERS = "0123456789";

    private static final CaptchaType[] ALL_TYPES = {
        CaptchaType.LINE,
        CaptchaType.CIRCLE,
        CaptchaType.SHEAR,
        CaptchaType.MATH,
        CaptchaType.NUMBER
    };

    private final RedisUtils redisUtils;

    @Value("${app.captcha.width:200}")
    private int width;

    @Value("${app.captcha.height:100}")
    private int height;

    @Value("${app.captcha.code-count:5}")
    private int codeCount;

    @Value("${app.captcha.interfere-count:20}")
    private int interfereCount;

    @Value("${app.captcha.expiration-minutes:50}")
    private int expire;

    public enum CaptchaType {
        LINE, CIRCLE, SHEAR, MATH, NUMBER
    }

    public record Response(String id, String captcha, long ttl) {}

    public record Result(boolean success, String message) {
        public static Result ok() {
            return new Result(true, "验证成功");
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }

    /**
     * 随机生成验证码
     */
    public Response random() {
        CaptchaType randomType = ALL_TYPES[ThreadLocalRandom.current().nextInt(ALL_TYPES.length)];
        return create(randomType);
    }

    /**
     * 生成指定类型验证码
     */
    public Response create(CaptchaType type) {
        AbstractCaptcha captcha = createCaptcha(type);
        String captchaId = IdUtil.fastSimpleUUID();
        String captchaCode = captcha.getCode();
        String key = PREFIX + captchaId;

        redisUtils.set(key, captchaCode, expire);
        return new Response(captchaId, captcha.getImageBase64Data(), expire);
    }

    /**
     * 验证验证码
     */
    public Result verify(String captchaId, String captchaCode) {
        if (StrUtil.hasBlank(captchaId, captchaCode)) {
            return Result.fail("参数不能为空");
        }

        try {
            String key = PREFIX + captchaId;
            String storedCode = redisUtils.get(key);

            if (storedCode == null) {
                return Result.fail("验证码不存在或已过期");
            }

            redisUtils.delete(key);

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

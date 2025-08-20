package io.github.oljc.arcoserve.modules.auth;

import cn.hutool.captcha.*;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.oljc.arcoserve.shared.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.random.RandomGeneratorFactory;

/**
 * 验证码服务
 */
@Service
@RequiredArgsConstructor
public final class CaptchaService implements InitializingBean, DisposableBean {

    private static final String PREFIX = "captcha:";
    private static final String NUMBERS = "0123456789";

    private static final java.util.random.RandomGenerator RANDOM =
        RandomGeneratorFactory.of("Xoshiro256PlusPlus").create();

    private static final CaptchaType[] TYPES = CaptchaType.values();

    private final RedisUtils redisUtils;

    @Value("${app.captcha.width:120}")
    private int width;
    @Value("${app.captcha.height:60}")
    private int height;
    @Value("${app.captcha.code-count:5}")
    private int codeCount;
    @Value("${app.captcha.interfere-count:6}")
    private int interfereCount;
    @Value("${app.captcha.expiration-seconds:60}")
    private int expire;
    @Value("${app.captcha.pool-size:512}")
    private int poolSize;

    private final ConcurrentLinkedQueue<PreGeneratedCaptcha> captchaPool = new ConcurrentLinkedQueue<>();
    private final ExecutorService refillExecutor = Executors.newSingleThreadExecutor(
        r -> {
            Thread t = new Thread(r, "captcha-refill");
            t.setDaemon(true);
            return t;
        }
    );
    private final AtomicBoolean isRefilling = new AtomicBoolean(false);

    public record PreGeneratedCaptcha(String code, String imageBase64) {}

    public enum CaptchaType {
        LINE, CIRCLE, SHEAR, MATH, NUMBER
    }

    public record Response(String id, String captcha, long ttl) {}

    public sealed interface Result permits Result.Success, Result.Failure {
        record Success(String message) implements Result {
            public Success() { this("验证成功"); }
        }
        record Failure(String message) implements Result {}

        static Result ok() { return new Success(); }
        static Result fail(String message) { return new Failure(message); }
    }

    @Override
    public void afterPropertiesSet() {
        refillPool();
    }

    @Override
    public void destroy() {
        refillExecutor.shutdown();
        try {
            if (!refillExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                refillExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            refillExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 生成验证码
     */
    public Response random() {
        PreGeneratedCaptcha preGenerated = captchaPool.poll();
        if (preGenerated != null) {
            return buildResponse(preGenerated.code(), preGenerated.imageBase64());
        }

        if (isRefilling.compareAndSet(false, true)) {
            refillExecutor.submit(this::refillPool);
        }

        var captcha = createRandomCaptcha();
        return buildResponse(captcha.getCode(), captcha.getImageBase64Data());
    }

    /**
     * 验证验证码
     */
    public Result verify(String captchaId, String captchaCode) {
        if (StrUtil.hasBlank(captchaId, captchaCode)) {
            return Result.fail("参数不能为空");
        }

        var key = PREFIX + captchaId;
        var storedCode = redisUtils.get(key);

        if (storedCode == null) {
            return Result.fail("验证码不存在或已过期");
        }

        redisUtils.delete(key);

        return storedCode.equalsIgnoreCase(captchaCode.trim())
            ? Result.ok()
            : Result.fail("验证码错误");
    }

    private Response buildResponse(String code, String imageBase64) {
        var captchaId = IdUtil.fastSimpleUUID();
        redisUtils.set(PREFIX + captchaId, code, expire);
        return new Response(captchaId, imageBase64, expire);
    }

    private void refillPool() {
        try {
            while (captchaPool.size() < poolSize) {
                var captcha = createRandomCaptcha();
                var preGenerated = new PreGeneratedCaptcha(captcha.getCode(), captcha.getImageBase64Data());
                captchaPool.offer(preGenerated);
            }
        } finally {
            isRefilling.set(false);
        }
    }

    private AbstractCaptcha createRandomCaptcha() {
        var type = TYPES[RANDOM.nextInt(TYPES.length)];
        return switch (type) {
            case LINE -> CaptchaUtil.createLineCaptcha(width, height, codeCount, interfereCount);
            case CIRCLE -> CaptchaUtil.createCircleCaptcha(width, height, codeCount, interfereCount);
            case SHEAR -> CaptchaUtil.createShearCaptcha(width, height, codeCount, 4);
            case MATH -> {
                var captcha = CaptchaUtil.createLineCaptcha(width, height);
                captcha.setGenerator(new MathGenerator(1));
                yield captcha;
            }
            case NUMBER -> {
                var captcha = CaptchaUtil.createLineCaptcha(width, height, codeCount, interfereCount);
                captcha.setGenerator(new RandomGenerator(NUMBERS, codeCount));
                yield captcha;
            }
        };
    }
}

package io.github.oljc.arcoserve.modules.auth;

import io.github.oljc.arcoserve.shared.annotation.RateLimit;
import io.github.oljc.arcoserve.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaService captchaService;

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    @RateLimit(limit = 10, window = 60, message = "验证码获取过于频繁，请稍后再试")
    public ApiResponse<CaptchaService.Response> getCaptcha() {
        CaptchaService.Response captcha = captchaService.random();
        return ApiResponse.success(captcha, "获取验证码成功");
    }

    /**
     * 临时 Post 接口，用于测试
     */
    @PostMapping("/demo")
    public ApiResponse<String> test(@RequestBody String body) {
        return ApiResponse.success(body, "测试成功");
    }
}

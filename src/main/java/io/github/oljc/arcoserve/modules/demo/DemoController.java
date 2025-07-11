package io.github.oljc.arcoserve.modules.demo;

import io.github.oljc.arcoserve.shared.response.ApiResponse;
import io.github.oljc.arcoserve.shared.annotation.Signature;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo控制器 - 提供基本的测试接口
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * 获取当前时间 - 需要签名验证
     */
    @GetMapping("/time")
    @Signature(maxAge = 300)
    public ApiResponse<Map<String, Object>> getCurrentTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("currentTime", LocalDateTime.now());
        data.put("timestamp", System.currentTimeMillis());
        data.put("message", "服务器时间");

        return ApiResponse.success(data, "获取时间成功");
    }
}

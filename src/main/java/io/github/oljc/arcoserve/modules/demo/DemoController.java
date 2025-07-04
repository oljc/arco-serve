package io.github.oljc.arcoserve.modules.demo;

import io.github.oljc.arcoserve.shared.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo控制器 - 提供基本的测试接口
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * 简单的Hello World接口
     */
    @GetMapping("/hello")
    public ApiResponse<String> hello() {
        return ApiResponse.success("Hello World! 项目启动成功！", "Hello接口调用成功");
    }

    /**
     * 获取当前时间
     */
    @GetMapping("/time")
    public ApiResponse<Map<String, Object>> getCurrentTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("currentTime", LocalDateTime.now());
        data.put("timestamp", System.currentTimeMillis());
        data.put("message", "当前服务器时间");

        return ApiResponse.success(data, "获取时间成功");
    }

    /**
     * 模拟用户列表
     */
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> getUsers() {
        List<Map<String, Object>> users = List.of(
            Map.of("id", 1, "name", "张三", "email", "zhangsan@example.com"),
            Map.of("id", 2, "name", "李四", "email", "lisi@example.com"),
            Map.of("id", 3, "name", "王五", "email", "wangwu@example.com")
        );

        return ApiResponse.success(users, "获取用户列表成功");
    }

    /**
     * 接收POST请求的示例
     */
    @PostMapping("/echo")
    public ApiResponse<Map<String, Object>> echo(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("received", requestData);
        responseData.put("processedAt", LocalDateTime.now());
        responseData.put("message", "数据接收成功");

        return ApiResponse.success(responseData, "Echo 请求处理成功");
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("service", "arco-serve");
        healthData.put("version", "0.0.1-SNAPSHOT");
        healthData.put("uptime", System.currentTimeMillis());

        return ApiResponse.success(healthData, "服务健康状态良好");
    }
}

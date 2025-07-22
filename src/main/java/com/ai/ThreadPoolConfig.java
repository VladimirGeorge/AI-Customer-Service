package com.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数，正确使用int类型
        executor.setCorePoolSize(5);
        // 设置最大线程数，int类型
        executor.setMaxPoolSize(10);
        // 设置队列容量，int类型
        executor.setQueueCapacity(100);
        // 设置线程名称前缀
        executor.setThreadNamePrefix("ai-thread-");
        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 设置线程空闲后的最大存活时间，避免线程资源浪费
        executor.setKeepAliveSeconds(60);
        // 设置线程池关闭时的等待策略，确保任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 设置线程池关闭时等待任务完成的最大时间
        executor.setAwaitTerminationSeconds(60);
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}

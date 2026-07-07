package backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class JobExecutionConfig {
    // This allows us to run up to 2 simulation jobs concurrently
    @Bean
    fun simulationJobExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 2
        executor.setQueueCapacity(50)
        executor.setThreadNamePrefix("sim-worker-")
        executor.initialize()
        return executor
    }
}
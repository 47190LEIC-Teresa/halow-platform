package backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class JobExecutionConfig(
    @Value("\${simulator.worker-threads:2}")
    private val workerThreads: Int
) {
    // This allows us to run up to 2 simulation jobs concurrently
    @Bean
    fun simulationJobExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = workerThreads
        executor.maxPoolSize = workerThreads
        executor.setQueueCapacity(50)
        executor.setThreadNamePrefix("sim-worker-")
        executor.initialize()
        return executor
    }
}
package backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class SimulationExecutorConfig {

    @Bean("simulationExecutor")
    fun simulationExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 2
            queueCapacity = 0
            setThreadNamePrefix("simulation-")
            setWaitForTasksToCompleteOnShutdown(true)
            initialize()
        }
    }
}
package com.iodice.mediasearch.di

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.util.concurrent.Executor
import java.util.concurrent.Executors


/**
 * Enables parallel task execution through an executor scheduler
 */
@Configuration
@EnableScheduling
class SchedulingConfig : SchedulingConfigurer {
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor())
    }

    @Bean(destroyMethod = "shutdown")
    fun taskExecutor(): Executor {
        return Executors.newScheduledThreadPool(10)
    }
}

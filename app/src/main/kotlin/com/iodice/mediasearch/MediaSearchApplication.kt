package com.iodice.mediasearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer




@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = ["com.iodice.mediasearch"])
class MediaSearchApplication


fun main(args: Array<String>) {
    runApplication<MediaSearchApplication>(*args)
}

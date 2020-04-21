package com.iodice.mediasearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling


@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = ["com.iodice.mediasearch"])
class MediaSearchApplication


fun main(args: Array<String>) {
    runApplication<MediaSearchApplication>(*args)
}

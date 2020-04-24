package com.iodice.mediasearch.client

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Component
class FFMPEGClient {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    private fun runCommand(args: Array<String>): String {
        val builder = ProcessBuilder()
        builder.command(*args)
        val process = builder.start()

        if (process.waitFor() != 0) {
            val err = BufferedReader(InputStreamReader(process.errorStream)).readLines().joinToString("\n")
            logger.error("Error processing audio file: cmd was ${args.joinToString(" ")}, error was $err")
            throw Exception("Error processing audio file: $err")
        }
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        return reader.readLines().joinToString("\n")
    }

    fun process(input: File, output: File, bitRate: Int, sampleRate: Int) {
        val inputPath = input.absolutePath
        val outputPath = output.absolutePath
        val cmdOutput = runCommand(arrayOf(
                "ffmpeg", "-y", "-i", inputPath, "-b:a", "$bitRate", "-ar", "$sampleRate", outputPath))
        logger.info("Finished processing audio file: $cmdOutput")
    }
}
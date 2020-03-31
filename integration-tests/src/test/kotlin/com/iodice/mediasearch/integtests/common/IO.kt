package com.iodice.mediasearch.integtests.common

import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

fun readTestResource(path: String): String {
    val root: Path = Paths.get("src", "test", "resources", path)
    return root.toFile().readText(Charset.defaultCharset())
}
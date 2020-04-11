package com.iodice.mediasearch.util

import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

fun <T> Iterator<T>.stream(): Stream<T> {
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(this, 0),
            false)
}

//fun <T> Iterator<T>.parallelStream(): Stream<T> {
//    return StreamSupport.stream(
//            Spliterators.spliteratorUnknownSize(this, 0),
//            true)
//}
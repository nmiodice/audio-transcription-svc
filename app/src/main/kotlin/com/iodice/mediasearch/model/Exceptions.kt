package com.iodice.mediasearch.model

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.Exception

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class NotFoundException(private val msg: String): Exception(msg)
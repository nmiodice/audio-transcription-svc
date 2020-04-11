package com.iodice.mediasearch.model

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class NotFoundException(msg: String) : Exception(msg)

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class InternalServerError(msg: String) : Exception(msg)
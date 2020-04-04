package com.iodice.mediasearch.integtests

import com.iodice.mediasearch.integtests.common.Config
import com.iodice.mediasearch.integtests.common.readTestResource
import kong.unirest.Unirest
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class MediaAPITest {
    @ParameterizedTest
    @ValueSource(strings = [
        Config.ROUTE_SOURCE,
        Config.ROUTE_MEDIA,
        Config.ROUTE_INDEX_RESULT
    ])
    fun `GET for random ID returns 404`(route: String) {
        val id = UUID.randomUUID().toString()
        val response = Unirest.get("${Config.SERVICE_ENDPOINT_BASE}/$route/$id")
                .header("accept", "application/json")
                .asString()

        assertEquals(HttpStatus.SC_NOT_FOUND, response.status)
        assertNotNull(response.body)
        // the error should contain the ID that cannot be found
        assertTrue(response.body.contains(id))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        Config.ROUTE_SOURCE,
        Config.ROUTE_MEDIA,
        Config.ROUTE_INDEX_RESULT
    ])
    fun `POST with no body returns 400`(route: String) {
        val response = Unirest.post("${Config.SERVICE_ENDPOINT_BASE}/$route/")
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.status)
    }

    @ParameterizedTest
    @CsvSource(
            "${Config.ROUTE_SOURCE},requests/source_01.json",
            "${Config.ROUTE_MEDIA},requests/media_01.json",
            "${Config.ROUTE_INDEX_RESULT},requests/index_result_01.json"
    )
    fun `POST, GET and DELETE all work together`(route: String, requestFile: String) {
        // Step (1): POST data, validate the call did not fail
        val request = readTestResource(requestFile)
        val postResponse = Unirest.post("${Config.SERVICE_ENDPOINT_BASE}/$route/")
                .header("content-type", "application/json")
                .header("accept", "application/json")
                .body(request)
                .asJson()
        assertEquals(HttpStatus.SC_OK, postResponse.status)
        JSONAssert.assertEquals(request, postResponse.body.toString(), false);

        // Step (2): GET data, validate it is the correct data
        val id = postResponse.body.`object`.get("id")
        val getResponse = Unirest.get("${Config.SERVICE_ENDPOINT_BASE}/$route/$id")
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_OK, getResponse.status)
        JSONAssert.assertEquals(postResponse.body.toString(), getResponse.body, true);

        // Step (3): DELETE data, validate the call did not fail
        val deleteResponse = Unirest.delete("${Config.SERVICE_ENDPOINT_BASE}/$route/$id")
                .header("accept", "application/json")
                .body(request)
                .asString()
        assertEquals(HttpStatus.SC_OK, deleteResponse.status)

        // Step (4): GET data, validate it does not exist
        val secondGetResponse = Unirest.get("${Config.SERVICE_ENDPOINT_BASE}/$route/$id")
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_NOT_FOUND, secondGetResponse.status)
    }
}
package com.iodice.mediasearch.integtests

import com.iodice.mediasearch.integtests.common.Config
import com.iodice.mediasearch.integtests.common.readTestResource
import kong.unirest.Unirest
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class MediaAPITest {
    @Test
    fun `GET for random ID returns 404`() {
        val id = UUID.randomUUID().toString()
        val response = Unirest.get("${Config.SERVICE_ENDPOINT_MEDIACONFIG}/$id")
                .header("accept", "application/json")
                .asString()

        assertEquals(HttpStatus.SC_NOT_FOUND, response.status)
        assertNotNull(response.body)
        // the error should contain the ID that cannot be found
        assertTrue(response.body.contains(id))
    }

    @Test
    fun `POST with no body returns 400`() {
        val response = Unirest.post(Config.SERVICE_ENDPOINT_MEDIACONFIG)
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.status)
    }

    @Test
    fun `POST, GET and DELETE all work together`() {
        // Step (1): POST data, validate the call did not fail
        val request = readTestResource("requests/post_mediaconfig_01.json")
        val postResponse = Unirest.post(Config.SERVICE_ENDPOINT_MEDIACONFIG)
                .header("content-type", "application/json")
                .header("accept", "application/json")
                .body(request)
                .asJson()
        assertEquals(HttpStatus.SC_OK, postResponse.status)
        JSONAssert.assertEquals(request, postResponse.body.toString(), false);

        // Step (2): GET data, validate it is the correct data
        val id = postResponse.body.`object`.get("id")
        val getResponse = Unirest.get("${Config.SERVICE_ENDPOINT_MEDIACONFIG}/$id")
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_OK, getResponse.status)
        JSONAssert.assertEquals(postResponse.body.toString(), getResponse.body, true);

        // Step (3): DELETE data, validate the call did not fail
        val deleteResponse = Unirest.delete("${Config.SERVICE_ENDPOINT_MEDIACONFIG}/$id")
                .header("accept", "application/json")
                .body(request)
                .asString()
        assertEquals(HttpStatus.SC_OK, deleteResponse.status)

        // Step (4): GET data, validate it does not exist
        val secondGetResponse = Unirest.get("${Config.SERVICE_ENDPOINT_MEDIACONFIG}/$id")
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_NOT_FOUND, secondGetResponse.status)

    }
}
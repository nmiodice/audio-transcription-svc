package com.iodice.mediasearch.integtests

import com.google.gson.JsonParser
import com.iodice.mediasearch.integtests.common.Config
import com.iodice.mediasearch.integtests.common.readTestResource
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


enum class MediaAPITestConfig(
        val route: String,
        val postBodyFile: String,
        val dependencies: List<MediaAPITestConfig>
) {
    API_SOURCE(
            Config.ROUTE_PART_SOURCE,
            "requests/source_01.json",
            emptyList()
    ),
    API_MEDIA(
            "${Config.ROUTE_PART_SOURCE}/integ_test_source_id_01/${Config.ROUTE_PART_MEDIA}",
            "requests/media_01.json",
            listOf(API_SOURCE)
    ),
}


class MediaAPITest {
    @ParameterizedTest
    @EnumSource(MediaAPITestConfig::class)
    fun `GET for random ID returns 404`(config: MediaAPITestConfig) {
        val id = UUID.randomUUID().toString()
        val response = Unirest.get("${Config.SERVICE_ENDPOINT_BASE}/${config.route}/$id")
                .header("accept", "application/json")
                .asString()

        assertEquals(HttpStatus.SC_NOT_FOUND, response.status)
        assertNotNull(response.body)
        // the error should contain the ID that cannot be found
        assertTrue(response.body.contains(id))
    }

    @ParameterizedTest
    @EnumSource(MediaAPITestConfig::class)
    fun `POST with no body returns 400`(config: MediaAPITestConfig) {
        val response = Unirest.post("${Config.SERVICE_ENDPOINT_BASE}/${config.route}/")
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.status)
    }

    @ParameterizedTest
    @EnumSource(MediaAPITestConfig::class)
    fun `POST, GET and DELETE all work together`(config: MediaAPITestConfig) {
        // Step (0): Setup dependent API calls
        config.dependencies.forEach { postForConfig(it) }

        // Step (1): POST data, validate the call did not fail
        val request = readTestResource(config.postBodyFile)
        val postResponse = postForConfig(config)
        assertEquals(HttpStatus.SC_OK, postResponse.status)
        JSONAssert.assertEquals(request, postResponse.body.toString(), false);

        // Step (2): GET data, validate it is the correct data
        val id = postResponse.body.`object`.get("id")
        val getResponse = Unirest.get("${Config.SERVICE_ENDPOINT_BASE}/${config.route}/$id")
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_OK, getResponse.status)
        JSONAssert.assertEquals(postResponse.body.toString(), getResponse.body, true);

        // Step (2.5): GET all and assert the item is returned
        val getAllResponse = Unirest.get("${Config.SERVICE_ENDPOINT_BASE}/${config.route}/$id")
                .header("accept", "application/json")
                .asJson()
        assertEquals(HttpStatus.SC_OK, getResponse.status)
        assertTrue(getAllResponse.body.toString().contains(postResponse.body.toString()));


        // Step (3): DELETE data, validate the call did not fail
        val deleteResponse = deleteForConfig(config)
        assertEquals(HttpStatus.SC_OK, deleteResponse.status)

        // Step (4): GET data, validate it does not exist
        val secondGetResponse = Unirest.get("${Config.SERVICE_ENDPOINT_BASE}/${config.route}/$id")
                .header("accept", "application/json")
                .asString()
        assertEquals(HttpStatus.SC_NOT_FOUND, secondGetResponse.status)

        // step (5): Cleanup dependencies
        config.dependencies.reversed().forEach {
            deleteForConfig(it)
        }
    }

    private fun postForConfig(config: MediaAPITestConfig): HttpResponse<JsonNode> = Unirest.post("${Config.SERVICE_ENDPOINT_BASE}/${config.route}/")
            .header("content-type", "application/json")
            .header("accept", "application/json")
            .body(readTestResource(config.postBodyFile))
            .asJson()

    private fun deleteForConfig(config: MediaAPITestConfig): HttpResponse<String> {
        val id = JsonParser
                .parseString(readTestResource(config.postBodyFile))
                .asJsonObject["id"]
                .asString
        return Unirest.delete("${Config.SERVICE_ENDPOINT_BASE}/${config.route}/$id")
                .header("accept", "application/json")
                .asString()
    }
}
package com.iodice.mediasearch.api

import com.iodice.mediasearch.client.SearchIndexClient
import com.iodice.mediasearch.model.AggregatedQueryResponse
import com.iodice.mediasearch.model.SearchIndexResponse
import com.iodice.mediasearch.service.SearchIndexService
import org.springframework.web.bind.annotation.*
import javax.inject.Inject

@RestController
@RequestMapping("query")
class QueryController(
        @Inject var searchIndexService: SearchIndexService
) {

    @GetMapping("/{query}")
    fun query(@PathVariable query: String): AggregatedQueryResponse {
        return searchIndexService.queryAggregatedByMedia(query)
    }
}

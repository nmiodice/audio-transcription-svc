package com.iodice.mediasearch.api

import com.iodice.mediasearch.METRICS
import com.iodice.mediasearch.model.AggregatedQueryResponse
import com.iodice.mediasearch.service.SearchIndexService
import com.iodice.mediasearch.util.trackDuration
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.inject.Inject

@RestController
@RequestMapping("/api/v1/query")
class QueryController(
        @Inject var searchIndexService: SearchIndexService,
        @Inject var metricsClient: TelemetryClient
) {

    @GetMapping("/{query}")
    fun query(@PathVariable query: String): AggregatedQueryResponse {
        return metricsClient.trackDuration(METRICS.API_QUERY_DURATION) {
            searchIndexService.queryAggregatedByMedia(query)
        }
    }
}

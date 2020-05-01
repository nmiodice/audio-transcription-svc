package com.iodice.mediasearch.adapter

import com.iodice.mediasearch.SOURCES
import com.iodice.mediasearch.model.Media
import com.iodice.mediasearch.model.Source
import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component
import javax.inject.Inject

interface SourceListAdapter {
    fun queryForMedia(source: Source): Iterator<Media>
}

@Component
class SourceListAdapterFactory(
        @Inject private val beanFactory: BeanFactory
) {
    fun forSource(source: Source): SourceListAdapter? {
        fun matches(s: String) = source.trackListEndpoint.toLowerCase().startsWith(s.toLowerCase())
        val clazz = when {
            matches(SOURCES.HUB_HOPPER) -> HubHopperAdapter::class.java
            else -> null
        }

        return if (clazz != null) beanFactory.getBean(clazz) else null
    }
}
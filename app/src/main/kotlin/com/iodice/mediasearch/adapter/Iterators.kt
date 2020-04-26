package com.iodice.mediasearch.adapter

import java.util.*

class FilteredIterator<T>(
        private val iterator: Iterator<T>,
        private val filter: (T) -> Boolean
) : Iterator<T> {
    private var next: T? = null

    init {
        setNext()
    }


    override fun hasNext(): Boolean {
        return next != null
    }

    // assumes hasNext has been called
    override fun next(): T {
        val toReturn = next!!
        setNext()
        return toReturn
    }

    private fun setNext() {
        while (iterator.hasNext()) {
            val nextFromSource = iterator.next()
            if (filter(nextFromSource)) {
                next = nextFromSource
                return
            }
        }
        next = null
    }
}

class PaginatedIterator<T>(
        private val hasPage: (Int) -> Boolean,
        private val getPage: (Int) -> Iterator<T>
) : Iterator<T> {
    private var currPage: Int = 0
    private var currPageResults: Iterator<T> = Collections.emptyIterator()

    override fun hasNext(): Boolean {
        return currPageResults.hasNext() || hasPage(currPage + 1)
    }

    override fun next(): T {
        if (currPageResults.hasNext()) {
            return currPageResults.next()
        }

        if (hasNext()) {
            currPage++
            currPageResults = getPage(currPage)
        }

        return currPageResults.next()
    }
}
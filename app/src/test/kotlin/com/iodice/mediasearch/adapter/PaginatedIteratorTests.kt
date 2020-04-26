package com.iodice.mediasearch.adapter

import org.junit.jupiter.api.Test

class PaginatedIteratorTests {
    @Test
    fun `PaginatedIterator should be empty if there is no first page`() {
        val iterator = PaginatedIterator(
                hasPage = fun(_): Boolean = false,
                getPage = fun(_): Iterator<String> = throw IllegalStateException("FOO")
        )
        assert(!iterator.hasNext())
    }

    @Test
    fun `PaginatedIterator should be return first page if there is only one page`() {
        val iterator = PaginatedIterator(
                hasPage = fun(i): Boolean = i == 1,
                getPage = fun(i): Iterator<String> = listOf("$i").iterator()
        )
        assert(iterator.hasNext())
        assert(iterator.next() == "1")
        assert(!iterator.hasNext())
    }

    @Test
    fun `PaginatedIterator should be return first page if there are two pages`() {
        val iterator = PaginatedIterator(
                hasPage = fun(i): Boolean = i == 1 || i == 2,
                getPage = fun(i): Iterator<String> = listOf("$i").iterator()
        )
        assert(iterator.hasNext())
        assert(iterator.next() == "1")
        assert(iterator.hasNext())
        assert(iterator.next() == "2")
        assert(!iterator.hasNext())
    }
}
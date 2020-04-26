package com.iodice.mediasearch.adapter

import org.junit.jupiter.api.Test

class FilteredIteratorTests {
    @Test
    fun `FilteredIterator should be empty if original iterator is empty`() {
        val iterator = FilteredIterator(listOf("foo", "bar").iterator()) { it.startsWith("A") }
        assert(!iterator.hasNext())
    }

    @Test
    fun `FilteredIterator should never hasNext if nothing passes filter`() {
        val iterator = FilteredIterator(listOf<String>().iterator()) { it.startsWith("A") }
        assert(!iterator.hasNext())
    }

    @Test
    fun `FilteredIterator should return hasNext if only first item passes filter`() {
        val iterator = FilteredIterator(listOf("foo", "bar").iterator()) { it == "foo" }
        assert(iterator.hasNext())
        assert(iterator.next() == "foo")
        assert(!iterator.hasNext())
    }

    @Test
    fun `FilteredIterator should return hasNext if only second item passes filter`() {
        val iterator = FilteredIterator(listOf("foo", "bar").iterator()) { it == "bar" }
        assert(iterator.hasNext())
        assert(iterator.next() == "bar")
        assert(!iterator.hasNext())
    }

    @Test
    fun `FilteredIterator should return hasNext until empty if all items pass filter`() {
        val iterator = FilteredIterator(listOf("foo", "bar").iterator()) { it != "" }
        assert(iterator.hasNext())
        assert(iterator.next() == "foo")
        assert(iterator.hasNext())
        assert(iterator.next() == "bar")
        assert(!iterator.hasNext())
    }
}
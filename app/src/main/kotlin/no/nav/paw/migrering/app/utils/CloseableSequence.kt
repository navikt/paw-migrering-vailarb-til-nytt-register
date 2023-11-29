package no.nav.paw.migrering.app.utils

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

interface CloseableSequence<V>: Sequence<V>, Closeable

fun <V> closeableSequenceOf(iterable: Iterable<V>): CloseableSequence<V> {
    return object : CloseableSequence<V> {
        private val isClosed = AtomicBoolean(false)
        override fun iterator(): Iterator<V> = object : Iterator<V> {
            private val iterator = iterable.iterator()
            override fun hasNext(): Boolean {
                return !isClosed.get() && iterator.hasNext()
            }

            override fun next(): V {
                if (!hasNext()) throw NoSuchElementException("No more elements")
                return iterator.next()
            }
        }
        override fun close() {
            isClosed.set(true)
        }
    }
}

package kotlinx.websocket.test

import com.squareup.okhttp.OkHttpClient
import kotlinx.websocket.*
import kotlinx.websocket.gson.withGsonConsumer
import kotlinx.websocket.gson.withGsonProducer
import okio.Buffer
import org.junit.Before
import org.junit.Rule
import rx.lang.kotlin.ReplaySubject
import rx.lang.kotlin.subscriber
import rx.lang.kotlin.toObservable
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import org.junit.Test as test

public class RegularTest {

    public var server: ServerTestResource = ServerTestResource()
        [Rule] get() = $server
        set(ns) {
            $server = ns;
        }

    val allEvents = ReplaySubject<Pair<String, String?>>()

    [Before]
    fun before() {
        server.events.subscribe(allEvents)
    }

    test fun main() {
        var lastState: WebSocketState? = null
        val stateObserver = subscriber<WebSocketState>()
                .onNext {
                    lastState = it
                }

        OkHttpClient().newWebSocket("ws://localhost:${server.port}/ws")
                .withGsonProducer(listOf(1, 2, 3).toObservable())
                .withStateObserver(stateObserver)
                .open()

        val lastEvent = server.events.filter { it.first in listOf("onClose", "onError") }.toBlocking().first()
        assertEquals("onClose", lastEvent.first)
        assertEquals(WebSocketState.CLOSED, lastState)

        allEvents.onCompleted()
        assertEquals(listOf("onOpen" to null, "onMessage" to "1", "onMessage" to "2", "onMessage" to "3", "onClose" to null), allEvents.toList().toBlocking().first())
    }

    test fun receive() {
        server.toBeSent = listOf("a", "b", "c", "EOF").toObservable()

        val received = CopyOnWriteArrayList<String>()
        val consumer = subscriber<String>()
                .onNext {
                    received.add(it)
                }

        val stateObserver = ReplaySubject<WebSocketState>()

        val socket = OkHttpClient().newWebSocket("ws://localhost:${server.port}/ws")
                .withGsonConsumer(consumer, javaClass<String>())
                .withStateObserver(stateObserver)
                .withReconnectOnEndOfStream(false)
                .open()

//        stateObserver.filter { it == WebSocketState.CLOSED }.toBlocking().first()

        val lastEvent = server.events.filter { it.first in listOf("onClose", "onError") }.toBlocking().first()
        assertEquals("onClose", lastEvent.first)
        socket.close()

        println(received)
    }
}
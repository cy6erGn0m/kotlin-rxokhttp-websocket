package kotlinx.websocket.test

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.websocket.WebSocketState
import kotlinx.websocket.gson.newWebSocket
import org.junit.Before
import org.junit.Rule
import rx.lang.kotlin.ReplaySubject
import rx.lang.kotlin.toObservable
import kotlin.test.assertEquals
import org.junit.Test as test

public class RegularTest {

    public var server : ServerTestResource = ServerTestResource()
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
        val mySocket = OkHttpClient().newWebSocket<Any, String>(
                Request.Builder().url("ws://localhost:${server.port}/ws").build(),
                listOf("a", "b", "c").toObservable()
        )

        var lastState : WebSocketState? = null
        mySocket.state.subscribe {
            lastState = it
        }

        val lastEvent = server.events.filter {it.first in listOf("onClose", "onError")}.toBlocking().first()
        assertEquals("onClose", lastEvent.first)
        assertEquals(WebSocketState.CLOSED, lastState)

        allEvents.onCompleted()
        println(allEvents.toList().toBlocking().first())
    }
}
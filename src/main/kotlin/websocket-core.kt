package kotlinx.websocket

import com.squareup.okhttp.ws.WebSocket
import okio.Buffer
import rx.Observable
import rx.Observer
import rx.Subscription
import rx.lang.kotlin.subscriber
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

public fun JetSocketBuilder.open(): JetWebSocket {
    state.onNext(WebSocketState.CREATED)
    val jetSocket = JetWebSocket()

    with<JetSocketBuilder, Any, Any> {
        val closer = socketCloser()
        val pinger = Observable.timer(15L, 10L, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
        val outgoingSubscription = AtomicReference<Subscription?>()
        val pingerSubscription = AtomicReference<Subscription?>()

        jetSocket.closeSubject.subscribeOn(Schedulers.io()).subscribe {
            outgoingSubscription.unsubscribe()
            pingerSubscription.unsubscribe()

            closer.onCompleted()
            consumer.onCompleted()

            state.onNext(WebSocketState.CLOSED)
            state.onCompleted()
        }

        webSocketFactory(this.client, this.request, consumer, reconnectOnEndOfStream, subscriber(), decoder).
                subscribeOn(Schedulers.io()).
                doOnSubscribe { state.onNext(WebSocketState.CONNECTING) }.
                doOnError { closer.onError(it) }.
                retryWhen { it.flatMap { Observable.timer(5L, TimeUnit.SECONDS) } }.
                doOnCompleted { closer.onCompleted(); state.onNext(WebSocketState.CLOSED) }.
                subscribe { socket ->
                    state.onNext(WebSocketState.CONNECTED)
                    subscribeSocket(socket, producer, jetSocket.closeSubject, encoder).putTo(outgoingSubscription)
                    subscribeSocket(socket, pinger, jetSocket.closeSubject) { s, o ->
                        s.sendPing(Buffer().writeUtf8("ping"))
                    }.putTo(pingerSubscription)
                    closer.onNext(socket)
                }
    }

    return jetSocket
}

private fun socketCloser(): Observer<WebSocket> = AtomicReference<WebSocket?>().let { prev ->
    subscriber<WebSocket>().onNext { socket ->
        prev.getAndSet(socket)?.safeClose()
    }.onError {
        prev.getAndSet(null)?.safeClose()
    }.onCompleted {
        prev.getAndSet(null)?.safeClose()
    }
}
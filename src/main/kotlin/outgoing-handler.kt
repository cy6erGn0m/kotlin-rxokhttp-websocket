package kotlinx.websocket

import com.squareup.okhttp.ws.WebSocket
import rx.Observable
import rx.Observer
import rx.lang.kotlin.subscriber


fun <O> subscribeSocket(socket: WebSocket, producer: Observable<O>, closeObserver : Observer<CloseReason>,  onMessage: (WebSocket, O) -> Unit) =
        producer.doOnCompleted {
            closeObserver.onNext(CLOSE_NO_REASON)
        }.unsafeSubscribe(subscriber<O>().onNext {
            onMessage(socket, it)
        }.onError {
            socket.safeClose() // we expect to receive onFailure in webSocketFactory so we have nothing to do here actually
        })

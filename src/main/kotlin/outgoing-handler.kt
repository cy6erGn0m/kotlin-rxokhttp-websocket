package kotlinx.websocket

import com.squareup.okhttp.ws.WebSocket
import rx.Observable
import rx.Observer
import rx.lang.kotlin.subscriber


fun <O> subscribeSocket(socket: WebSocket, outgoing: Observable<O>, incoming: Observer<*>, state: Observer<WebSocketState>, onMessage : (socket : WebSocket, obj : O) -> Unit) =
        outgoing.doOnCompleted { incoming.onCompleted(); state.onNext(WebSocketState.CLOSED); state.onCompleted(); socket.safeClose() }.unsafeSubscribe(subscriber<O>().onNext {
            onMessage(socket, it)
        }.onError {
            socket.safeClose() // we expect to receive onFailure in webSocketFactory so we have nothing to do here actually
        })

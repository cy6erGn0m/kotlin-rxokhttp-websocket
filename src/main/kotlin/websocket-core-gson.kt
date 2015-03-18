package kotlinx.websocket.gson

import com.google.gson.Gson
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.ws.WebSocket
import kotlinx.websocket.ReactiveWebSocket
import kotlinx.websocket.newWebSocket
import okio.Buffer
import okio.BufferedSource
import rx.Observable
import rx.Observer

public fun <I, O> OkHttpClient.newWebSocket(
        request: Request, outgoing: Observable<O>, reconnectOnEndOfStream: Boolean,
        incomingClass: Class<I>
): ReactiveWebSocket<I> = this.newWebSocket<I, O>(request, outgoing, reconnectOnEndOfStream,
        createGsonIncomingHandler(incomingClass), createGsonOutgoingHandler())

private fun <I> createGsonIncomingHandler(incomingClass: Class<I>, gson: Gson = Gson()) =
        {(type: WebSocket.PayloadType, buffer: BufferedSource, incoming: Observer<in I>) ->
            when (type) {
                WebSocket.PayloadType.TEXT -> incoming.onNext(gson.fromJson(buffer.readUtf8(), incomingClass))
                else -> {
                }
            }
        }

private fun <O> createGsonOutgoingHandler(gson: Gson = Gson()) = {(socket: WebSocket, obj: O) ->
    socket.sendMessage(WebSocket.PayloadType.TEXT, Buffer().writeUtf8(gson.toJson(obj)))
}

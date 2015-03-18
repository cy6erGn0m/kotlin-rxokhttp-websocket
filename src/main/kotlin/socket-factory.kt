package kotlinx.websocket

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketCall
import com.squareup.okhttp.ws.WebSocketListener
import okio.Buffer
import okio.BufferedSource
import rx.Observer
import rx.lang.kotlin.observable
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

fun <I> webSocketFactory(httpClient: OkHttpClient, request: Request, incoming: Observer<I>, reconnectOnEndOfStream: Boolean, pongs: Observer<Long>, messageHandler : (WebSocket.PayloadType, BufferedSource, Observer<I>) -> Unit) =
        observable<WebSocket> { socketConsumer ->
            val lastWebSocket = AtomicReference<WebSocket?>()

            WebSocketCall.create(httpClient, request).enqueue(object : WebSocketListener {
                override fun onOpen(webSocket: WebSocket?, request: Request?, response: Response?) {
                    if (response?.code() != 101) {
                        socketConsumer.onError(IOException("Failed to connect to websocket $request due to ${response?.code()} ${response?.message()}"))
                    } else {
                        lastWebSocket.set(webSocket)
                        socketConsumer.onNext(webSocket)
                    }
                }

                override fun onPong(payload: Buffer?) {
                    pongs.onNext(System.currentTimeMillis())
                }

                override fun onClose(code: Int, reason: String?) {
                    if (reconnectOnEndOfStream) {
                        socketConsumer.onError(IOException("End of stream. Reconnect required"))
                    } else {
                        incoming.onCompleted()
                    }
                }

                override fun onFailure(e: IOException?) {
                    lastWebSocket.get()?.safeClose()
                    socketConsumer.onError(e)
                }

                override fun onMessage(payload: BufferedSource?, type: WebSocket.PayloadType?) {
                    if (payload != null && type != null) {
                        messageHandler(type, payload, incoming)
                    }
                }
            })
        }
package kotlinx.websocket

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.ws.WebSocket
import okio.BufferedSource
import rx.Observable
import rx.Observer
import rx.lang.kotlin.PublishSubject
import rx.lang.kotlin.subscriber
import rx.subjects.Subject
import java.io.IOException
import java.util.concurrent.TimeUnit

public enum class WebSocketState {
    CREATED
    CONNECTING
    CONNECTED
    CLOSED
}

public trait JetSocketBuilder {
    val request: Request
    val state: Observer<WebSocketState>
    val reconnectOnEndOfStream : Boolean
    val reconnectProvider : (Throwable) -> Observable<*>
}

public trait JetSocketBuilderWithReader<I> : JetSocketBuilder {
    val consumer: Observer<I>
    val decoder: (WebSocket.PayloadType, BufferedSource, Observer<I>) -> Unit
}

public trait JetSocketBuilderWithWriter<O> : JetSocketBuilder {
    val producer: Observable<O>
    val encoder: (socket: WebSocket, out: O) -> Unit
}

public trait JetSocketDuplex<I, O> : JetSocketBuilderWithReader<I>, JetSocketBuilderWithWriter<O> {}

data class JetSocketInput<I, O>(val client: OkHttpClient, override val request: Request) : JetSocketBuilder, JetSocketBuilderWithReader<I>, JetSocketBuilderWithWriter<O> {
    override var state: Observer<WebSocketState> = subscriber()
    override var reconnectOnEndOfStream : Boolean = true
    override var reconnectProvider : (Throwable) -> Observable<*> = {Observable.timer(10L, TimeUnit.SECONDS)}

    override var consumer: Observer<I> = subscriber()
    override var decoder: (WebSocket.PayloadType, BufferedSource, Observer<I>) -> Unit = { t, b, o -> }

    override var producer: Observable<O> = PublishSubject()
    override var encoder: (socket: WebSocket, out: O) -> Unit = { s, o -> }
}

[suppress("UNCHECKED_CAST")]
inline fun <R : JetSocketBuilder, I, O> JetSocketBuilder.with(body: JetSocketInput<I, O>.() -> Unit): R {
    val underlying = this as JetSocketInput<I, O>
    underlying.body()
    return underlying as R
}

public fun OkHttpClient.newWebSocket(request: Request): JetSocketBuilder = JetSocketInput<Any, Any>(this.ensureConfiguration(), request)
public fun OkHttpClient.newWebSocket(url: String, headers: Map<String, String> = emptyMap()): JetSocketBuilder = newWebSocket(
        headers.entrySet().fold(Request.Builder().url(url)) { acc, e -> acc.addHeader(e.getKey(), e.getValue()) }.build()
)

public fun <I> JetSocketBuilder.withConsumer(consumer: Observer<I>, decoder: (WebSocket.PayloadType, BufferedSource, Observer<I>) -> Unit): JetSocketBuilderWithReader<I> =
        with<JetSocketBuilderWithReader<I>, I, Any> { this.consumer = consumer; this.decoder = decoder }

public fun <I, O> JetSocketBuilderWithWriter<O>.withConsumer(consumer: Observer<I>, decoder: (WebSocket.PayloadType, BufferedSource, Observer<I>) -> Unit): JetSocketDuplex<I, O> =
        with<JetSocketDuplex<I, O>, I, Any> { this.consumer = consumer; this.decoder = decoder }

public fun <I, O> JetSocketDuplex<I, O>.withConsumer(consumer: Observer<I>, decoder: (WebSocket.PayloadType, BufferedSource, Observer<I>) -> Unit): JetSocketDuplex<I, O> =
        with<JetSocketDuplex<I, O>, I, Any> { this.consumer = consumer; this.decoder = decoder }

public fun <O> JetSocketBuilder.withProducer(producer: Observable<O>, encoder: (socket: WebSocket, out: O) -> Unit): JetSocketBuilderWithWriter<O> =
        with<JetSocketBuilderWithWriter<O>, Any, O> { this.producer = producer; this.encoder = encoder }

public fun <I, O> JetSocketBuilderWithReader<I>.withProducer(producer: Observable<O>, encoder: (socket: WebSocket, out: O) -> Unit): JetSocketDuplex<I, O> =
        with<JetSocketDuplex<I, O>, I, O> { this.producer = producer; this.encoder = encoder }

public fun <I, O> JetSocketDuplex<I, O>.withProducer(producer: Observable<O>, encoder: (socket: WebSocket, out: O) -> Unit): JetSocketDuplex<I, O> =
        with<JetSocketDuplex<I, O>, I, O> { this.producer = producer; this.encoder = encoder }

public fun <B : JetSocketBuilder> B.withStateObserver(stateObserver: Observer<WebSocketState>): B =
        with<B, Any, Any>{ this.state = stateObserver }

public fun <B : JetSocketBuilder> B.withReconnectOnEndOfStream(reconnect : Boolean): B =
        with<B, Any, Any>{ this.reconnectOnEndOfStream = reconnect }

public fun <B : JetSocketBuilder> B.withReconnectProvider(reconnectProvider : (Throwable) -> Observable<*> ): B =
        with<B, Any, Any>{ this.reconnectProvider = reconnectProvider }

public class JetWebSocket {
    val closeSubject : Subject<CloseReason, CloseReason> = PublishSubject()
}

public data class CloseReason(val closeCode : CloseCode = CloseCodes.NORMAL_CLOSURE, val message : String = "")
val CLOSE_NO_REASON = CloseReason()

public fun JetWebSocket.close(reason : CloseReason = CloseReason()) : Unit = closeSubject.onNext(reason)

public fun WebSocket.safeClose(code: CloseCode = CloseCodes.NORMAL_CLOSURE, message: String = ""): Unit =
        try {
            close(code.code, message)
        } catch(ignore: Throwable) {
        }


public class WebSocketClosedWithReasonIOException(val reason : CloseReason) : IOException("WebSocket closed due to reason ${reason.closeCode.code} ${reason.closeCode.codeName()}: ${reason.message}")

private fun CloseCode.codeName() = CloseCodes.getCloseCode(this.code).let { resolved -> if (resolved is CloseCodes) resolved.name() else "" }
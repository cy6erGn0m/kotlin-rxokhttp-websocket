package kotlinx.websocket

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.ws.WebSocket
import rx.Observable
import rx.Observer
import rx.lang.kotlin.BehaviourSubject
import rx.lang.kotlin.PublishSubject
import rx.lang.kotlin.subscriber
import rx.lang.kotlin.toObservable
import java.util.concurrent.atomic.AtomicReference

public enum class WebSocketState {
    CREATED
    CONNECTING
    CONNECTED
    CLOSED
}

public trait ReactiveWebSocketBuilder {
    val state: Observer<WebSocketState>
}

public trait ReactiveWebSocketBuilderWithReader<I> : ReactiveWebSocketBuilder {
    val consumer: Observer<I>
}

public trait ReactiveWebSocketBuilderWithWriter<O> : ReactiveWebSocketBuilder {
    val producer: Observable<O>
}

public trait ReactiveWebSocketDuplex<I, O> : ReactiveWebSocketBuilderWithReader<I>, ReactiveWebSocketBuilderWithWriter<O> {}

private data class ReactiveWebSocketInput<I, O>(val client: OkHttpClient) : ReactiveWebSocketBuilder, ReactiveWebSocketBuilderWithReader<I>, ReactiveWebSocketBuilderWithWriter<O> {
    override var state: Observer<WebSocketState> = subscriber()
    override var consumer: Observer<I> = subscriber()
    override var producer: Observable<O> = PublishSubject()
    val closeSubject = BehaviourSubject(false)
}

[suppress("UNCHECKED_CAST")]
fun <R : ReactiveWebSocketBuilder, I, O> ReactiveWebSocketBuilder.with(body: ReactiveWebSocketInput<I, O>.() -> Unit): R {
    val underlying = this as ReactiveWebSocketInput<I, O>
    underlying.body()
    return underlying as R
}

public fun OkHttpClient.newWebSocket(): ReactiveWebSocketBuilder = ReactiveWebSocketInput<Any, Any>(this.ensureConfiguration())
public fun <I> ReactiveWebSocketBuilder.withConsumer(clazz: Class<I>, consumer: Observer<I>, decoder: () -> I): ReactiveWebSocketBuilderWithReader<I> =
        with<ReactiveWebSocketBuilderWithReader<I>, I, Any> { this.consumer = consumer }

public fun <I, O> ReactiveWebSocketBuilderWithWriter<O>.withConsumer(clazz: Class<I>, consumer: Observer<I>, decoder: () -> I): ReactiveWebSocketDuplex<I, O> =
        with<ReactiveWebSocketDuplex<I, O>, I, Any> { this.consumer = consumer }

public fun <I, O> ReactiveWebSocketDuplex<I, O>.withConsumer(clazz: Class<I>, consumer: Observer<I>, decoder: () -> I): ReactiveWebSocketDuplex<I, O> =
        with<ReactiveWebSocketDuplex<I, O>, I, Any> { this.consumer = consumer }

public fun <O> ReactiveWebSocketBuilder.withProducer(producer : Observable<O>): ReactiveWebSocketBuilderWithWriter<O> =
        with<ReactiveWebSocketBuilderWithWriter<O>, Any, O> { this.producer = producer }

public fun <I, O> ReactiveWebSocketBuilderWithReader<I>.withProducer(producer : Observable<O>): ReactiveWebSocketDuplex<I, O> =
        with<ReactiveWebSocketDuplex<I, O>, I, O> { this.producer = producer }

public fun <I, O> ReactiveWebSocketDuplex<I, O>.withProducer(producer : Observable<O>): ReactiveWebSocketDuplex<I, O> =
        with<ReactiveWebSocketDuplex<I, O>, I, O> { this.producer = producer }

public fun <B : ReactiveWebSocketBuilder> B.withStateObserver(stateObserver : Observer<WebSocketState>) : B =
    with<B, Any, Any>{this.state = stateObserver}


// TODO here we go with open
//public fun ReactiveWebSocketBuilder.open() : ReactiveWebSocket2 {
//    with<ReactiveWebSocketBuilder, Any, Any> {
//
//    }
//}

public class ReactiveWebSocket2 {

}

public class ReactiveWebSocket<I>(
        val state: Observable<WebSocketState>,
        val incoming: Observable<in I>
) {
    val webSocketReference: AtomicReference<WebSocket?> = AtomicReference()
}

public fun ReactiveWebSocket<*>.close(code: CloseCode = CloseCodes.NORMAL_CLOSURE, message: String = ""): Unit =
        this.webSocketReference.get()?.close(code, message) // TODO may not work due to race!!!


public fun WebSocket.close(code: CloseCode = CloseCodes.NORMAL_CLOSURE, message: String = ""): Unit =
        close(code.code, message)

public fun WebSocket.safeClose(code: CloseCode = CloseCodes.NORMAL_CLOSURE, message: String = ""): Unit =
        try {
            close(code.code, message)
        } catch(ignore: Throwable) {
        }



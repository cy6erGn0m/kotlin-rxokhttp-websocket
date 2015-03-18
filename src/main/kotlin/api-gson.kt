package kotlinx.websocket.gson

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.websocket.ReactiveWebSocket
import rx.Observable
import rx.lang.kotlin.PublishSubject

public inline fun <reified I, O> OkHttpClient.newWebSocket(request: Request, outgoing: Observable<O> = PublishSubject(), reconnectOnEndOfStream: Boolean = true): ReactiveWebSocket<I> =
        this.newWebSocket(request, outgoing, reconnectOnEndOfStream, javaClass<I>())

package kotlinx.websocket

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.ws.WebSocket
import okio.Buffer
import okio.BufferedSource
import rx.Observable
import rx.Observer
import rx.Subscription
import rx.lang.kotlin.BehaviourSubject
import rx.lang.kotlin.PublishSubject
import rx.lang.kotlin.subscriber
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

public fun <I, O> OkHttpClient.newWebSocket(
        request: Request, outgoing: Observable<O>, reconnectOnEndOfStream: Boolean,
        incomingHandler : (WebSocket.PayloadType, BufferedSource, Observer<I>) -> Unit,
        outgoingHandler : (socket : WebSocket, obj : O) -> Unit
): ReactiveWebSocket<I> =
        BehaviourSubject(WebSocketState.CREATED).let { state ->
            PublishSubject<I>().let { incoming ->
                val pinger = Observable.timer(15L, 10L, TimeUnit.SECONDS).subscribeOn(Schedulers.newThread())
                val outgoingSubscription = AtomicReference<Subscription?>()
                val pingSubscription = AtomicReference<Subscription?>()

                val pongObserver = subscriber<Long>().onNext {
                }

                webSocketFactory(this@newWebSocket, request, incoming, reconnectOnEndOfStream, pongObserver, incomingHandler).
                        subscribeOn(Schedulers.io()).
                        doOnSubscribe {
                            state.onNext(WebSocketState.CONNECTING)
                        }.
                        doOnError {
                            outgoingSubscription.unsubscribe()
                            pingSubscription.unsubscribe()
                        }.
                        retryWhen { it.flatMap { Observable.timer(5L, TimeUnit.SECONDS) } }.
                        subscribe { socket ->
                            state.onNext(WebSocketState.CONNECTED)
                            subscribeSocket(socket, outgoing, incoming, state, outgoingHandler).putTo(outgoingSubscription)
                            subscribeSocket(socket, pinger, incoming, state) { s, o ->
                                s.sendPing(Buffer())
                            }.putTo(pingSubscription)
                        }

                incoming.doOnCompleted {
                    outgoingSubscription.unsubscribe()
                    pingSubscription.unsubscribe()
                }

                ReactiveWebSocket(state.distinctUntilChanged(), incoming)
            }

        }

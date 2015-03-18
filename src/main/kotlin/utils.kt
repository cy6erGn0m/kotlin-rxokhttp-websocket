package kotlinx.websocket

import rx.Subscription
import java.util.concurrent.atomic.AtomicReference


fun AtomicReference<Subscription?>.subscribed(s: Subscription) = getAndSet(s)?.unsubscribe()
fun AtomicReference<Subscription?>.unsubscribe() = getAndSet(null)?.unsubscribe()

fun Subscription.putTo(reference: AtomicReference<Subscription?>) = reference.subscribed(this)

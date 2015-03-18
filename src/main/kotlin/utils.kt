package kotlinx.websocket

import com.squareup.okhttp.OkHttpClient
import rx.Subscription
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


fun AtomicReference<Subscription?>.subscribed(s: Subscription) = getAndSet(s)?.unsubscribe()
fun AtomicReference<Subscription?>.unsubscribe() = getAndSet(null)?.unsubscribe()

fun Subscription.putTo(reference: AtomicReference<Subscription?>) = reference.subscribed(this)

fun OkHttpClient.ensureConfiguration() : OkHttpClient {
    var configuredClient = this
    if (this.getConnectTimeout() == 0) {
        configuredClient = configuredClient.clone()
        configuredClient.setConnectTimeout(15L, TimeUnit.SECONDS)
    }
    if (this.getWriteTimeout() == 0) {
        configuredClient = configuredClient.clone()
        configuredClient.setWriteTimeout(10L, TimeUnit.SECONDS)
    }

    return configuredClient
}
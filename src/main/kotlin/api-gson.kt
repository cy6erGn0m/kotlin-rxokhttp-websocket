package kotlinx.websocket.gson

import com.google.gson.Gson
import com.squareup.okhttp.ws.WebSocket
import kotlinx.websocket.*
import okio.Buffer
import okio.BufferedSource
import rx.Observable
import rx.Observer

private fun <I> getGsonDecoder(clazz : Class<I>, gson : Gson) = { type: WebSocket.PayloadType, buffer: BufferedSource, consumer: Observer<I> ->
        when (type) {
                WebSocket.PayloadType.TEXT -> consumer.onNext(gson.fromJson(buffer.inputStream().reader(Charsets.UTF_8).use {it.readText()}, clazz))
        }
}

private fun <O> getGsonEncoder(gson : Gson) = {  socket : WebSocket, out : O ->
        socket.sendMessage(WebSocket.PayloadType.TEXT, Buffer().writeUtf8(gson.toJson(out)))
}

public inline fun <reified I> JetSocketBuilder.withGsonConsumer(consumer: Observer<I>, gson : Gson = Gson()): JetSocketBuilderWithReader<I> =
        withGsonConsumer(consumer, javaClass<I>(), gson)

public fun <I> JetSocketBuilder.withGsonConsumer(consumer: Observer<I>, clazz : Class<I>, gson : Gson = Gson()): JetSocketBuilderWithReader<I> =
        with<JetSocketBuilderWithReader<I>, I, Any> { this.consumer = consumer; this.decoder = getGsonDecoder(clazz, gson) }

public inline fun <reified I, O> JetSocketBuilderWithWriter<O>.withGsonConsumer(consumer: Observer<I>, gson : Gson = Gson()): JetSocketDuplex<I, O> =
        withGsonConsumer(consumer, javaClass<I>(), gson)

public fun <I, O> JetSocketBuilderWithWriter<O>.withGsonConsumer(consumer: Observer<I>, clazz : Class<I>, gson : Gson = Gson()): JetSocketDuplex<I, O> =
        with<JetSocketDuplex<I, O>, I, Any> { this.consumer = consumer; this.decoder = getGsonDecoder(clazz, gson) }

public inline fun <reified I, O> JetSocketDuplex<I, O>.withGsonConsumer(consumer: Observer<I>, gson : Gson = Gson()): JetSocketDuplex<I, O> =
        withGsonConsumer(consumer, javaClass<I>(), gson)

public fun <I, O> JetSocketDuplex<I, O>.withGsonConsumer(consumer: Observer<I>, clazz : Class<I>, gson : Gson = Gson()): JetSocketDuplex<I, O> =
        with<JetSocketDuplex<I, O>, I, Any> { this.consumer = consumer; this.decoder = getGsonDecoder(clazz, gson) }

public fun <O> JetSocketBuilder.withGsonProducer(producer: Observable<O>, gson : Gson = Gson()): JetSocketBuilderWithWriter<O> =
        with<JetSocketBuilderWithWriter<O>, Any, O> { this.producer = producer; this.encoder = getGsonEncoder(gson) }

public fun <I, O> JetSocketBuilderWithReader<I>.withGsonProducer(producer: Observable<O>, gson : Gson = Gson()): JetSocketDuplex<I, O> =
        with<JetSocketDuplex<I, O>, I, O> { this.producer = producer; this.encoder = getGsonEncoder(gson) }

public fun <I, O> JetSocketDuplex<I, O>.withGsonProducer(producer: Observable<O>, gson : Gson = Gson()): JetSocketDuplex<I, O> =
        with<JetSocketDuplex<I, O>, I, O> { this.producer = producer; this.encoder = getGsonEncoder(gson) }

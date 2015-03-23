# kotlin-rxokhttp-websocket
WebSocket library for Kotlin and RxJava/RxKotlin based on OkHttp and Gson

## Motivation
Do you even know how terrible it would be programming 
websocket on client side to get it async and robust? This is my try to adopt reactive approach to building websocket 
clients with RxJava/RxKotlin

## Examples
With this thing you can simply do like this

```kotlin
import kotlinx.websocket.*
import kotlinx.websocket.gson.*
import com.squareup.okhttp.*
import rx.*
import rx.lang.kotlin.*
import java.util.concurrent.TimeUnit

// class we use to keep location, send and serialize to json
data class GeoLocation(val lat: Double, val lon: Double)

// here is just dummy geoPositionObservable that produces random coordinates
val geoPositionObservable =
        kotlin.sequence { GeoLocation(Math.random(), Math.random()) }.toObservable()

// create web socket that will check location every 5 seconds and
// send it if location changed since last time
//
// will automatically reconnect if loose connection
val geoPositionWebSocket = OkHttpClient().
        newWebSocket("ws://some-host:8080/ws").
        withGsonProducer(
                geoPositionObservable
                        .sample(5L, TimeUnit.SECONDS)
                        .distinctUntilChanged()).
        open()
```

Another example to receive events from server on Twitter stream:

```kotlin
import kotlinx.websocket.*
import kotlinx.websocket.gson.*
import com.squareup.okhttp.*
import rx.*
import rx.lang.kotlin.*

// class we use to keep tweet
data class Tweet(
        val user : String,
        val login : String,
        val text : String,
        val tags : List<String>
)

// here we can subscribe console logger, UI or something else
val observer = subscriber<Tweet>().
        onNext { tweet -> println(tweet) }

val twitterWebSocket = OkHttpClient().
        newWebSocket("ws://some-server:8080/ws").
        withGsonConsumer(observer).
        open()

```

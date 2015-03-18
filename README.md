# kotlin-rxokhttp-websocket
WebSocket bindings for Kotlin and RxKotlin based on OkHttp and Gson

With with thing you can simply do

```kotlin
import kotlinx.websocket.*
import kotlinx.websocket.gson.*
import com.squareup.okhttp.*
import rx.*
import rx.lang.kotlin.*

// class we use to keep location, send and serialize to json
data class GeoLocation(val lat : Double, val lon : Double)

// here is just dummy geoPositionObservable that produces random coordinates
val geoPositionObservable = 
    kotlin.sequence { GeoLocation(Math.random(), Math.random()) }.toObservable()

// create web socket that will check location every 5 seconds and
// send it if location changed since last time
// 
// will automatically reconnect if loose connection
val geoPositionWebSocket = OkHttpClient().
        newWebSocket<Any, GeoLocation>(
                Request.Builder().url("ws://some-host:8080/some-path/some-endpoint-ws").build(),
                geoPositionObservable.sample(5L, TimeUnit.SECONDS).distinctUntilChanged()
        )

```

Another example to receive events from server on Twitter stream:

```kotlin
import kotlinx.websocket.*
import kotlinx.websocket.gson.*
import com.squareup.okhttp.*
import rx.*
import rx.lang.kotlin.*

// class we use to keep tweet
data class Tweet(val user : String, val login : String, val text : String, val tags : List<String>)

val url = "ws://some-server:8080/mytwitterserver/my-websocket-endpoint"
val twitterWebSocket = OkHttpClient().with { setWriteTimeout(15L, TimeUnit.MILLISECONDS) }.
        newWebSocket<Tweet, Any>(Request.Builder().url(url).build())

// here we can subscribe console logger, UI or something else
val subscription = twitterWebSocket.incoming.subscribe(someSubscriber) 
```

package jetsocket.examples.geo

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

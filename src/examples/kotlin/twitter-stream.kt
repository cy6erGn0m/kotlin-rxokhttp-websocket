package jetsocket.examples.twitter

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


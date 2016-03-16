package kotlinx.websocket.test

import org.eclipse.jetty.server.*
import org.eclipse.jetty.servlet.*
import org.eclipse.jetty.websocket.jsr356.server.deploy.*
import org.junit.rules.*
import rx.Observable
import rx.lang.kotlin.*
import rx.schedulers.*
import java.net.*
import java.nio.*
import java.util.*
import java.util.concurrent.atomic.*
import java.util.concurrent.locks.*
import javax.websocket.*
import javax.websocket.server.*
import kotlin.concurrent.*

private val testServerMapper = WeakHashMap<WebSocketContainer, ServerTestResource>()
private val mapperLock = ReentrantLock()

private fun putResource(container : WebSocketContainer, resource : ServerTestResource) {
    mapperLock.withLock {
        testServerMapper[container] = resource
    }
}

private fun forgetResource(container : WebSocketContainer) {
    mapperLock.withLock {
        testServerMapper.remove(container)
    }
}

private fun getResource(container : WebSocketContainer) = mapperLock.withLock {
    testServerMapper[container]!!
}

private fun Session.getResource() = getResource(this.container)

class ServerTestResource : ExternalResource() {

    var toBeSent : Observable<String> = Observable.empty()
    val events = ReplaySubject<Pair<String, String?>>()

    val server = AtomicReference<Server?>()
    var port : Int = 0

    private var container : WebSocketContainer? = null

    override fun before() {
        super.before()

        port = guessFreePort()
        val server = Server(port)

        val handler = ServletContextHandler(ServletContextHandler.SESSIONS)
        handler.contextPath = "/"
        server.handler = handler

        val wsContainer = WebSocketServerContainerInitializer.configureContext(handler);
        wsContainer.addEndpoint(TestServerHandler::class.java)

        server.start()

        container = wsContainer
        putResource(wsContainer, this)

        this.server.set(server)
    }

    override fun after() {
        super.after()

        container?.let { forgetResource(it) }
        server.getAndSet(null)?.stop()
        events.onCompleted()
    }

    private fun guessFreePort() : Int = ServerSocket(0).use { it.localPort }
}

@ServerEndpoint(value="/ws")
class TestServerHandler {
    @OnOpen
    fun onConnect(session : Session) {
        session.getResource().events.onNext("onOpen" to null)

        session.getResource().toBeSent.subscribeOn(Schedulers.io()).subscribe {
            if (it == "EOF") {
                session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "EOF received"))
            } else {
                session.basicRemote.sendText(it)
            }
        }
    }

    @OnMessage
    fun onMessage(session : Session, text : String) {
        session.getResource().events.onNext("onMessage" to text)
    }

    @OnMessage
    fun onMessage(session : Session, bytes : ByteBuffer) {
        session.getResource().events.onNext("onMessage" to bytes.toString())
    }

    @OnMessage
    fun onPing(session : Session, bytes : PongMessage) {
        session.getResource().events.onNext("ping/pong" to null)
    }

    @OnClose
    fun onClose(session : Session) {
        session.getResource().events.onNext("onClose" to null)
    }

    @OnError
    fun onError(session : Session, error : Throwable) {
        session.getResource().events.onNext("onError" to error.toString())
    }
}
package kotlinx.websocket.test

import okio.Buffer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import org.junit.Rule
import org.junit.rules.ExternalResource
import rx.Observable
import rx.lang.kotlin.ReplaySubject
import rx.schedulers.Schedulers
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import javax.websocket.*
import javax.websocket.server.ServerEndpoint
import kotlin.concurrent.withLock

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

private fun Session.getResource() = getResource(this.getContainer())

[Rule]
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
        handler.setContextPath("/")
        server.setHandler(handler)

        val wsContainer = WebSocketServerContainerInitializer.configureContext(handler);
        wsContainer.addEndpoint(javaClass<TestServerHandler>())

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

    private fun guessFreePort() : Int = ServerSocket(0).use { it.getLocalPort() }
}

[ServerEndpoint(value="/ws")]
class TestServerHandler {
    [OnOpen]
    fun onConnect(session : Session) {
        session.getResource().events.onNext("onOpen" to null)

        session.getResource().toBeSent.subscribeOn(Schedulers.io()).subscribe {
            if (it == "EOF") {
                session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "EOF received"))
            } else {
                session.getBasicRemote().sendText(it)
            }
        }
    }

    [OnMessage]
    fun onMessage(session : Session, text : String) {
        session.getResource().events.onNext("onMessage" to text)
    }

    [OnMessage]
    fun onMessage(session : Session, bytes : ByteBuffer) {
        session.getResource().events.onNext("onMessage" to bytes.toString())
    }

    [OnMessage]
    fun onPing(session : Session, bytes : PongMessage) {
        session.getResource().events.onNext("ping/pong" to null)
    }

    [OnClose]
    fun onClose(session : Session) {
        session.getResource().events.onNext("onClose" to null)
    }

    [OnError]
    fun onError(session : Session, error : Throwable) {
        session.getResource().events.onNext("onError" to error.toString())
    }
}
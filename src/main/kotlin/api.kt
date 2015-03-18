package kotlinx.websocket

import com.squareup.okhttp.ws.WebSocket
import rx.Observable
import java.util.concurrent.atomic.AtomicReference

public enum class WebSocketState {
    CREATED
    CONNECTING
    CONNECTED
    CLOSED
}

public class ReactiveWebSocket<I>(
        val state: Observable<WebSocketState>,
        val incoming: Observable<in I>
) {
    val webSocketReference: AtomicReference<WebSocket?> = AtomicReference()
}

public fun ReactiveWebSocket<*>.close(code: CloseCode = CloseCodes.NORMAL_CLOSURE, message: String = ""): Unit =
        this.webSocketReference.get()?.close(code, message) // TODO may not work due to race!!!


public fun WebSocket.close(code: CloseCode = CloseCodes.NORMAL_CLOSURE, message: String = ""): Unit =
        close(code.code, message)

public fun WebSocket.safeClose(code: CloseCode = CloseCodes.NORMAL_CLOSURE, message: String = ""): Unit =
        try {
            close(code.code, message)
        } catch(ignore: Throwable) {
        }



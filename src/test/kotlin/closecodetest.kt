package kotlinx.websocket

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test as test

class WebSocketTest {
    test fun testGeneralCodes() {
        val close : CloseCode = CloseCodes.NORMAL_CLOSURE
        assertEquals(1000, close.code)

        val custom = object : CloseCode {
            override val code: Int
                get() = 777
        }

        assertEquals(777, custom.code)
    }

    test fun testFindCloseCodes() {
        val found = CloseCodes.getCloseCode(1000)
        assertNotNull(found)
        assert(found === CloseCodes.NORMAL_CLOSURE)
    }
}
package xyz.angm.terra3d.client.networking

import com.badlogic.gdx.utils.Array
import ktx.collections.*
import xyz.angm.terra3d.common.log

/** A client used for sending and receiving packages from a server.
 * @property disconnectListener Called when the client is disconnected.
 * @constructor Will create a socket for a local server. */
class Client() {

    private var client: ClientSocketInterface = LocalClientSocket.getSocket(this)
    private val listeners = GdxArray<(Any) -> Unit>()

    // Required since receiving multiple packets at once on different threads
    // would otherwise crash since GdxArrays cannot be iterated multiple times at once
    private val listenersIter = ThreadLocal.withInitial { Array.ArrayIterator(listeners) }
    var disconnectListener: () -> Unit = {}

    init {
        addListener { packet -> log.debug { "[CLIENT] Received packet of class ${packet.javaClass.name}" } }
    }

    /** Constructs a client for a remote server using netty.
     * @param ip The IP to connect to. */
    constructor(ip: String) : this() {
        client = NettyClientSocket(this)
        client.connect(ip)
    }

    /** Add a listener for received packets */
    fun addListener(listener: (packet: Any) -> Unit) {
        listeners.add(listener)
    }

    /** Remove all currently registered listeners. */
    fun clearListeners() = listeners.clear()

    /** Send the specified packet to server. */
    fun send(packet: Any) {
        client.send(packet)
        log.debug { "[CLIENT] Sent packet of class ${packet.javaClass.name}" }
    }

    internal fun receive(packet: Any) {
        listenersIter.get().reset()
        listenersIter.get().forEach { it(packet) }
    }

    internal fun disconnected() = disconnectListener()

    /** Dispose of the client. Object is unusable after this. */
    fun close() = client.close()
}

package xyz.angm.terra3d.server

import xyz.angm.terra3d.common.TICK_RATE

/** A runnable that will do a tick, meant to be run on a timer or scheduler.
 * @param server The server to tick on. */
class TickThread(private val server: Server) : Runnable {

    /** Do 1 tick. */
    override fun run() = server.engine.update(1f / TICK_RATE)
}
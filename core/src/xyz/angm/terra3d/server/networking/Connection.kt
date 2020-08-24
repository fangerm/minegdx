package xyz.angm.terra3d.server.networking

/** A wrapper for a connection. Subclass it and add other connection info if needed (eg. a socket)
 * @property ip The IP of the client connected
 * @property id The id of the client. IDs should be unique.*/
open class Connection(val ip: String, val id: Int)
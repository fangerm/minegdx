/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 9:56 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.networking

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.util.concurrent.GlobalEventExecutor
import xyz.angm.terra3d.common.*
import xyz.angm.terra3d.common.networking.FSTDecoder
import xyz.angm.terra3d.common.networking.FSTEncoder
import xyz.angm.terra3d.server.Server

/** A socket for online communication, using Netty. */
class NettyServerSocket(server: Server) : ServerSocketInterface(server) {

    private var connectionIndex = 0
    private val bossGroup = NioEventLoopGroup()
    private val workerGroup = NioEventLoopGroup()
    private val allChannels = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

    init {
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.RCVBUF_ALLOCATOR, FixedRecvByteBufAllocator(NETTY_BUFFER_SIZE))
            .childOption(ChannelOption.SO_RCVBUF, NETTY_BUFFER_SIZE)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        LengthFieldPrepender(LENGTH_SIZE),
                        LengthFieldBasedFrameDecoder(MAX_NETTY_FRAME_SIZE, 0, LENGTH_SIZE, 0, LENGTH_SIZE),
                        FSTEncoder(),
                        FSTDecoder(),
                        ServerHandler(this@NettyServerSocket)
                    )
                }
            })
            .bind(PORT)
            .sync()
    }

    override fun send(packet: Any, connection: Connection) {
        (connection as ChannelConnection).channel.writeAndFlush(packet)
    }

    override fun sendAll(packet: Any) {
        allChannels.writeAndFlush(packet)
    }

    override fun closeConnection(connection: Connection) {
        connection as ChannelConnection
        connection.channel.close()
        server.onDisconnected(connection)
        connections.removeValue(connection, false)
        allChannels.remove(connection.channel)
    }

    override fun close() {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    private fun addNewChannel(ctx: ChannelHandlerContext): ChannelConnection {
        val connection = ChannelConnection(ctx.channel(), ++connectionIndex)
        connections.add(connection)
        server.onConnected(connection)
        allChannels.add(connection.channel)
        return connection
    }


    internal class ChannelConnection(internal val channel: Channel, id: Int) : Connection(channel.remoteAddress().toString(), id)


    internal class ServerHandler(private val socket: NettyServerSocket) : ChannelInboundHandlerAdapter() {

        private lateinit var connection: ChannelConnection

        override fun channelActive(ctx: ChannelHandlerContext) {
            connection = socket.addNewChannel(ctx)
        }

        override fun channelInactive(ctx: ChannelHandlerContext?) {
            socket.closeConnection(connection)
        }

        override fun channelRead(ctx: ChannelHandlerContext, input: Any) = socket.server.received(connection, input)

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            log.info(cause) { "Exception during client communication:" }
            log.info { "Ignoring packet." }
        }
    }
}

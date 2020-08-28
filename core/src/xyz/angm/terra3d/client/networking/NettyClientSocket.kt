package xyz.angm.terra3d.client.networking

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import xyz.angm.terra3d.common.MAX_NETTY_FRAME_SIZE
import xyz.angm.terra3d.common.NETTY_BUFFER_SIZE
import xyz.angm.terra3d.common.PORT
import xyz.angm.terra3d.common.log
import xyz.angm.terra3d.common.networking.FSTDecoder
import xyz.angm.terra3d.common.networking.FSTEncoder

/** Client socket for non-local servers (multiplayer). Uses Netty. */
class NettyClientSocket(client: Client) : ClientSocketInterface(client) {

    private val workerGroup = NioEventLoopGroup()
    private lateinit var future: ChannelFuture

    override fun connect(ip: String) {
        future = Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.RCVBUF_ALLOCATOR, FixedRecvByteBufAllocator(NETTY_BUFFER_SIZE))
            .option(ChannelOption.SO_RCVBUF, NETTY_BUFFER_SIZE)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        LengthFieldPrepender(4),
                        LengthFieldBasedFrameDecoder(MAX_NETTY_FRAME_SIZE, 0, 4, 0, 4),
                        FSTEncoder(),
                        FSTDecoder(),
                        ClientHandler(client)
                    )
                }
            })
            .connect(ip, PORT)
            .sync()
    }

    override fun send(packet: Any) {
        future.channel().writeAndFlush(packet)
    }

    override fun close() {
        workerGroup.shutdownGracefully()
    }

    private class ClientHandler(private val client: Client) : ChannelInboundHandlerAdapter() {

        lateinit var channel: Channel

        override fun channelActive(ctx: ChannelHandlerContext) {
            channel = ctx.channel()
        }

        override fun channelRead(ctx: ChannelHandlerContext, input: Any) = client.receive(input)

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            log.info(cause) { "Exception during server communication:" }
            log.info { "Closing connection with server." }
            ctx.close()
        }
    }
}
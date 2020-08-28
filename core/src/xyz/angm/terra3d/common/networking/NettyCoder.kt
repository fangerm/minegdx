package xyz.angm.terra3d.common.networking

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import xyz.angm.terra3d.common.MAX_NETTY_FRAME_SIZE
import xyz.angm.terra3d.common.fst

/** An encoder for the Netty pipeline that turns any object sent into a byte array using FST. */
class FSTEncoder : MessageToByteEncoder<Any>() {

    private val len = IntArray(1)

    /** Encodes using FST. */
    override fun encode(ctx: ChannelHandlerContext, toWrite: Any, out: ByteBuf) {
        val arr = fst.asSharedByteArray(toWrite, len)
        if (toWrite is InitPacket) println(len[0])
        out.writeBytes(arr, 0, len[0])
    }
}

/** An encoder for the Netty pipeline that turns any bytes received into an object using FST. */
class FSTDecoder : ByteToMessageDecoder() {

    private val buf = ByteArray(MAX_NETTY_FRAME_SIZE)

    /** Decodes using FST. */
    override fun decode(ctx: ChannelHandlerContext?, input: ByteBuf, out: MutableList<Any>) {
        input.readBytes(buf, 0, input.readableBytes())
        out.add(fst.asObject(buf))
    }
}
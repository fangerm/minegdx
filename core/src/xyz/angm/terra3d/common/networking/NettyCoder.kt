package xyz.angm.terra3d.common.networking

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import xyz.angm.terra3d.common.fst

/** An encoder for the Netty pipeline that turns any object sent into a byte array using FST. */
class FSTEncoder : MessageToByteEncoder<Any>() {

    /** Encodes using FST. */
    override fun encode(ctx: ChannelHandlerContext, toWrite: Any, out: ByteBuf) {
        out.writeBytes(fst.asByteArray(toWrite))
    }
}

/** An encoder for the Netty pipeline that turns any bytes received into an object using FST. */
class FSTDecoder : ByteToMessageDecoder() {

    /** Decodes using FST. */
    override fun decode(ctx: ChannelHandlerContext?, input: ByteBuf, out: MutableList<Any>) {
        val byteArray = ByteArray(input.readableBytes())
        input.readBytes(byteArray)
        out.add(fst.asObject(byteArray))
    }
}
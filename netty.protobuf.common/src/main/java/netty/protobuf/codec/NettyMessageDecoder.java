package netty.protobuf.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import netty.protobuf.NettyMessage;

import java.util.List;

public class NettyMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		int nameLen = msg.readInt();
		String className = new String(msg.readBytes(nameLen).array(), "utf-8");
		byte[] rawbytes = new byte[msg.readableBytes()];
		msg.readBytes(rawbytes);
		NettyMessage message = new NettyMessage(className, rawbytes);
		out.add(message);
		//msg.release();
	}
}

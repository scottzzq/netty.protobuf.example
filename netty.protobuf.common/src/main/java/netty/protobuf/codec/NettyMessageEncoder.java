package netty.protobuf.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import netty.protobuf.NettyMessage;

import java.util.List;

public class NettyMessageEncoder extends MessageToMessageEncoder<NettyMessage> {
	@Override
	protected void encode(ChannelHandlerContext ctx, NettyMessage msg, List<Object> out) throws Exception {
		String className = msg.getClassName();
		ByteBufAllocator alloc = PooledByteBufAllocator.DEFAULT;
		ByteBuf buf = alloc.directBuffer(className.length() + 4);
		buf.writeInt(className.length());
		buf.writeBytes(className.getBytes());
		buf.writeBytes(msg.getMessage());
		out.add(buf);
	}
}

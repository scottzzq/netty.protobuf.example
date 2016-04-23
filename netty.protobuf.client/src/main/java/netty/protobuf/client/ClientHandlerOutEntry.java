package netty.protobuf.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class ClientHandlerOutEntry extends ChannelOutboundHandlerAdapter {
	private static Logger logger = LoggerFactory.getLogger(ClientHandlerInEntry.class.getName());

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
		logger.info("Close Channel");
		ctx.close(promise);
	}
}

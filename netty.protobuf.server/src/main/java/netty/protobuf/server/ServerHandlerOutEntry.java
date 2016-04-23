package netty.protobuf.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class ServerHandlerOutEntry extends ChannelOutboundHandlerAdapter {
	private static Logger logger = LoggerFactory.getLogger(ServerHandlerOutEntry.class.getName());
	// TCP链路关闭的时候
	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
		logger.info("client close");
		ctx.close(promise);
	}
}

package netty.protobuf.server;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import netty.protobuf.NettyMessage;
import netty.protobuf.proto.Auth;

public class ServerHandlerInEntry extends ChannelInboundHandlerAdapter {
	private static Logger logger = LoggerFactory.getLogger(ServerHandlerInEntry.class.getName());
	private static Map<String, Method> functionMap = new ConcurrentHashMap<String, Method>();
	private int port;
	static {
		try {
			functionMap.put(Auth.AuthRequest.class.getName(),
					ServerHandlerInEntry.class.getMethod("process", ChannelHandlerContext.class, NettyMessage.class));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public ServerHandlerInEntry(int port){
		this.port = port;
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
		NettyMessage msg = (NettyMessage) o;
		functionMap.get(msg.getClassName()).invoke(this, ctx, msg);
	}
	
	public void process(ChannelHandlerContext ctx, NettyMessage msg) throws InterruptedException {
		Auth.AuthResponse response = Auth.AuthResponse.newBuilder().setResultCode(0).setResultMessage("success").build();
		NettyMessage message = new NettyMessage(response.getClass().getName(), response.toByteArray());
		ctx.writeAndFlush(message);
	}
	
	// TCP链路建立成功
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("New Connection created! port:" + this.port);
	}

	// TCP链路断开
	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		logger.info("client close");
		ctx.close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state().equals(IdleState.READER_IDLE)) {
				logger.info("READER_IDLE");
				ctx.close().addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future) {
						logger.info("ctx.channel().close() complete! READIDLE Event");
					}
				});
			} else if (event.state().equals(IdleState.WRITER_IDLE)) {
				logger.info("WRITE_IDLE");
				ctx.close().addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future) {
						logger.info("ctx.channel().close() complete WRITE Event");
					}
				});
			} else if (event.state().equals(IdleState.ALL_IDLE)) {
				logger.info("READ_WRITE_IDLE");
				ctx.close().addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future) {
						logger.info("ctx.channel().close() complete ALLIDLE Event");
					}
				});
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (cause instanceof IOException){
			logger.warn(ctx.channel() + " IOException " + cause);
			ctx.channel().close();
		}else{
			logger.warn(ctx.channel() + " UNKNOW exceptionCaught " + cause);
			ctx.channel().close();
		}
	}
}

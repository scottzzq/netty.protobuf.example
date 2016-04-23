package netty.protobuf.proxy;

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
import io.netty.handler.timeout.ReadTimeoutException;
import netty.protobuf.NettyMessage;
import netty.protobuf.proto.Auth;

public class HandlerBackendInEntry extends ChannelInboundHandlerAdapter {
	private static Logger logger = LoggerFactory.getLogger(HandlerBackendInEntry.class.getName());
	private static Map<String, Method> functionMap = new ConcurrentHashMap<String, Method>();
	private final ProxyClientRequestCtx requestCtx;
	private final Servant pool;
	static {
		try {
			functionMap.put(Auth.AuthResponse.class.getName(),
					HandlerBackendInEntry.class.getMethod("process", ChannelHandlerContext.class, NettyMessage.class));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public HandlerBackendInEntry(ProxyClientRequestCtx requestCtx, Servant pool){
		this.requestCtx = requestCtx;
		this.pool = pool;
	}
	
	public ProxyClientRequestCtx getRequestCtx() {
		return requestCtx;
	}
	public Servant getPool() {
		return pool;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
		NettyMessage msg = (NettyMessage) o;
		functionMap.get(msg.getClassName()).invoke(this, ctx, msg);
	}

	public void process(ChannelHandlerContext ctx, NettyMessage msg) {
		try {
			Auth.AuthResponse resp = Auth.AuthResponse.parseFrom(msg.getMessage());
			requestCtx.copyResponse(resp);
			this.pool.release(ctx.channel());
			
			// 没有处理
			if (requestCtx.isProcessed() == false) {
				if (requestCtx.getBadConnCount() == 0) {
					if (requestCtx.getCount() == requestCtx.getCurrent()) {
						String respMsg = new String();
						for (int i = 0; i < requestCtx.getCount(); ++i) {
							respMsg += requestCtx.getReq().getUserId();
							respMsg += "----";
						}
						Auth.AuthResponse response = Auth.AuthResponse.newBuilder().setResultCode(0).setResultMessage(respMsg).build();
						NettyMessage responMessage = new NettyMessage(response.getClass().getName(),response.toByteArray());
						requestCtx.getCtx().writeAndFlush(responMessage);
						requestCtx.getCtx().channel().config().setAutoRead(true);
						requestCtx.setProcessed();
					}
				}else{
					String respMsg = "internal server error" + requestCtx.getReq().getUserId();
					Auth.AuthResponse response = Auth.AuthResponse.newBuilder().setResultCode(1).setResultMessage(respMsg).build();
					NettyMessage responMessage = new NettyMessage(response.getClass().getName(),response.toByteArray());
					requestCtx.getCtx().writeAndFlush(responMessage);
					requestCtx.getCtx().channel().config().setAutoRead(true);
					logger.warn(requestCtx.getCtx().channel() + " Client Respon Failed msg:" + respMsg);
					requestCtx.setProcessed();
				}
			}else{
				logger.warn("Request has respon to Client Ignore this Message");
			}
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}
	
	// TCP链路建立成功
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("New Connection created!!");
	}

	// TCP链路断开
	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		logger.info("client close");
		this.pool.release(ctx.channel());
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
		if (cause instanceof ReadTimeoutException) {
			logger.warn(ctx.channel() + " TIMEOUT " + cause);
			requestCtx.addTimeoutCount();
			if (requestCtx.isProcessed() == false) {
				String respMsg = "BackEnd Server Timeout" + requestCtx.getReq().getUserId();
				Auth.AuthResponse response = Auth.AuthResponse.newBuilder().setResultCode(1).setResultMessage(respMsg).build();
				NettyMessage responMessage = new NettyMessage(response.getClass().getName(), response.toByteArray());
				requestCtx.getCtx().writeAndFlush(responMessage);
				requestCtx.getCtx().channel().config().setAutoRead(true);
				logger.warn(requestCtx.getCtx().channel() + " Client Respon Failed msg:" + respMsg);
				requestCtx.setProcessed();
			}
			ctx.close();
		} else {
			logger.warn(ctx.channel() + " unknow exceptionCaught!!" + cause);
			if (requestCtx.isProcessed() == false) {
				String respMsg = "BackEnd Server Unknow Error" + requestCtx.getReq().getUserId();
				Auth.AuthResponse response = Auth.AuthResponse.newBuilder().setResultCode(1).setResultMessage(respMsg).build();
				NettyMessage responMessage = new NettyMessage(response.getClass().getName(), response.toByteArray());
				requestCtx.getCtx().writeAndFlush(responMessage);
				requestCtx.getCtx().channel().config().setAutoRead(true);
				logger.warn(requestCtx.getCtx().channel() + " Client Respon Failed msg:" + respMsg);
				requestCtx.setProcessed();
			}
			ctx.close();
		}
	}
}
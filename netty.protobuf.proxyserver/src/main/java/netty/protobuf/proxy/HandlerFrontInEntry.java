package netty.protobuf.proxy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import netty.protobuf.NettyMessage;
import netty.protobuf.proto.Auth;

public class HandlerFrontInEntry extends ChannelInboundHandlerAdapter {
	private static Logger logger = LoggerFactory.getLogger(HandlerFrontInEntry.class.getName());
	private static ProxyServer server;
	private static Map<String, Method> functionMap = new ConcurrentHashMap<String, Method>();
	static {
		try {
			functionMap.put(Auth.AuthRequest.class.getName(), HandlerFrontInEntry.class.getMethod("processAuthRequest",
					ChannelHandlerContext.class, NettyMessage.class));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public ProxyServer getServer() {
		return server;
	}

	public static void setProxyServer(ProxyServer serv) {
		HandlerFrontInEntry.server = serv;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
		NettyMessage msg = (NettyMessage) o;
		functionMap.get(msg.getClassName()).invoke(this, ctx, msg);
	}

	public void processAuthRequest(ChannelHandlerContext ctx, NettyMessage msg) {
		try {
			ctx.channel().config().setAutoRead(false);
			final Auth.AuthRequest req = Auth.AuthRequest.parseFrom(msg.getMessage());
			logger.info("request:" + req.getUserId() + " " + req.getPassword());
			final ProxyClientRequestCtx request = new ProxyClientRequestCtx(ctx, server.ServerGroup().getServerGroup(0).levelSize());
			request.setReq(req);
			
			int groupIndex = this.hashCode() % server.ServerGroup().poolSize();
			ServerGroup groupList = server.ServerGroup().getServerGroup(groupIndex);
			
			for (final ServantGroup group : groupList.getGroup()) {
				//find avaiable server in group
				final Servant svr = group.findUsableServant();
				if (svr == null){
					request.addBadConnCount();
					String respMsg = "Connection backend server error" + request.getReq().getUserId();
					Auth.AuthResponse response = Auth.AuthResponse.newBuilder().setResultCode(1).setResultMessage(respMsg).build();
					NettyMessage responMessage = new NettyMessage(response.getClass().getName(),response.toByteArray());
					request.getCtx().writeAndFlush(responMessage);
					request.getCtx().channel().config().setAutoRead(true);
					logger.warn(request.getCtx().channel() + " Client Respon Failed msg:" + respMsg);
					request.setProcessed();
					return;
				}
				Future<Channel> future = svr.acquire();
				future.addListener(new FutureListener<Channel>() {
					@Override
					public void operationComplete(Future<Channel> f) {
						if (f.isSuccess()) {
							Channel ch = f.getNow();
							//read timeout handler
							ch.pipeline().addLast("readtimeouthandler", new ReadTimeoutHandler(3));
							//user handler
							ch.pipeline().addLast("userhandler", new HandlerBackendInEntry(request, svr));
							Auth.AuthRequest request = Auth.AuthRequest.newBuilder().setUserId(req.getUserId() + svr.getAddress()).setPassword("abcde").build();
							NettyMessage message = new NettyMessage(request.getClass().getName(), request.toByteArray());
							ch.writeAndFlush(message);
						}else{
							request.addBadConnCount();
							String respMsg = "internal server error" + request.getReq().getUserId();
							Auth.AuthResponse response = Auth.AuthResponse.newBuilder().setResultCode(1).setResultMessage(respMsg).build();
							NettyMessage responMessage = new NettyMessage(response.getClass().getName(),response.toByteArray());
							request.getCtx().writeAndFlush(responMessage);
							request.getCtx().channel().config().setAutoRead(true);
							logger.warn(request.getCtx().channel() + " Client Respon Failed msg:" + respMsg);
							request.setProcessed();
						}
					}
				});
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
}

package netty.protobuf.client;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import netty.protobuf.NettyMessage;
import netty.protobuf.proto.Auth;
import java.util.UUID;

public class ClientHandlerInEntry extends ChannelInboundHandlerAdapter {
	private static Logger logger = LoggerFactory.getLogger(ClientHandlerInEntry.class.getName());
	private static Map<String, Method> functionMap = new ConcurrentHashMap<String, Method>();
	private Random random = new Random(1000);
	 
	static {
		try {
			functionMap.put(Auth.AuthResponse.class.getName(), ClientHandlerInEntry.class
					.getMethod("processAuthResponse", ChannelHandlerContext.class, NettyMessage.class));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Auth.AuthRequest request = Auth.AuthRequest.newBuilder().setUserId(UUID.randomUUID().toString()).setPassword("abcde").build();
		NettyMessage message = new NettyMessage(request.getClass().getName(), request.toByteArray());
		ctx.writeAndFlush(message);
		logger.info("Auth.AuthRequest:" + request.getUserId() + " "  + request.getPassword());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
		NettyMessage msg = (NettyMessage) o;
		functionMap.get(msg.getClassName()).invoke(this, ctx, msg);
	}

	public void processAuthResponse(ChannelHandlerContext ctx, NettyMessage msg) {
		try {
			Auth.AuthResponse resp = Auth.AuthResponse.parseFrom(msg.getMessage());
			//logger.info("Auth.AuthResponse:" + resp.getResultCode() + " " + resp.getResultMessage());
		
			Auth.AuthRequest request = Auth.AuthRequest.newBuilder().setUserId(random.nextLong() + "").setPassword("abcde").build();
			NettyMessage message = new NettyMessage(request.getClass().getName(), request.toByteArray());
			ctx.writeAndFlush(message);
			
			logger.info("Auth.AuthRequest:" + request.getUserId() + " "  + request.getPassword());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}
}
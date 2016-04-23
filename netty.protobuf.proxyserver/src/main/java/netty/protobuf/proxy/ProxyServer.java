package netty.protobuf.proxy;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import netty.protobuf.NettyConstant;
import netty.protobuf.codec.NettyMessageDecoder;
import netty.protobuf.codec.NettyMessageEncoder;

public class ProxyServer {
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	private ServerGroupPool server_groups;
	
	public ProxyServer(){
	}
	
	public void setPool(ServerGroupPool group){
		server_groups = group;
	}
	
	public final ServerGroupPool ServerGroup(){
		return server_groups;
	}
	
	public void start(EventLoopGroup eventLoop, int port) throws InterruptedException {
		this.server_groups.start();
		HandlerFrontInEntry.setProxyServer(this);
		ServerBootstrap b = new ServerBootstrap();// 引导辅助程序
		try {
			b.group(eventLoop);
			b.channel(NioServerSocketChannel.class);// 设置nio类型的channel
			b.handler(new LoggingHandler(LogLevel.TRACE));
			b.localAddress(new InetSocketAddress(port));// 设置监听端口
			b.childHandler(new ChannelInitializer<SocketChannel>() {// 有连接到达时会创建一个channel
				protected void initChannel(SocketChannel ch) throws Exception {
					// decoder
					ch.pipeline().addLast("headerdecoder", new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
					ch.pipeline().addLast("decoder", new NettyMessageDecoder());
					// encoder
					ch.pipeline().addLast("headerEncoder", new LengthFieldPrepender(4));
					ch.pipeline().addLast("encoder", new NettyMessageEncoder());
					// connection
					ch.pipeline().addLast("idle", new IdleStateHandler(NettyConstant.CLIENT_READ_TIMEOUT, NettyConstant.CLIENT_WRITE_TIMEOUT, NettyConstant.CLIENT_READ_WRITE_TIMEOUT));
					//user handler
					ch.pipeline().addLast("handler in entry", new HandlerFrontInEntry());
				}
			});
			ChannelFuture f = b.bind().sync();// 配置完成，开始绑定server，通过调用sync同步方法阻塞直到绑定成功
			logger.info(ProxyServerMain.class.getName() + " started and listen on " + f.channel().localAddress());
			f.channel().closeFuture().sync();// 应用程序会一直等待，直到channel关闭
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			eventLoop.shutdownGracefully().sync();// 关闭EventLoopGroup，释放掉所有资源包括创建的线程
		}
	}
}

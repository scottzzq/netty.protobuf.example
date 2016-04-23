package netty.protobuf.proxy;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import netty.protobuf.codec.NettyMessageDecoder;
import netty.protobuf.codec.NettyMessageEncoder;

public class Servant {
	class PoolHandler implements ChannelPoolHandler {
		private final Logger logger = LoggerFactory.getLogger(PoolHandler.class.getName());
		@Override
		public void channelAcquired(Channel ch) throws Exception {
		}

		@Override
		public void channelCreated(Channel ch) throws Exception {
			logger.info("Channel Create " + ch);
			ch.pipeline().addLast("framedecoder", new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
			ch.pipeline().addLast("decoder", new NettyMessageDecoder());
			// encoder
			ch.pipeline().addLast("headerEncoder", new LengthFieldPrepender(4));
			ch.pipeline().addLast("encoder", new NettyMessageEncoder());
		}

		@Override
		public void channelReleased(Channel ch) throws Exception {
			ch.pipeline().remove("userhandler");
			ch.pipeline().remove("readtimeouthandler");
		}
	}
	
	private static Logger logger = LoggerFactory.getLogger(Servant.class.getName());
	private SimpleChannelPool pool;
	private InetSocketAddress address;
	public final Lock lock = new ReentrantLock();
	private EventLoopGroup group;
	private boolean active;
	private int maxConn;
	private static final int cronTime = 30;
	
	public Servant(EventLoopGroup group, InetSocketAddress addr, int maxConn) {
		this.group = group;
		this.address = addr;
		this.active = false;
		this.maxConn = maxConn;
	}

	public boolean isActive() {
		return active;
	}

	public void disable() {
		this.active = false;
	}

	public void start() {
		final Bootstrap cb = new Bootstrap();
		cb.group(group).channel(NioSocketChannel.class);
		cb.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
		//pool = new SimpleChannelPool(cb.remoteAddress(address), new PoolHandler());
		pool = new FixedChannelPool(cb.remoteAddress(address), new PoolHandler(), maxConn);
		// crontab
		this.group.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				logger.info("FixRate Crontab task:" + address);
				Future<Channel> future = acquire();
				//async
				future.addListener(new FutureListener<Channel>() {
					@Override
					public void operationComplete(Future<Channel> f) {
						if (f.isSuccess()) {
							logger.info("Reconnect Servant:" + address + " Success!");
							Channel ch = f.getNow();
							ch.pipeline().addLast("userhandler", new NettyMessageDecoder());
							ch.pipeline().addLast("readtimeouthandler", new NettyMessageDecoder());
							active = true;
							release(ch);
						} else {
							logger.warn("Reconnect Servant:" + address + " Fail!");
							active = false;
						}
					}
				});
			}
		}, 0,cronTime, TimeUnit.SECONDS);
	}

	public void stop() {
		pool.close();
	}
	
	public Future<Channel> acquire() {
		return pool.acquire();
	}

	public void release(Channel ch) {
		pool.release(ch);
	}

	public final InetSocketAddress getAddress() {
		return address;
	}

	public int getMaxConn() {
		return maxConn;
	}
}

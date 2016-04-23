package netty.protobuf.proxy;

import io.netty.channel.ChannelHandlerContext;
import netty.protobuf.proto.Auth;

public class ProxyClientRequestCtx {
	private ChannelHandlerContext ctx;
	private int count;
	private Auth.AuthResponse[] respons;
	private int current;
	private Auth.AuthRequest req;
	private int badConnCount;
	private int timeoutCount;
	
	private boolean processed;
	
	public ProxyClientRequestCtx(ChannelHandlerContext ctx, int count){
		this.ctx = ctx;
		this.count = count;
		this.current = 0;
		this.badConnCount = 0;
		this.processed = false;
		this.respons = new Auth.AuthResponse[count];
	}
	
	public int getCount() {
		return count;
	}
	
	public Auth.AuthResponse[] getRespons() {
		return respons;
	}
	
	public ChannelHandlerContext getCtx() {
		return ctx;
	}
	
	public void copyResponse(Auth.AuthResponse resp){
		respons[current++] = resp;
	}
	
	public int getCurrent(){
		return current;
	}

	public Auth.AuthRequest getReq() {
		return req;
	}

	public void setReq(Auth.AuthRequest req) {
		this.req = req;
	}

	public int getBadConnCount() {
		return badConnCount;
	}

	public void addBadConnCount() {
		this.badConnCount++;
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed() {
		this.processed = true;
	}

	public int getTimeoutCount() {
		return timeoutCount;
	}

	public void addTimeoutCount() {
		this.timeoutCount++;
	}
}

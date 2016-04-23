package netty.protobuf;

public class NettyMessage {
	private String className;
	private byte[] message;
	public NettyMessage(String className, byte[] message){
		this.className = className;
		this.message = message;
	}
	public String getClassName() {
		return className;
	}
	public byte[] getMessage() {
		return message;
	}
}

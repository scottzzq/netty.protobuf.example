package netty.protobuf.proxy;

import java.util.ArrayList;
import java.util.List;

public class ServerGroupPool {
	private List<ServerGroup> groupPool;

	public ServerGroupPool(){
		groupPool = new ArrayList<ServerGroup>();
	}
	
	public void start(){
		for (ServerGroup group: groupPool){
			group.start();
		}
	}
	
	public void addGroup(ServerGroup svrGroup){
		groupPool.add(svrGroup);
	}
	
	public int poolSize(){
		return groupPool.size();
	}
	
	public final ServerGroup getServerGroup(int index){
		return groupPool.get(index);
	}
}

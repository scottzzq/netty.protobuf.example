package netty.protobuf.proxy;

import java.util.ArrayList;
import java.util.List;

public class ServerGroup {
	private List<ServantGroup> group;
	
	public ServerGroup(){
		group = new ArrayList<ServantGroup>();
	}
	
	public void start(){
		for (ServantGroup svrgroup:group){
			svrgroup.start();
		}
	}
	
	public void stop(){
		for (ServantGroup svrgroup:group){
			svrgroup.stop();
		}
	}
	
	public void addServantGroup(ServantGroup svrs){
		group.add(svrs);
	}
	
	public final List<ServantGroup> getGroup(){
		return group;
	}
	
	public int levelSize(){
		return group.size();
	}
}

package netty.protobuf.proxy;

import java.util.ArrayList;
import java.util.List;

public class ServantGroup {
	private String groupName;
	private int id;
	private List<Servant> backSvrList;
	private long callNum;
	
	public ServantGroup() {
		backSvrList = new ArrayList<Servant>();
		callNum = 0;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public final List<Servant> getBackSvrList() {
		return backSvrList;
	}

	public void addServant(Servant svr) {
		backSvrList.add(svr);
	}

	public void start(){
		for (Servant svr: backSvrList){
			svr.start();
		}
	}
	
	public void stop() {
		for (Servant svr: backSvrList){
			svr.stop();
		}
	}
	
	public Servant findUsableServant(){
		for (int i = 0; i < backSvrList.size(); ++i){
			Servant svr = backSvrList.get((int) (callNum++ %  backSvrList.size()));
			if (svr.isActive()){
				return svr;
			}
		}
		return null;
	}
}

package util;

public class MulticastMessage extends TimeStampedMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String groupName;
	private String id;
	private String sender;//add var sender
	public MulticastMessage(){
		super();
		this.groupName = null;
		this.id = null;
		this.sender = null;
	}
	
	public MulticastMessage(MulticastMessage m){
		super(m);
		this.groupName = m.groupName;
		this.id = m.getId();
		this.sender = m.getSrc();
	}
	
	public MulticastMessage(String dest, String kind, Object data){
		super(dest, kind, data);
	}
	
	public String getSender(){
		return this.sender;
	}
	
	public void setSender(String sender){
		this.sender = sender;
	}
	@Override
	public int hashCode(){
		int rst = 31;
		rst = 30 * rst + this.getId().hashCode();
		rst = 29 * rst + this.getKind().hashCode();
		rst = 28 * rst + this.getMessage().hashCode();
		return rst;
	}
	
	@Override
	public boolean equals(Object obj){
		MulticastMessage m = (MulticastMessage) obj;
		return this.getId().equals(m.getId()) && this.getKind().equals(m.getKind()) && this.getMessage().equals(m.getMessage());
	}
	@Override
	public String toString(){
		return "From: " + this.getSender() + ", To: " + this.getDest() + ", kind: "+ this.getKind() + ", sequence # "+ this.getSeqNum() +" Group "+this.getGroupName()+ " Time Stamp " + this.getClock().getClock() + "\nContent: " + this.getMessage();
	}
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}

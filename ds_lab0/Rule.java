/*
 * Class Rule
 */
public class Rule{
	private String action;
	private String src;
	private String dest;
	private String kind;
	private int seqNum;
	private Boolean duplicate;

	public Rule(){
		this.action = null;
		this.src = null;
		this.dest = null;
		this.kind = null;
		this.seqNum = -1;
	}
	public Rule(String action, String src, String dest, String kind, int seqNum, Boolean dup){
		this.setAction(action);
		this.setSrc(src);
		this.setDest(dest);
		this.setKind(kind);
		this.setSeqNum(seqNum);
		this.setDuplicate(dup);
	}
	public Boolean isMatch(Message message){
		if (src != null && !src.equals(message.getSource())){
			return false;
		}
		if (dest != null && !dest.equals(message.getDest())){
			return false;
		}
		if (kind != null && !kind.equals(message.getKind())){
			return false;
		}
		if (seqNum != -1 && seqNum != message.seqNumber){
			return false;
		}
		if (duplicate != null && duplicate != message.duplicate){
			return false;
		}
		return true;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getSrc(){
		return src;
	}

	public void setSrc(String src){
		this.src = src;
	}

	public String getDest(){
		return dest;
	}

	public void setDest(String dest){
		this.dest = dest;
	}

	public String getKind(){
		return kind;
	}

	public void setKind(String kind){
		this.kind = kind;
	}

	public int getSeqNum(){
		return seqNum;
	}

	public void setSeqNum(int seqNum){
		this.seqNum = seqNum;
	}
	public Boolean getDuplicate() {
		return duplicate;
	}
	public void setDuplicate(Boolean duplicate) {
		this.duplicate = duplicate;
	}
}
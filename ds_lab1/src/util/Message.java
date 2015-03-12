package util;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/*
 * The class to hold message info
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 7526472295622776147L;
	private String src;
	private String dest;
	private String kind;
	private String message;
	int seqNumber;
	Boolean duplicate = false;
	
	public Message(Message m){
		this.setMessage(m.getMessage());
		this.setKind(m.getKind());
		this.setDest(m.getDest());
		this.setSeqNum(m.getSeqNum());
		this.setSrc(m.getSrc());
	}
	public Message(String dest, String kind, Object data){
		this.setMessage(data.toString());
		this.setKind(kind);
		this.setDest(dest);
	}
	public String toString(){
		return "From: " + src + ", To: " + dest + ", kind: "+ kind + ", sequence # "+ seqNumber + "\nContent: " + message;
	}
	public void setSrc(String source){
		this.src = source;
	}
	public String getSrc(){
		return src;
	}
	public void setSeqNum(int sequenceNumber){
		this.seqNumber = sequenceNumber;
	}
	public int getSeqNum(){
		return seqNumber;
	}
	public void setDuplicate(Boolean dupe){
		this.duplicate = dupe;
	}
	public Boolean getDuplicate(){
		return duplicate;
	}

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	private void readObject(ObjectInputStream aInputStream)
			throws ClassNotFoundException, IOException {
		     aInputStream.defaultReadObject();
		  }
	
	private void writeObject(ObjectOutputStream aOutputStream)
			throws IOException {
		      aOutputStream.defaultWriteObject();
		    }
}
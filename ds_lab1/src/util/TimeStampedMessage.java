package util;

import java.util.ArrayList;

import clock.ClockService;

public class TimeStampedMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ClockService clock;
	private boolean log;
	private ArrayList<TimeStampedMessage> concurrent;
	public TimeStampedMessage(TimeStampedMessage m) {
		super(m);
		this.log = m.isLog();
		concurrent = new ArrayList<TimeStampedMessage>();
	}
	
	public TimeStampedMessage(String dest, String kind, Object data){
		super(dest,kind,data);
		concurrent = new ArrayList<TimeStampedMessage>();

	}

	@Override
	public String toString(){
		return "From: " + getSrc() + ", To: " + getDest() + ", kind: "+ getKind() + ", sequence # "+ seqNumber + " timeStamp: "+this.getClock().getClock() +"\nContent: " + getMessage();
	}

	public ClockService getClock() {
		return clock;
	}

	public void setClock(ClockService clock) {
		this.clock = clock;
	}

	public ArrayList<TimeStampedMessage> getConcurrent() {
		return concurrent;
	}

	public void setConcurrent(ArrayList<TimeStampedMessage> concurrent) {
		this.concurrent = concurrent;
	}

	public boolean isLog() {
		return log;
	}

	public void setLog(boolean log) {
		this.log = log;
	}
	public TimeStampedMessage copyof(){
		TimeStampedMessage rst = new TimeStampedMessage(this);
		rst.setClock(this.getClock());
		return rst;
	}
}


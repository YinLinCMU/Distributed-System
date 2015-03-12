package clock;

import java.io.Serializable;

import util.Configuration;

public abstract class ClockService implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// ObjectFactory
	public static ClockService selectClock(String clockType, Configuration conf, String localName){
		if (clockType.equalsIgnoreCase("vector")){
			return new VectorClock(conf, localName);
		}
		else if (clockType.equalsIgnoreCase("logical")){
			return new LogicalClock();
		}
		else{
			System.err.println("No this type of clock");
			System.exit(1);
		}
		return null;
	}
	
	public abstract void add();
	public abstract void update(ClockService timeStamp);
	public abstract int compareTo(ClockService clock);
	public abstract String getType();
	public abstract Object getClock();
	public abstract ClockService copyOf();

}

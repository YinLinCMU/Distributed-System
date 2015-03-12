package clock;

import java.util.Hashtable;
import java.util.Map;

import util.*;

public class VectorClock extends ClockService {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Hashtable<String, Integer> clock;
	private String host;
	
	public VectorClock(){
		this.clock = new Hashtable<String, Integer>();
	}

	public VectorClock(Configuration conf, String localName){
		this.host = localName;
		this.clock = new Hashtable<String, Integer>();
		for (Node node : conf.getConfiguration()){
			this.clock.put(node.getName(), 0);
		}
	}
	@Override
	synchronized public void add(){//add local name's vector time
		if (this.clock.containsKey(this.host)){
			this.clock.put(host, this.clock.get(host) + 1);
		}
	}

	@Override
	synchronized public void update(ClockService messageClock){
		if (messageClock == null){
			System.out.println("No timestamp");
			return;
		}
		@SuppressWarnings("unchecked")
		Hashtable<String, Integer> messageTime = (Hashtable<String, Integer>)(messageClock.getClock());
		for ( Map.Entry<String, Integer> e : this.clock.entrySet()){
			if (messageTime.containsKey(e.getKey())){
				if (!e.getKey().equals(this.host)){//if not the receiver
					this.clock.put(e.getKey(), Math.max(messageTime.get(e.getKey()), e.getValue()));
				}
				else{//receiver should add 1
					this.clock.put(e.getKey(), Math.max(messageTime.get(e.getKey()), e.getValue()) + 1);
				}
			}
		}
	}
	
	synchronized public Hashtable<String, Integer> getClock(){
		return this.clock;
	}

	@Override
	public int compareTo(ClockService clock) {
		VectorClock vect = (VectorClock) clock;
		boolean bigger = true;
		boolean smaller = true;
		for (Map.Entry<String, Integer> e : this.clock.entrySet()){
			if (e.getValue() > vect.clock.get(e.getKey())){
				smaller = false;
			}
			if (e.getValue() < vect.clock.get(e.getKey())){
				bigger = false;
			}
		}
		// all values is bigger
		if (bigger == true && smaller == false){
			return 1;
		}
		// all values is smaller
		if (bigger == false && smaller == true){
			return -1;
		}
		// not all bigger nor smaller, concurrent
		return 0;
	}

	@Override
	public String toString(){
		return "VectorClock is " + this.clock + ".\n";
	}
	
	public String getType(){
		return "Vector Clock";
	}

	@Override
	public VectorClock copyOf() {
		VectorClock rst = new VectorClock();
		rst.clock = clock;
		return rst;
	}

}

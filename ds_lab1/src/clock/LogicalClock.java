package clock;

public class LogicalClock extends ClockService{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int clock;
	public LogicalClock(){
		this.clock = 0;
	}
	@Override
	synchronized public void add(){
		this.clock++;
	}
	@Override
	synchronized public void update(ClockService messageClock){
		this.clock = Math.max(this.clock, (Integer)(messageClock.getClock())) + 1;
	}
	
	synchronized public Integer getClock(){
		return this.clock;
	}
	@Override
	public int compareTo(ClockService messageClock){
		LogicalClock lc = (LogicalClock) messageClock;
		if (this.clock < lc.clock){
			return -1;
		}
		if (this.clock > lc.clock){
			return 1;
		}
		return 0;
	}
	@Override
	public String toString(){
		return "LogicalClock is " + this.clock + ".\n";
	}
	
	public String getType(){
		return "Logical Clock";
	}
	@Override
	public LogicalClock copyOf(){
		LogicalClock rst = new LogicalClock();
		rst.clock = clock;
		return rst;
	}
}

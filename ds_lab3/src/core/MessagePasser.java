package core;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import clock.ClockService;
import util.CSState;
import util.Configuration;
import util.MulticastMessage;
import util.Node;
import util.Rule;
import util.TimeStampedMessage;

public class MessagePasser{

	private String localName;
	private int port;
	private int seqNum = 0;
	private int multiNum = 0;
	int sendCount;
	int receiveCount;
	
	private HashMap<String, SocketChannel> pool = new HashMap<String, SocketChannel>();
	private static Queue<TimeStampedMessage> delay_send_queue = new LinkedList<TimeStampedMessage>();
	private static ConcurrentLinkedQueue<TimeStampedMessage> receive_queue = new ConcurrentLinkedQueue<TimeStampedMessage>();
	//private static ConcurrentLinkedQueue<TimeStampedMessage> delay_receive_queue = new ConcurrentLinkedQueue<TimeStampedMessage>();
	
	String configFile;
	private Configuration conf;
	private ClockService clock;
	Selector selector;

	//every sender has one multicast, so every mp has one mc 
	multicast mc;
	CSState state;
	boolean voted;
	int voteCount;
	
	public MessagePasser(String configuration_filename, String local_name, String clockType){
		this.configFile = configuration_filename;
		this.setLocalName(local_name);
		try {
			conf = YalmParser.parse(configFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setPort(conf.getNode(localName).getPort());
		try {
			this.selector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		clock = ClockService.selectClock(clockType, conf, localName);
		this.mc = new multicast(this);
		state = CSState.RELEASED;
		voted = false;
		sendCount = 0;
		receiveCount = 0;
	} 
	
	void send(TimeStampedMessage message) throws IOException{
		this.conf=(YalmParser.parse(this.getConfigFile()));
		message.setSeqNum(seqNum++);
		ArrayList<Rule> r = conf.getSendRules();
		if(r == null)	return;
		if(message != null){
			for(Rule rule : r){
				if(rule.isMatch(message)){
					String action = rule.getAction();
					switch (action){
					case "drop":		break;
					case "duplicate":	TimeStampedMessage dup_m = new TimeStampedMessage(message);
										if (message.isLog()){
											TimeStampedMessage mess = new TimeStampedMessage(message);
											message.setLog(false);
											sending(mess, "logger");
											TimeStampedMessage dup_mess = new TimeStampedMessage(dup_m);
											dup_m.setLog(false);
											sending(dup_mess, "logger");
										}
										dup_m.setDuplicate(true);
										sending(dup_m, dup_m.getDest());
										message.setDuplicate(false);
										sending(message, message.getDest());
										break;
					case "delay":		delay_send_queue.add(message);
										break;
					}
					return;
				}				
			}
			if (message.isLog()){
				TimeStampedMessage mess = new TimeStampedMessage(message);
				message.setLog(false);
				sending(mess, "logger");
			}
			
			while(!delay_send_queue.isEmpty()){ //after one non-delay mess has been send
				TimeStampedMessage tmp = delay_send_queue.poll();
				if (tmp.isLog()){
					TimeStampedMessage mess = new TimeStampedMessage(tmp);
					tmp.setLog(false);
					sending(mess, "logger");
				}
				sending(tmp, tmp.getDest());
			}
			sending(message, message.getDest());
		}
	}

	void sending(TimeStampedMessage message, String dest) throws IOException {
		if(message == null)	return;
		else{
			String ip = null;
			int port = -1;

			Node node = conf.getNode(dest);
			ip = node.getIp();
			port = node.getPort();
			
			SocketChannel socketChannel = null;
			
			if (pool.containsKey(node.getName())){
				socketChannel = pool.get(node.getName());
				System.out.println("from pool: "+node.getName());
			}
			else{
				SocketAddress socketAddress = new InetSocketAddress(ip, port);
				socketChannel = SocketChannel.open();				
				try {
					socketChannel.connect(socketAddress);
				} catch (IOException e) {
					System.err.println("The receiver is offline, cannot connect");
					return;
				}
				socketChannel.configureBlocking(false);
				socketChannel.register(selector, SelectionKey.OP_READ);
				pool.put(dest, socketChannel);
			}
			message.setClock(clock.copyOf());
			// increment of clock
			if (!message.isLog()&&!message.getDuplicate()){
				clock.add();
			}
			//byte[] messbyte = null;//convert to byte
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for(int i = 0; i < 4; i++) { baos.write(0); }
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(message);
			oos.close();
			final ByteBuffer wrap = ByteBuffer.wrap(baos.toByteArray());
			wrap.putInt(0, baos.size()-4);
			try {
				int len = socketChannel.write(wrap);
				System.out.println("Message sent bytes:" + len );
			} catch (IOException e) {
				e.printStackTrace();
			}
			sendCount++;
			System.out.println("Message sent: "+message+", Total sent: "+sendCount);
		}
	}
	

	TimeStampedMessage receiving(){
		return receive_queue.poll();
	}
	
	
	/*
	 *  Getters and setters
	 */
	public multicast getMc(){
		return this.mc;
	}
	
	public String getLocalName() {
		return localName;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getSeqNum() {
		return seqNum;
	}

	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}
	
	public String getConfigFile(){
		return this.configFile;
	}

	public void setConf(Configuration conf){
		this.conf = conf;
	}
	
	public Configuration getConf(){
		return this.conf;
	}
	
	public Selector getSelector(){
		return this.selector;
	}
	
	public ConcurrentLinkedQueue<TimeStampedMessage> getRecivedQueue(){
		return receive_queue;
	}
	
	public ClockService getClock(){
		return this.clock;
	}
	
	public HashMap<String, SocketChannel> getPool(){
		return this.pool;
	}
	
	
	// start receiving thread 
	public void listen(){
		Thread thread = new Thread(new Receiver(this));
		thread.start();
	}
	
	public void run() throws IOException{
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader buffedReader = new BufferedReader(input);
		
		// infinite loop to perform the interaction
		while (true){
			System.out.println("Hello, This is " + localName);
			System.out.println("state is "+state);
			System.out.println("vote = "+voted);
			System.out.println("Please chose \"send\",\"group send\",\"receive\", \"request\", \"release\" , \"message count\"or \"exit\"");
			
			String line = buffedReader.readLine().trim();
			switch(line){
			case "send":System.out.println("Enter the destination name:\n");
			
						String dest = buffedReader.readLine().trim();
						if (!conf.hasNode(dest)){
							System.err.println("The Node is not exist!");
							continue;
						}
						System.out.println("Enter the kind of message:\n");
						String kind = buffedReader.readLine().trim();
						System.out.println("Enter the content of the message:\n");
						String data = buffedReader.readLine();
						TimeStampedMessage message = new TimeStampedMessage(dest, kind, data);
						message.setSrc(localName);
						
						
						// see if the message need to be logged
						System.out.println("Log the message(Y/N)");
						String log = buffedReader.readLine();
						if (log.equalsIgnoreCase("y")){
							message.setLog(true);
						}
						else {
							message.setLog(false);
						}
						
						// send original message to dest
			
						send(message);
						break;
			case "group send":System.out.println("Enter the group name:\n");
			
							String groupName = buffedReader.readLine().trim();
							if (!conf.hasGroup(groupName)){
								System.err.println("The group is not exist!");
								continue;
							}
							System.out.println("Enter the kind of message:\n");
							String groupKind = buffedReader.readLine().trim();
							System.out.println("Enter the content of the message:\n");
							String groupData = buffedReader.readLine();
							MulticastMessage groupMessage = new MulticastMessage(groupName, groupKind, groupData);
							groupMessage.setSrc(localName);
							groupMessage.setSender(localName);
							groupMessage.setId(localName+String.valueOf(multiNum));
							multiNum++;
							
							// see if the message need to be logged
							System.out.println("Log the message(Y/N)");
							String groupLog = buffedReader.readLine();
							if (groupLog.equalsIgnoreCase("y")){
								groupMessage.setLog(true);
							}
							else {
								groupMessage.setLog(false);
							}
							//for sender, just call bmulti
							this.getMc().bMulticast(groupMessage, groupName, this);
							
							break;
			case "receive":try {
								TimeStampedMessage receiveMess = receiving();
								if(receiveMess == null)	System.out.println("There is no message!");
								else	System.out.println(receiveMess);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
			case "exit":for (Map.Entry<String, SocketChannel> e : pool.entrySet()){
								e.getValue().close();
							}
							System.exit(0);
							break;
			case "request":
					synchronized(state){
						if (state == CSState.RELEASED){//p enter the cs
							state = CSState.WANTED;
							String gName = conf.getNode(localName).getMemberOf().get(0);
							voteCount = conf.getGroup(gName).getMembers().size();
							MulticastMessage request = new MulticastMessage(gName, "request"," "); 
							request.setSrc(localName);
							request.setSender(localName);
							request.setId(localName+String.valueOf(multiNum));
							
							multiNum++;
							this.getMc().bMulticast(request, gName, this);
							
						}
						else{
							if (state == CSState.HELD){
								System.err.println("holding lock");
							}
							else{//WANTED
								System.err.println("requesting lock");
							}
						}
					}
					break;
			case "release":
//					synchronized(state){
						if (state == CSState.HELD){
							state = CSState.RELEASED;
							String gName = conf.getNode(localName).getMemberOf().get(0);
							MulticastMessage release = new MulticastMessage(gName, "release","");
							release.setSrc(localName);
							release.setSender(localName);
							release.setId(localName+String.valueOf(multiNum));
							
							multiNum++;
							this.getMc().bMulticast(release, gName, this);
						}
						else{
							System.err.println("Not holding lock");
						}
//					}
					break;
			case "message counts": 	System.out.println("Total sent: "+sendCount+". Total received: "+receiveCount);		
		    default: System.err.println("invalid input, Please enter again");
		    		break;
			
			}
		}
	}
	
	// Main func
	public static void main(String[] args) throws IOException{
		if (args.length != 3){
			System.err.println("Usage: $java -cp jar_file core/MessagePasser <config_file> <localName> <clockType>");
			System.exit(1);
		}
		MessagePasser mp = new MessagePasser(args[0],args[1], args[2]);
		mp.listen();
		mp.run();
		
	}

	public int getMultiNum() {
		return multiNum;
	}

	public void setMultiNum(int multiNum) {
		this.multiNum = multiNum;
	}
}

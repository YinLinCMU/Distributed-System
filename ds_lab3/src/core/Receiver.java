package core;
import java.io.ByteArrayInputStream;
import java.io.IOException;
//import java.io.IOException;
import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
//import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
//import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import clock.ClockService;
import util.CSState;
import util.MulticastMessage;
//import util.Node;
import util.Rule;
import util.TimeStampedMessage;
import util.Configuration;
/*
 * The thread checking receiving messages using NIO 
 * selector is used to monitoring all the live sockets.
 */
public class Receiver implements Runnable{
	
	ConcurrentLinkedQueue<TimeStampedMessage> receivedQueue;
	ConcurrentLinkedQueue<TimeStampedMessage> delay_receive_queue;
	Queue<MulticastMessage> delay_send_queue = new LinkedList<MulticastMessage>();
	Queue<MulticastMessage> delay_group_queue = new LinkedList<MulticastMessage>();
	
//	Queue<MulticastMessage> requestQueue = new LinkedList<MulticastMessage>();
	
	int voteCounts;
	int receiveCounts;

	//for every receiver, it should have its own CO queue 
	private PriorityQueue<MulticastMessage> causal_order_queue = new PriorityQueue<MulticastMessage>(20, new Comparator<MulticastMessage>(){
		@Override
		public int compare(MulticastMessage o1, MulticastMessage o2) {
			
			ClockService c1 = o1.getClock();
			ClockService c2 = o2.getClock();
			int rst = c1.compareTo(c2);
			if (rst == 0){
				o1.getConcurrent().add(o2);
				o2.getConcurrent().add(o1);
			}
			return rst;
		}});
	private PriorityQueue<MulticastMessage> requestQueue = new PriorityQueue<MulticastMessage>(20, new Comparator<MulticastMessage>(){
		@Override
		public int compare(MulticastMessage o1, MulticastMessage o2) {
			
			ClockService c1 = o1.getClock();
			ClockService c2 = o2.getClock();
			int rst = c1.compareTo(c2);
			if (rst == 0){
				o1.getConcurrent().add(o2);
				o2.getConcurrent().add(o1);
			}
			return rst;
		}});
	
	private PriorityQueue<MulticastMessage> groupQueue = new PriorityQueue<MulticastMessage>(20, new Comparator<MulticastMessage>(){
		@Override
		public int compare(MulticastMessage o1, MulticastMessage o2) {
			
			ClockService c1 = o1.getClock();
			ClockService c2 = o2.getClock();
			int rst = c1.compareTo(c2);
			if (rst == 0){
				o1.getConcurrent().add(o2);
				o2.getConcurrent().add(o1);
			}
			return rst;
		}});
	
	int port;
	HashMap<String, SocketChannel> pool = null;
	Selector selector;
	ClockService clock;
	Configuration conf;
	String configFile;
	String localName;
	MessagePasser mp;
	multicast mc;
	public Receiver(MessagePasser mp){
		this.mp = mp;
		this.mc = mp.getMc();
		this.port = mp.getPort();
		this.receivedQueue = mp.getRecivedQueue();
		this.pool = mp.getPool();
		this.selector = mp.getSelector();
		this.clock = mp.getClock();
		this.conf = mp.getConf();
		this.configFile = mp.getConfigFile();
		this.localName = mp.getLocalName();
		delay_receive_queue = new ConcurrentLinkedQueue<TimeStampedMessage>();
		voteCounts = 0;
		this.receiveCounts = mp.receiveCount;
	}
	
	@Override
	public void run() {
		ServerSocketChannel serverChannel;
		SocketChannel channel = null;
		
		try {
			serverChannel = ServerSocketChannel.open();
			ServerSocket serverSocket = serverChannel.socket();			
			serverSocket.bind(new InetSocketAddress(port));
			serverChannel.configureBlocking(false);			
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);

			while(true){
				int n = selector.selectNow();
				
				if (n == 0){
					continue;
				}
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				
				while(it.hasNext()){
					SelectionKey key = (SelectionKey) it.next();
					it.remove();
					if (key.isAcceptable()){
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						channel = server.accept();
						registerChannel(selector, channel, SelectionKey.OP_READ);
					}
					
					if (key.isReadable()){
						TimeStampedMessage[] messages = readDataFromSocket(key);
						if (messages == null || messages.length == 0){
						//	key.cancel();
							continue;
						}
						for(TimeStampedMessage message : messages){
							if (!pool.containsKey(message.getSrc())){
								pool.put(message.getSrc(), (SocketChannel) key.channel());
							}

						/////////// group message////////////////////////
						if(message.getGroup()){
							
							MulticastMessage tmp = (MulticastMessage)message;

							MulticastMessage mm1 = this.mc.rMulticast(tmp, localName);
							checkRecRule(mm1);
							MulticastMessage mm = groupQueue.poll();
							
							if(mm != null){
								
								switch(mm.getKind()){
									case "request": 
													CSState state;
													boolean voted;
													synchronized(mp){
														state = mp.state;
														voted = mp.voted;
													}
													if (state == CSState.HELD || voted){
														System.out.println("CS is locked, request is queued");
														requestQueue.offer(mm);//add mm to tail
													}
													else{
														MulticastMessage sendm = new MulticastMessage(mm.getSender(),"vote","");
														sendm.setSrc(localName);
														sendm.setSender(localName);
														mp.send(sendm);
														synchronized(mp){
															mp.voted = true;
														}
													}
													break;
									case "release": 
													if(requestQueue.isEmpty()){
														mp.voted = false;
													}
													else{
														MulticastMessage last = requestQueue.poll();

														MulticastMessage sendm = new MulticastMessage(last.getSender(),"vote","");
														sendm.setSrc(localName);
														sendm.setSender(localName);
														mp.send(sendm);
														synchronized(mp){
															mp.voted = true;
														}
													}
													break;
									default: 		if(mm != null){//there is a new message
													causal_order_queue.add(mm);
													checkRecRule(causal_order_queue.poll());
													}
								}
							}
						}
						
						////////////////////////////////////
						else{
							if (message.getKind().equals("vote")){
									voteCounts++;
								if (voteCounts == mp.voteCount){
									synchronized(mp.state){
									mp.state = CSState.HELD;}
									voteCounts = 0;
									System.out.println("state is "+mp.state);
								}

							}
							else
								checkRecRule(message);
							
						}
						
							// update clock
						clock.update(message.getClock());
						}
				}
				}
				
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 *  Register a new channel to the selector
	 */
	protected void registerChannel( Selector selector, SelectableChannel channel, int ops) throws Exception{
		if (channel == null){
			return;
		}
		channel.configureBlocking(false);
		channel.register(selector, ops);
	}
	
	/*
	 * Read the message from the socket
	 */
	protected TimeStampedMessage[] readDataFromSocket(SelectionKey key) throws Exception{
		ArrayList<TimeStampedMessage> ret = new ArrayList<TimeStampedMessage>(); 
		SocketChannel socketChannel = (SocketChannel) key.channel();
		TimeStampedMessage message = readMessage(socketChannel);
		while (message != null) {
			ret.add(message);
			message = readMessage(socketChannel);
		}
        return ret.toArray(new TimeStampedMessage[]{});
	}
	
	
	private TimeStampedMessage readMessage(SocketChannel socketChannel) throws IOException, ClassNotFoundException {
		TimeStampedMessage ret = null;
		ByteBuffer lengthByteBuffer = ByteBuffer.wrap(new byte[4]);
		ByteBuffer dataByteBuffer = null;
		socketChannel.read(lengthByteBuffer);
		if (lengthByteBuffer.remaining() == 0) {
			int cap = lengthByteBuffer.getInt(0);
            dataByteBuffer = ByteBuffer.allocate(cap);
            lengthByteBuffer.clear();
        }
		if (dataByteBuffer == null) {
			return ret;
		}
		socketChannel.read(dataByteBuffer);
        if (dataByteBuffer.remaining() == 0) {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dataByteBuffer.array()));
            ret = (TimeStampedMessage) ois.readObject();
            // clean up
            dataByteBuffer = null; 
            ois.close();
        }
        System.out.println("received: "+ret);
        return ret;

	}
	
	String getConfigFile(){
		return this.configFile;
	}
	
	void checkRecRule(TimeStampedMessage message) throws Exception{
		this.conf = (YalmParser.parse(this.getConfigFile()));
		
		ArrayList<Rule> r = conf.getReceiveRules();
		
			for(Rule rule : r){
	
				if(rule.isMatch(message)){
					String action = rule.getAction();
	
					switch (action){
					case "drop":		
										return;
					case "duplicate":	
										TimeStampedMessage dup_m = message.copyof();
										System.out.println(dup_m.getClock().toString());
										message.setDuplicate(false);
										if(!message.getGroup())	receivedQueue.add(message);
										else	groupQueue.add((MulticastMessage) message);
										dup_m.setDuplicate(true);
										if(!message.getGroup())	receivedQueue.add(dup_m);
										else	groupQueue.add((MulticastMessage) message);
										return;
										
					case "delay":		
										if(message.getGroup())	delay_group_queue.add((MulticastMessage) message);
										else	delay_receive_queue.add(message);
										return;
					}
				}
			}
			
			if(!message.getGroup()){

				receiveCounts++;
				receivedQueue.add(message);
				if(!delay_receive_queue.isEmpty())	receivedQueue.add(delay_receive_queue.poll());
			}
			else{
				
				groupQueue.add((MulticastMessage) message);
				if(!delay_group_queue.isEmpty())	groupQueue.add(delay_group_queue.poll());
			}
	}

	
	
}

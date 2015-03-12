package core;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
	
	
	
	
	//Stack<MulticastMessage> multicast_Received = new Stack<MulticastMessage>();//use stack. return peek
	//HashSet<MulticastMessage> multicast_Received;
	int port;
	HashMap<String, SocketChannel> pool = null;
	Selector selector;
	ClockService clock;
	Configuration conf;
	String configFile;
	String localName;
	MessagePasser mp;
	multicast mc;
	public Receiver(multicast mc, MessagePasser mp, String localName,Selector selector,HashMap<String, SocketChannel> pool, 	ConcurrentLinkedQueue<TimeStampedMessage> receivedQueue, Configuration conf, String configFile, int port, ClockService clock){
		this.mc = mc;
		this.mp = mp;
		this.port = port;
		this.receivedQueue = receivedQueue;
		this.pool = pool;
		this.selector = selector;
		this.clock = clock;
		this.conf = conf;
		this.configFile = configFile;
		this.localName = localName;
		delay_receive_queue = new ConcurrentLinkedQueue<TimeStampedMessage>();
		//multicast_Received = new HashSet<MulticastMessage>();
		//multicast_Received = new Stack<MulticastMessage>();
		//multicast_Received.add(new MulticastMessage());
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
					
					if (key.isAcceptable()){
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						channel = server.accept();
						registerChannel(selector, channel, SelectionKey.OP_READ);
					}
					
					if (key.isReadable()){
						TimeStampedMessage message = readDataFromSocket(key);
						if (message == null){
							key.cancel();
							continue;
						}
						///////////send group message////////////////////////
						if(message.getGroup()){
							MulticastMessage mm = (MulticastMessage)message;
							MulticastMessage tmp = this.mc.rMulticast(mm, localName);
							if(tmp != null){//there is a new message
								causal_order_queue.add(tmp);
								checkRecRule(causal_order_queue.poll());
							}
							
						}
						
						////////////////////////////////////
						else{
							if (!pool.containsKey(message.getSrc())){
								pool.put(message.getSrc(), (SocketChannel) key.channel());
							}
						checkRecRule(message);
							
						}
							// update clock
						clock.update(message.getClock());
				}
				}
				it.remove();
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
	private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
	
	/*
	 * Read the message from the socket
	 */
	protected TimeStampedMessage readDataFromSocket(SelectionKey key) throws Exception{
		SocketChannel socketChannel = (SocketChannel) key.channel();
		TimeStampedMessage message = null;
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		int count;
		byte[] bytes = null;
		buffer.clear();
		while ((count = socketChannel.read(buffer))>0){
			buffer.flip();
			bytes = new byte[count];
			buffer.get(bytes);
			b.write(bytes);
			buffer.clear();
		}
		bytes = b.toByteArray();
		if (bytes.length>0){
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
	        ObjectInputStream ois = null;
	        ois = new ObjectInputStream(bais);
	        message = (TimeStampedMessage) ois.readObject();
	        ois.close();
	        b.close();
		}
		return message;
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
					case "duplicate":	TimeStampedMessage dup_m = message.copyof();
										System.out.println(dup_m.getClock().toString());
										message.setDuplicate(false);
										receivedQueue.add(message);
										dup_m.setDuplicate(true);
										receivedQueue.add(dup_m);
										return;
										
					case "delay":		
										delay_receive_queue.add(message);
										return;
					}
				}
			}
			receivedQueue.add(message);
			if(!delay_receive_queue.isEmpty())	receivedQueue.add(delay_receive_queue.poll());
	}

	
	
}

package core;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import clock.ClockService;
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
	int port;
	HashMap<String, SocketChannel> pool = null;
	Selector selector;
	ClockService clock;
	Configuration conf;
	String configFile;
	public Receiver(Selector selector,HashMap<String, SocketChannel> pool, 	ConcurrentLinkedQueue<TimeStampedMessage> receivedQueue, Configuration conf, String configFile, int port, ClockService clock){
		this.port = port;
		this.receivedQueue = receivedQueue;
		this.pool = pool;
		this.selector = selector;
		this.clock = clock;
		this.conf = conf;
		this.configFile = configFile;
		delay_receive_queue = new ConcurrentLinkedQueue<TimeStampedMessage>();
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
						if (!pool.containsKey(message.getSrc())){
							pool.put(message.getSrc(), (SocketChannel) key.channel());
						}
						checkRecRule(message);
						// update clock
						clock.update(message.getClock());
				//		message.setClock(clock);
						
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

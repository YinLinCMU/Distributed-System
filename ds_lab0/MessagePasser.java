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

public class MessagePasser{

	private String localName;
	private int port;
	private int seqNum = 0;
	
	private HashMap<String, SocketChannel> pool = new HashMap<String, SocketChannel>();
	private static Queue<Message> delay_send_queue = new LinkedList<Message>();
	private static Queue<Message> receive_queue = new LinkedList<Message>();
	private static Queue<Message> delay_receive_queue = new LinkedList<Message>();
	
	String configFile;
	private Configuration conf;
	
	Selector selector;
	
	public MessagePasser(String configuration_filename, String local_name){
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
		
	}

	void send(Message message) throws IOException{
		this.conf=(YalmParser.parse(this.getConfigFile()));
		ArrayList<Rule> r = conf.getSendRules();
		if(r == null)	return;
		if(message != null){
			for(Rule rule : r){
				if(rule.isMatch(message)){
					String action = rule.getAction();
					switch (action){
					case "drop":		break;
					case "duplicate":	message.setDuplicate(false);
										sending(message);
										Message dup_m = new Message(message);
										dup_m.setDuplicate(true);
										
										sending(dup_m);
										break;
					case "delay":		delay_send_queue.add(message);
										break;
					}
					return;
				}				
			}
			sending(message);
			while(!delay_send_queue.isEmpty()) //after one non-delay mess has been send
				sending(delay_send_queue.poll());
		}
	}

	void receive() throws Exception{
		
		this.conf = (YalmParser.parse(this.getConfigFile()));
		Message message = null;
		ArrayList<Rule> r = conf.getReceiveRules();
		if(receive_queue.isEmpty())	{System.out.println("There is no message.");}
		else{
			message = receive_queue.peek();
			Boolean matchrule = false;
			for(Rule rule : r){
				if(rule.isMatch(message)){
					String action = rule.getAction();

					switch (action){
					case "drop":		matchrule = true;
										receive_queue.poll();
										break;
					case "duplicate":	matchrule = true;
										message.setDuplicate(false);
										Message dup_m = new Message(message);
										dup_m.setDuplicate(true);
										receiving();
										System.out.println("receive " + dup_m);
										break;
					case "delay":		matchrule = true;
										delay_receive_queue.add(message);
										while(receive_queue.isEmpty())
										{ receive_queue.add(delay_receive_queue.poll());}
										break;
					}
				}
			}
			if(matchrule == false){
				receiving();
			}
		}
	}
	
	void receiving(){
		System.out.println("receive " + receive_queue.poll());
	}

	void sending(Message message) throws IOException {
		ObjectOutputStream oos = null;
		if(message == null)	return;
		else{
			String dest = message.getDest();
			String ip = null;
			int port = -1;

			Node node = conf.getNode(dest);
			ip = node.getIp();
			port = node.getPort();
			
			SocketChannel socketChannel = null;
			
			if (pool.containsKey(node.getName())){
				socketChannel = pool.get(node.getName());
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

			byte[] messbyte = null;//convert to byte
			ByteArrayOutputStream bytearr = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bytearr);
			oos.writeObject(message);
			messbyte = bytearr.toByteArray();
			ByteBuffer buffer = ByteBuffer.wrap(messbyte);
			socketChannel.write(buffer);
			System.out.println("Message sent");
		}
	}
	
	/*
	 *  Getters and setters
	 */
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
	
	// start receiving thread 
	public void listen(){
		Thread thread = new Thread(new Receiver( selector,pool, receive_queue, port));
		thread.start();
	}
	
	public void run() throws IOException{
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader buffedReader = new BufferedReader(input);
		
		// infinite loop to perform the interaction
		while (true){
			System.out.println("Hello, This is " + localName);
			System.out.println("Please chose \"send\", \"receive\" or \"exit\"");
			String line = buffedReader.readLine().trim();
			if (line.equalsIgnoreCase("send")){
				System.out.println("Enter the destination name:\n");
				
				String dest = buffedReader.readLine().trim();
				if (!conf.hasNode(dest)){
					System.err.println("The Node is not exist!");
					continue;
				}
				System.out.println("Enter the kind of message:\n");
				String kind = buffedReader.readLine().trim();
				System.out.println("Enter the content of the message:\n");
				String data = buffedReader.readLine();
				Message message = new Message(dest, kind, data);
				message.setSource(localName);
				message.setSeqNum(seqNum++);
				send(message);
			}
			else if (line.equalsIgnoreCase("receive")){
				try {
					receive();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (line.equalsIgnoreCase("exit")){
				for (Map.Entry<String, SocketChannel> e : pool.entrySet()){
					e.getValue().close();
				}
				System.exit(0);
				break;
			}
			else{
				System.err.println("invalid input, Please enter again");
			}
		}
	}
	
	// Main func
	public static void main(String[] args) throws IOException{
		MessagePasser mp = new MessagePasser(args[0],args[1]);
		mp.listen();
		mp.run();
		
	}
}

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;

/*
 * The thread checking receiving messages using NIO 
 * selector is used to monitoring all the live sockets.
 */
public class Receiver implements Runnable{
	
	Queue<Message> receivedQueue;
	int port;
	HashMap<String, SocketChannel> pool = null;
	Selector selector;
	public Receiver(Selector selector,HashMap<String, SocketChannel> pool, Queue<Message> receivedQueue, int port){
		this.port = port;
		this.receivedQueue = receivedQueue;
		this.pool = pool;
		this.selector = selector;
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
						Message message = readDataFromSocket(key);
						if (message == null){
							key.cancel();
							continue;
						}
						if (!pool.containsKey(message.getSource())){
							pool.put(message.getSource(), (SocketChannel) key.channel());
						}
						
						receivedQueue.add(message);
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
	protected Message readDataFromSocket(SelectionKey key) throws Exception{
		SocketChannel socketChannel = (SocketChannel) key.channel();
		Message message = null;
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
	        message = (Message) ois.readObject();
	        ois.close();
	        b.close();
		}
		return message;
	}
}
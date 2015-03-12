package core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import clock.ClockService;
import util.Configuration;
import util.MulticastMessage;

public class multicast {

	HashSet<MulticastMessage> multicast_Received;
	MessagePasser mp;

	public multicast(MessagePasser mp){
		this.mp = mp;
		
		this.multicast_Received = new HashSet<MulticastMessage>();
	}
	
	public MessagePasser getMp(){
		return this.mp;
	}
	
	public Configuration getConf(){
		return this.getMp().getConf();
	}
	
	public HashSet<MulticastMessage> getMultiRec(){
		return this.multicast_Received;
	}
	
	public void setMultiRec(HashSet<MulticastMessage> mr){
		this.multicast_Received = mr;
	}
	public String getConFile(){
		return this.getMp().configFile;
	}
	
	public String getLocalName(){
		return this.getMp().getLocalName();
	}
	
	public ClockService getClock(){
		return this.getMp().getClock();
	}
	
	public void bMulticast(MulticastMessage message, String groupName, MessagePasser mp) throws IOException{
		message.setGroup(true);;
		ArrayList<String> pGroup = this.getConf().getGroup(groupName).getMembers();
		//System.out.println(pGroup);
		String senderName = message.getSender();
		for (String name : pGroup){
			MulticastMessage m = new MulticastMessage(message);
			m.setSender(senderName);
			m.setGroupName(groupName);
			m.setDest(name);
			mp.send(m);
		}
	}
	
	public MulticastMessage rMulticast(MulticastMessage message, String localName) throws IOException{
		MulticastMessage mm = (MulticastMessage)message;
		
		if (!multicast_Received.contains(mm)){//for sender
			multicast_Received.add(mm);
			return mm;
		}
		if(!multicast_Received.contains(mm) && !mm.getSender().equals(localName)){//for receivers
			multicast_Received.add(mm);
			mm.setSrc(localName);
			this.bMulticast(mm, mm.getGroupName(), this.getMp());
			return mm;
		}
		//if no need to forward or there is no new message, then return null
		return null;
	}
}

package util;
import java.util.ArrayList;

/*
 * The class to hold info from config.yaml
 */
public class Configuration{
	private ArrayList<Rule> sendRules;
	private ArrayList<Rule> receiveRules;
	private ArrayList<Node> configuration;
	private ArrayList<Group> groups;
	
	public Configuration(){
		this.setSendRules(new ArrayList<Rule>());
		this.setReceiveRules(new ArrayList<Rule>());
		this.setConfiguration(new ArrayList<Node>());
	}
	
	public Group getGroup(String groupName){
		for(Group g : this.groups){
			if(g.getName().equals(groupName)){
				return g;
			}
		}
		return null;
	}
	
	public ArrayList<Rule> getSendRules() {
		return sendRules;
	}

	public void setSendRules(ArrayList<Rule> sendRules) {
		this.sendRules = sendRules;
	}

	public ArrayList<Rule> getReceiveRules() {
		return receiveRules;
	}

	public void setReceiveRules(ArrayList<Rule> receiveRules) {
		this.receiveRules = receiveRules;
	}

	public ArrayList<Node> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(ArrayList<Node> configuration) {
		this.configuration = configuration;
	}
	
	public boolean hasNode(String name){
		Node node = getNode(name);
		if (node == null){
			return false;
		}
		return true;
	}

	public Node getNode(String name){
		for (Node node : configuration){
			if(node.getName() != null && node.getName().equalsIgnoreCase(name) ){
				return node;
			}
		}
		return null;
	}
	
	public boolean hasGroup(String name){
		Group group = getGroup(name);
		if (group == null){
			return false;
		}
		return true;
	}

	

	public void setGroups(ArrayList<Group> groups) {
		this.groups = groups;
	}
}
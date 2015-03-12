package util;

import java.util.ArrayList;

public class Node{
	private String name;
	private String ip;
	private Integer port;
	private ArrayList<String> memberOf;

	public Node(String name, String ip, Integer port){
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.setMemberOf(new ArrayList<String>());
	}
	public Node(){
		this.name = null;
		this.ip = null;
		this.port = -1;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public ArrayList<String> getMemberOf() {
		return memberOf;
	}
	public void setMemberOf(ArrayList<String> memberOf) {
		this.memberOf = memberOf;
	}
}
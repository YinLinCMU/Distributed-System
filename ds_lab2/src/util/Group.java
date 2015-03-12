package util;

import java.util.ArrayList;

public class Group {
	private String name;
	private ArrayList<String> members;

	public Group(){
		this.name = null;
		this.members = null;
	}
	public Group(String name, ArrayList<String> members){
		this.name = name;
		this.members = members;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ArrayList<String> getMembers() {
		return members;
	}
	public void setMembers(ArrayList<String> members) {
		this.members = members;
	}

}

package com.shunote.Entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Node
 * @author Jeffrey
 * @since 2012-3-31
 */
public class Node implements Serializable {
	
	private static final long serialVersionUID = 5331841769033381757L;
	private int id;
	private String title;
	private String content;
	private Image img;
	private Node father;
	private List<Node> sons;
	
	public Node(){
		super();
	}
	
	public Node(int id, String title, String content, Image img, Node father) {
		super();
		this.id = id;
		this.title = title;
		this.content = content;
		this.img = img;
		this.father = father;
		this.sons = new ArrayList<Node>();
	}
	
	public int getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public String getContent() {
		return content;
	}
	public Image getImg() {
		return img;
	}
	public Node getFather() {
		return father;
	}
	public List<Node> getSons() {
		return sons;
	}
	public void addSons(Node node){
		sons.add(node);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setImg(Image img) {
		this.img = img;
	}

	public void setFather(Node father) {
		this.father = father;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	
}

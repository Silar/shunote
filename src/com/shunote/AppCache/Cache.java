package com.shunote.AppCache;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.shunote.Entity.Node;
import com.shunote.Entity.Note;
import com.shunote.Entity.Transform;
import com.shunote.Exception.CacheException;
import com.shunote.HTTP.MyCookieStore;
import com.shunote.HTTP.WebClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Cache - opt data with Server & DB
 * @author Jeffrey
 *
 */
public class Cache{
	
	private static Cache instance = null;
	
	private DBHelper dbHelper = null;
	
	private CookieStore cookieStore = null;
	
	private int userid = 0; 
	
	private Cache(){};
	
	private final String tag = "Cache";
	
	private SharedPreferences sp = null;
	
	String USERID,JSESSIONID, SESSIONID, USERNAME, PWD,HOST; // SP
	
	private String SPTAG = "";
	
	private WebClient webClient = null;
	/**
	 * Singleton
	 * @return instance
	 */
	public static Cache getInstance(){
		if (instance == null) {
			instance = new Cache();
			return instance;
		}else{
			return instance;
		}
	}
	
	/**
	 * init DBHelper\COOKIESTORE\WEBCLient\USERID
	 * @param localcookieStore
	 * @param 
	 * @throws CacheException 
	 */
	public void init(Context con) throws CacheException{
		try{
			if(this.dbHelper==null){
				
				Configuration config = new Configuration(con);
				SPTAG = config.getValue("SPTAG");
				HOST = config.getValue("host");
				sp = con.getSharedPreferences(SPTAG,Context.MODE_WORLD_READABLE);
				
				// fetch data from SP
				USERID = sp.getString("userid", null);
				JSESSIONID = sp.getString("JSESSIONID", null);
				SESSIONID = sp.getString("sessionid", null);
				userid = Integer.parseInt(USERID);
				MyCookieStore myc = new MyCookieStore(JSESSIONID,SESSIONID,HOST);
				cookieStore = myc.getCookieStore();	
				
				Log.d("Cache.initDB","DB inited");
				dbHelper = new DBHelper(con);
				
				webClient = WebClient.getInstance();
				
				webClient.init(con);
			}
		}catch(NullPointerException e){
			throw new CacheException("init failed!",e);			
		}		
	}
	
	
	/**
	 *  get Note
	 * @param userid
	 * @param noteid
	 * @return Note , else null
	 * @throws CacheException 
	 */
	public Note getNote(int noteid) throws CacheException{
		Note note = null;	
		try {		
			String url = "/users/" + userid + "/usernodes/" + noteid;
			String result = webClient.GetData(url, cookieStore);
			Log.i("Cache.getNote()","result=" +result);		
		
			JSONObject objr = new JSONObject(result);
			if (Integer.parseInt(objr.getString("result"))<0){
					throw new CacheException("unable to get Note from server!");
			}else{
				JSONObject obj = objr.getJSONObject("data");
				Log.d("Cache.getNote()","get Note from server!");
				note = new Note(obj.getInt("id"),obj.getString("title"),obj.getInt("root") ,obj.getString("nodes"));
				Note dbNote = dbHelper.getNote(noteid);
				if (dbNote==null){
					Log.d("Cache.getNote()","insert new note into DB");
					dbHelper.insertNote(note);
				}else{
					Log.d("Cache.getNote()","already has a note in DB");
					if (dbNote.equals(note)==false){
						Log.d("Cache.getNote()","update note in DB");
						dbHelper.updateNote(note);
					}					
				}
			}
			
		} catch (JSONException e) {
			Log.e("Cache.getNote()","JSON wrong in getNote()");
			e.printStackTrace();
		} catch (NumberFormatException e){
			e.printStackTrace();
			throw new CacheException("wrong input args!",e);
		}
		return note;
	}
	
	/**
	 * add Note
	 * @param note
	 * @param url
	 * @return noteid , else -1
	 */
	public int addNote(String title){
		String url = "/users/" + userid + "/nodes";
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("title",title));
		pairs.add(new BasicNameValuePair("parentid","-1"));
		pairs.add(new BasicNameValuePair("pub", "0"));
		String result = webClient.PostData(url, cookieStore, pairs);
		try {
			JSONObject objr = new JSONObject(result);
				if (Integer.parseInt(objr.getString("result"))<0){
					Log.d("Cache.addNote()","unable to upload new Note to server!");
					return -1;
				}else{
					JSONObject obj = objr.getJSONObject("data");
					Log.d("Cache.addNote()","upload a new Note to server!");
					Note note = new Note(obj.getInt("id"),obj.getString("title"),obj.getInt("root") ,obj.getString("nodes"));
					dbHelper.insertNote(note);
					return note.getId();
			}
			
		} catch (JSONException e) {
			Log.e("Cache.addNote()","JSON wrong in addNote()");
			e.printStackTrace();
		}
		
		return -1;
		
	}
	
	/**
	 * update Note
	 * @param note
	 * @param url
	 * @return noteid, else -1
	 */
	public int updateNote(Note note) {
		String url = "/users/" + userid + "/usernodes/" + note.getId();
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("title", note.getName()));
		pairs.add(new BasicNameValuePair("parentid","-1"));
		pairs.add(new BasicNameValuePair("pub", "0"));
		String result = webClient.PostData(url, cookieStore, pairs);
		Log.i("Cache.updateNote()","result=" + result);
		try {
			JSONObject obj = new JSONObject(result);

			if (Integer.parseInt(obj.getString("result"))<0){
					Log.d("Cache.updateNote()","unable to update Note to server!");
					return -1;
			}else{
				Log.d("Cache.updateNote()","update a Note to server! id = " + note.getId());
				Note newnote = new Note(note.getId(),note.getName(),note.getRoot(),note.getJson());
				dbHelper.updateNote(newnote);
				return newnote.getId();
			}
			
		} catch (JSONException e) {
			Log.e("Cache.updateNote()","JSON wrong in addNote()");
			e.printStackTrace();
		}
		return -1;

	}
	
	/**
	 * del Note
	 * @param noteid
	 * @return noteid, else -1
	 */
	public int delNote(int noteid){
		String url = "/users/"+userid+"/usernodes/" + noteid;
		
		String result = webClient.DelData(url, cookieStore);
		
		Log.i("Cache.delNote()","result="+result);
		try {
			JSONObject obj = new JSONObject(result);
			if(obj.has("result")){
				if (Integer.parseInt(obj.getString("result"))<0){
					Log.d("Cache.delNote()","unable to delete Note in server!");
					return -1;
				}else{
					Log.d("Cache.delNote()","delete note id=" + noteid + " success!");
					dbHelper.delNote(noteid);
					return noteid;
				}
			}
			else{
				Log.d("Cache.delNote()","unable to delete Note in server!");
				return -1;
			}
		} catch (JSONException e) {
			Log.e("Cache.delNote()","JSON wrong in delNote()");
			e.printStackTrace();
		}
		
		return -1;
		
	}
	
	/**
	 * add Node
	 * @param userid
	 * @param node
	 * @return father node
	 * @throws CacheException 
	 */
	public Node addNode(Node node,int noteid) throws CacheException{
		
		String url = "/users/" + userid + "/nodes";
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("title", node.getTitle()));
		pairs.add(new BasicNameValuePair("parentid", Integer.toString(node.getFather().getId())));
		pairs.add(new BasicNameValuePair("content", node.getContent()));
		
		//push changes to the server
		String result = webClient.PostData(url, cookieStore, pairs);
		
		Log.i(tag,result);
		try{
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("result")<0){
				throw new CacheException("add node to server failed");
			}else{
				node.setId(obj.getJSONObject("data").getInt("id"));
			}
		}catch(JSONException e){
			e.printStackTrace();
		}
		
		try{
			//获得父结点并将新结点加入到队尾
			Node father = node.getFather();
			father.addSons(node);
			
			//更新笔记信息
			if(Cache.getInstance().rebuildNodes(noteid, father)>0){
				return father;
			}
		}	
		catch(NullPointerException e){
			throw new CacheException("get Note faild!",e);
		}
		
		
		return null;
	}
	
	/**
	 * update Node
	 * @param userid
	 * @param node
	 * @return father node
	 * @throws CacheException 
	 */
	public Node updateNode(Node node,int noteid) throws CacheException{
		String url = "/users/" + userid + "/nodes/" + node.getId();
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("title", node.getTitle()));
		pairs.add(new BasicNameValuePair("content", node.getContent()));
		
		String result = webClient.PostData(url, cookieStore, pairs);
		try{
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("result")<0){
				throw new CacheException("update node to server failed");
			}			
		}catch(JSONException e){
			e.printStackTrace();
		}
		
		try{
			//更新笔记信息
			if(Cache.getInstance().rebuildNodes(noteid, node)>0){
				return node.getFather();
			}
		}	
		catch(NullPointerException e){
			throw new CacheException("get Note faild!",e);
		}
		
		return null;
		
	}
	
	/**
	 * del Node
	 * @param userid
	 * @param node
	 * @param noteid
	 * @return father node
	 * @throws CacheException 
	 */
	public Node delNode(Node node,int noteid) throws CacheException{
		String url = "/users/" + userid + "/nodes/" + node.getId();
		
		String result = webClient.DelData(url, cookieStore);
		try{
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("result")<0){
				throw new CacheException("del node to server failed");
			}			
		}catch(JSONException e){
			e.printStackTrace();
		}
		
		try{
			Node father = node.getFather();
			father.getSons().remove(node); //delete the node
			//更新笔记信息
			if(Cache.getInstance().rebuildNodes(noteid, father)>0){
				return father;
			}
		}	
		catch(NullPointerException e){
			throw new CacheException("get Note faild!",e);
		}
		
		return null;
	}
	
	/**
	 * change node to the target position,if target = -1 then changed it to its father's layer
	 * @param userid
	 * @param target
	 * @param node
	 * @param noteid
	 * @return father node
	 * @throws CacheException 
	 */ 
	public Node changePosition(int target,Node node,int noteid) throws CacheException{
		
		//save the old node to new node and delete the old one;
		Node newnode = new Node();
		newnode = node;
		int brotherid = -1;
		Node father = node.getFather();
		father.getSons().remove(node);
		
		if(target == -1){			
			Node newfather = father.getFather();
			int loc = newfather.getSons().indexOf(father)+1;
			newfather.getSons().add(loc, newnode);
			brotherid = newfather.getSons().get(loc-1).getId();
			father = newfather;
		}else{
			father.getSons().add(target, newnode);
			brotherid = target -1;
		}
		
		String url = "/users/" + userid + "/nodes/" + node.getId() + "/relationship";
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("parentid", Integer.toString(father.getId())));
		pairs.add(new BasicNameValuePair("brotherid", Integer.toString(brotherid)));
		
		String result = webClient.PostData(url, cookieStore, pairs);
		
		Log.i(tag,result);
		try{
			JSONObject obj = new JSONObject(result);
			if(obj.getInt("result")<0){
				throw new CacheException("update node to server failed");
			}			
		}catch(JSONException e){
			e.printStackTrace();
		}
		
		try{
			//更新笔记信息
			if(Cache.getInstance().rebuildNodes(noteid, father)>0){
				return father;
			}
		}	
		catch(NullPointerException e){
			throw new CacheException("get Note faild!",e);
		}
		
		
		
		return null;
	}
	
	/**
	 * reverse to the root node and update data in DB
	 * @param noteid
	 * @param father
	 * @return 1,else -1
	 * @throws CacheException
	 */
	public int rebuildNodes(int noteid,Node father) throws CacheException{
		try{
			
			//回溯到根结点
			while(father.getFather().getId()>0){
				father = father.getFather();
			}
			
			//生成新JSON串
			JSONObject newJSON = Transform.getInstance().node2Json(father);
			String JSONs = newJSON.toString();
			
			Log.i(tag,"JSON:"+JSONs);
			
			Note note = dbHelper.getNote(noteid);
			note.setJson(JSONs);
			dbHelper.updateNote(note);
			return 1;
		}catch(JSONException e){
			throw new CacheException("parse JSON failed!",e);
		}
		
	}
	
}

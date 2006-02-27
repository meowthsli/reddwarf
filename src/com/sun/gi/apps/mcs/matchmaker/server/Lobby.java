package com.sun.gi.apps.mcs.matchmaker.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.gi.comm.routing.ChannelID;
import com.sun.gi.comm.routing.UserID;
import com.sun.gi.logic.GLO;
import com.sun.gi.logic.GLOReference;
import com.sun.gi.logic.SimTask;
import com.sun.gi.utils.SGSUUID;

/**
 * 
 * <p>Title: Lobby</p>
 * 
 * <p>Description: Represents a Lobby in the Match Making application.  Lobbies function
 * both as a starting point for creating games and as a chat area.</p>
 * 
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: Sun Microsystems, TMI</p>
 * 
 * @author	Sten Anderson
 * @version 1.0
 */
public class Lobby extends ChannelRoom {

	private int maxPlayers;
	private boolean canHostBoot;
	private boolean canHostBan;
	private boolean canHostChangeSettings;
	private int maxConnectionTime;			// in minutes?
	private HashMap<String, Object> gameParameters;
	private List<GLOReference> gameRoomList;
	
	public Lobby(String name, String description, String password, String channelName, ChannelID cid) {
		super(name, description, password, channelName, cid);
		
		gameParameters = new HashMap<String, Object>();
		gameRoomList = new LinkedList<GLOReference>();
	}
	
	public void setMaxPlayers(int num) {
		maxPlayers = num;
	}
	
	public void setCanHostBoot(boolean b) {
		canHostBoot = b;
	}
	
	public void setCanHostBan(boolean b) {
		canHostBan = b;
	}
	
	public void setCanHostChangeSettings(boolean b) {
		canHostChangeSettings = b;
	}
	
	public void setMaxConnectionTime(int time) {
		maxConnectionTime = time;
	}
	
	public int getMaxPlayers() {
		return maxPlayers;
	}
	
	public boolean getCanHostBoot() {
		return canHostBoot;
	}
	
	public boolean getCanHostBan() {
		return canHostBan;
	}
	
	public boolean getCanHostChangeSettings() {
		return canHostChangeSettings;
	}
	
	public int getMaxConnectionTime() {
		return maxConnectionTime;
	}
	
	public void addGameRoom(GLOReference grRef) {
		gameRoomList.add(grRef);
	}
	
	public SGSUUID getLobbyID() {
		return getChannelID();
	}
	
	/**
	 * Returns a read-only view of the game parameters map.
	 * 
	 * @return	a read-only view of the game parameters map.
	 */
	public Map<String, Object> getGameParamters() {
		return Collections.unmodifiableMap(gameParameters);
	}
}
/*
 * Copyright (c) 2024.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of DcissChatService.
 *
 * DcissChatService is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * DcissChatService is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.miashs.dciss.chatservice.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import fr.uga.miashs.dciss.chatservice.common.Packet;

import java.util.*;

public class ServerMsg {
	
	private final static Logger LOG = Logger.getLogger(ServerMsg.class.getName());
	public final static int SERVER_CLIENTID = 0;

	private transient ServerSocket serverSock;
	private transient boolean started;
	private transient ExecutorService executor;
	private transient ServerPacketProcessor sp;
	
	// maps pour associer les id aux users et groupes
	private Map<Integer, UserMsg> users;
	private Map<Integer, GroupMsg> groups;


	
	
	
	// séquences pour générer les identifiant d'utilisateurs et de groupe
	private AtomicInteger nextUserId;
	private AtomicInteger nextGroupId;

	public ServerMsg(int port) throws IOException {
		serverSock = new ServerSocket(port);
		started = false;
		users = new ConcurrentHashMap<>();
		groups = new ConcurrentHashMap<>(); 
		nextUserId = new AtomicInteger(1);
		nextGroupId = new AtomicInteger(-1);
		sp = new ServerPacketProcessor(this);
		executor = Executors.newCachedThreadPool();
	}

	public GroupMsg createGroup(int ownerId) {
		UserMsg owner = users.get(ownerId);
		if (owner==null) throw new ServerException("User with id="+ownerId+" unknown. Group creation failed.");
		int id = nextGroupId.getAndDecrement();
		GroupMsg res = new GroupMsg(id,owner);
		groups.put(id, res);
		LOG.info("Group "+res.getId()+" created");
		return res;
	}
	
	public boolean removeGroup(int groupId) {
		//TODO : implementation de la gestion des droits?
		GroupMsg g =groups.remove(groupId);
		if (g==null) return false;
		g.beforeDelete();
		LOG.info("Le groupe " + groupId + " a été supprimé");
		return true;
	}
	
	public boolean removeUser(int userId) {
		UserMsg u =users.remove(userId);
		if (u==null) return false;
		u.beforeDelete();
		return true;
	}
	
	public UserMsg getUser(int userId) {
		return users.get(userId);
	}

	/**
	 * Get the username of a user by its id
	 * @param userId
	 * @return username
	 */
	public String getUsernameByUserId(int userId) {
		UserMsg user = users.get(userId);
		if (user != null) {
			return user.getUsername();
		}
		return null; // or throw an exception
	}

	/**
	 * get all users, with their id, username and password
	 * @return
	 */
	public String getUsers() {
		String res = "";
		for (UserMsg u : users.values()) {
			res += u.getId() + ", " + u.getUsername() + ", " + u.getPassword() + "\n";
		}
		return res;
	}

	public GroupMsg getGroup(int groupId) {
		return groups.get(groupId);
	}

	public int authenticateUser(String username, String password) {
		//get user associated with this username
		UserMsg user = users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
		String realPassword = user.getPassword(); //get user's password
		if (user != null && realPassword.equals(password)) {//compare to given password
			return user.getId();
		}
		return 0;
	}

	
	// Methode utilisée pour savoir quoi faire d'un paquet
	// reçu par le serveur
	public void processPacket(Packet p) throws IOException {
		PacketProcessor pp = null;
		if (p.destId < 0) { //message de groupe
			// can be send only if sender is member
			UserMsg sender = users.get(p.srcId);
			GroupMsg g = groups.get(p.destId);
			//TODO : il faut informer l'expéditeur si le destinataire n'existe pas
			//if (g == null) {
			//	LOG.info("ce destinataire n'existe pas");
			//}
			if (g.getMembers().contains(sender)) pp=g;
		}
		else if (p.destId > 0) { // message entre utilisateurs
			 pp = users.get(p.destId);
		}
		else { // message de gestion pour le serveur
			pp=sp;
		}
		
		if (pp != null) {
			pp.process(p);
		}
	}

	public void start() {
		started = true;
		while (started) {
			try {
				// le serveur attend une connexion d'un client
				Socket s = serverSock.accept();

				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());

				// lit l'identifiant du client
				int userId = dis.readInt();
				String password = dis.readUTF();
				LOG.info("Connection request from " + userId + " with password " + password);
				//si 0 alors il faut créer un nouvel utilisateur et
				// envoyer l'identifiant au client
				if (userId == 0) {
					userId = nextUserId.getAndIncrement();
					dos.writeInt(userId);
					dos.flush();
					//j'ajoute un username par défaut, du type user3. le constructeur de UserMsg prend mtn une string en paramètre.
					users.put(userId, new UserMsg(userId, this, "user"+userId, password));
				}
				// si l'identifiant existe ou est nouveau alors 
				// deux "taches"/boucles  sont lancées en parralèle
				// une pour recevoir les messages du client, 
				// une pour envoyer des messages au client
				// les deux boucles sont gérées au niveau de la classe UserMsg
				UserMsg x = users.get(userId);
				if (x!= null && x.open(s, x.getUsername()) && x.getPassword().equals(password)) {
					LOG.info(userId + " connected");
					// lancement boucle de reception
					executor.submit(() -> x.receiveLoop());
					// lancement boucle d'envoi
					executor.submit(() -> x.sendLoop());
				} else { // si l'idenfiant est inconnu, on ferme la connexion
					s.close();
				}

			} catch (IOException e) {
				LOG.info("Close server");
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		started = false;
		try {
			serverSock.close();
			users.values().forEach(s -> s.close());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		ServerMsg s = new ServerMsg(1666);
		s.start();
	}


}

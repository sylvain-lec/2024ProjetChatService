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
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import fr.uga.miashs.dciss.chatservice.common.Packet;
import java.util.*;

public class UserMsg implements PacketProcessor{
	private final static Logger LOG = Logger.getLogger(UserMsg.class.getName());

	private int userId;
	private String username;
	private String password;

	private List<String> contacts;
	private Set<GroupMsg> groups;

	private ServerMsg server;
	private transient Socket s;
	private transient boolean active;

	private BlockingQueue<Packet> sendQueue;

	/**
	 *
	 * @param clientId
	 * @param server
	 * @param username : default username given by the server (in ServerMsg) is "user"+clientId
	 */
	public UserMsg(int clientId, ServerMsg server, String username, String password) {
		if (clientId<1) throw new IllegalArgumentException("id must not be less than 0");
		this.server=server;
		this.userId=clientId;
		active=false;
		sendQueue = new LinkedBlockingQueue<>();
		groups = Collections.synchronizedSet(new HashSet<>());
		contacts = new ArrayList<>();
		this.username = username;
		this.password = password;
	}

	public int getId() {
		return userId;
	}
	public String getUsername() { return username; }

	public void setUsername(String username) { this.username = username; }

	public String getPassword() { return password; }

//	public boolean checkPassword(String password) { return this.password.equals(password); }


	public boolean removeGroup(GroupMsg g) {
		if (groups.remove(g)) {
			g.removeMember(this);
			return true;
		}
		return false;
	}

	// to be used carrefully, do not add groups directly
	protected Set<GroupMsg> getGroups() {
		return groups;
	}

	/*
	 * This method has to be called before removing a group in order to clean membership.
	 */
	public void beforeDelete() {
		groups.forEach(g->g.getMembers().remove(this));
		
	}
	
	
	/*
	 * METHODS FOR MANAING THE CONNECTION
	 */
	public boolean open(Socket s, String username) {
		if (active) return false;
		this.s=s;
		this.username = username;
		active=true;
		return true;
	}
	
	public void close() {
		active=false;
		try {
			if (s!=null) s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		s=null;
		LOG.info(userId + " deconnected");
	}

public boolean isConnected() {
		return s!=null;
	}

	// boucle de réception
	public void receiveLoop() {
		try {
			DataInputStream dis = new DataInputStream(s.getInputStream());
			// tant que la connexion n'est pas terminée
			while (active && ! s.isInputShutdown()) {
				// on lit les paquets envoyé par le client
				int destId = dis.readInt();
				int length = dis.readInt();
				byte[] content = new byte[length];
				dis.readFully(content);
				// on envoie le paquet à ServerMsg pour qu'il le gère
				server.processPacket(new Packet(userId,destId,content));
			}
			
		} catch (IOException e) {
			// problem in reading, probably end connection
			LOG.warning("Connection with client "+userId+" is broken...close it.");
		}
		close();
	}
	
	// boucle d'envoi
	public void sendLoop() {
		Packet p = null;
		try {
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			// tant que la connexion n'est pas terminée
			while (active && s.isConnected()) {
				// on récupère un message à envoyer dans la file
				// sinon on attend, car la méthode take est "bloquante" tant que la file est vide
				p = sendQueue.take();
				// on envoie le paquet au client
				dos.writeInt(p.srcId);
				dos.writeInt(p.destId);
				dos.writeInt(p.data.length);
				dos.write(p.data);
				dos.flush();
				
			}
		} catch (IOException e) {
			// remet le paquet dans la file si pb de transmission (connexion terminée)
			if (p!=null) sendQueue.offer(p);
			LOG.warning("Connection with client "+userId+" is broken...close it.");
			//e.printStackTrace();
		} catch (InterruptedException e) {
			throw new ServerException("Sending loop thread of "+userId+" has been interrupted.",e);
		}
		//close();
	}
	
	/**
	 * Method for adding a packet to the sending queue
	 */
	// cette méthode est généralement appelée par ServerMsg
	public void process(Packet p) {
		sendQueue.offer(p);
	}

	public void setPassword(String password) {
		this.password = password;
	}

	;
	void addContact(String contactName) {
		contacts.add(contactName);
		LOG.info("Contact added successfully for user with ID: " + userId + " contact Name: " + contactName);
	}


	public List<String> getContacts() {

			return contacts;
		}

	public void sendPacket(byte[] array) {
		if (s != null && s.isConnected()) {
			try {
				// Construire le tableau de bytes avec les contacts
				String contactListStr = Arrays.toString(contacts.toArray());
				byte[] contactListBytes = contactListStr.getBytes();

				//ByteArrayOutputStream contactBytesStream = new ByteArrayOutputStream();

                // Concaténer le tableau de bytes des contacts avec le tableau de bytes du paquet
				byte[] combinedArray = new byte[array.length + contactListBytes.length];
				System.arraycopy(array, 0, combinedArray, 0, array.length);
				System.arraycopy(contactListBytes, 0, combinedArray, array.length, contactListBytes.length);

				// Envoi du paquet avec la liste des contacts
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				dos.writeInt(userId); // Envoi l'identifiant de l'utilisateur
				dos.writeInt(0); // Envoi l'identifiant du serveur
				dos.writeInt(combinedArray.length); // Envoi la taille des données
				dos.write(combinedArray); // Envoi les données
				dos.flush(); // Vide le tampon de sortie
				LOG.info("Packet sent to user " + userId);
			} catch (IOException e) {
				LOG.warning("Failed to send packet to user " + userId + ": " + e.getMessage());
			}
		} else {
			LOG.warning("User with ID " + userId + " is not connected. Unable to send packet.");
		}

	}


}


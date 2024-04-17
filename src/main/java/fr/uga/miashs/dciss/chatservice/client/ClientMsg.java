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

package fr.uga.miashs.dciss.chatservice.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.server.ServerMsg;

/**
 * Manages the connection to a ServerMsg. Method startSession() is used to
 * establish the connection. Then messages can be send by a call to sendPacket.
 * The reception is done asynchronously (internally by the method receiveLoop())
 * and the reception of a message is notified to MessagesListeners. To register
 * a MessageListener, the method addMessageListener has to be called. Session
 * are closed thanks to the method closeSession().
 */
public class ClientMsg {

	private String serverAddress;
	private int serverPort;

	private Socket s;
	private DataOutputStream dos;
	private DataInputStream dis;

	private int identifier;

	private List<MessageListener> mListeners;
	private List<ConnectionListener> cListeners;

	/**
	 * Create a client with an existing id, that will connect to the server at the
	 * given address and port
	 * 
	 * @param id      The client id
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(int id, String address, int port) {
		if (id < 0)
			throw new IllegalArgumentException("id must not be less than 0");
		if (port <= 0)
			throw new IllegalArgumentException("Server port must be greater than 0");
		serverAddress = address;
		serverPort = port;
		identifier = id;
		mListeners = new ArrayList<>();
		cListeners = new ArrayList<>();
	}

	/**
	 * Create a client without id, the server will provide an id during the the
	 * session start
	 * 
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(String address, int port) {
		this(0, address, port);
	}

	/**
	 * Register a MessageListener to the client. It will be notified each time a
	 * message is received.
	 * 
	 * @param l
	 */
	public void addMessageListener(MessageListener l) {
		if (l != null)
			mListeners.add(l);
	}
	protected void notifyMessageListeners(Packet p) {
		mListeners.forEach(x -> x.messageReceived(p));
	}
	
	/**
	 * Register a ConnectionListener to the client. It will be notified if the connection  start or ends.
	 * 
	 * @param l
	 */
	public void addConnectionListener(ConnectionListener l) {
		if (l != null)
			cListeners.add(l);
	}
	protected void notifyConnectionListeners(boolean active) {
		cListeners.forEach(x -> x.connectionEvent(active));
	}


	public int getIdentifier() {
		return identifier;
	}

	/**
	 * Method to be called to establish the connection.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void startSession() throws UnknownHostException {
		if (s == null || s.isClosed()) {
			try {
				s = new Socket(serverAddress, serverPort);
				dos = new DataOutputStream(s.getOutputStream());
				dis = new DataInputStream(s.getInputStream());
				dos.writeInt(identifier);
				dos.flush();
				if (identifier == 0) {
					identifier = dis.readInt();
				}
				// start the receive loop
				new Thread(() -> receiveLoop()).start();
				notifyConnectionListeners(true);
			} catch (IOException e) {
				e.printStackTrace();
				// error, close session
				closeSession();
			}
		}
	}

	/**
	 * Send a packet to the specified destination (etiher a userId or groupId)
	 * 
	 * @param destId the destinatiion id
	 * @param data   the data to be sent
	 */
	public void sendPacket(int destId, byte[] data) {
		try {
			synchronized (dos) {
				dos.writeInt(destId);
				dos.writeInt(data.length);
				dos.write(data);
				dos.flush();
			}
		} catch (IOException e) {
			// error, connection closed
			closeSession();
		}
		
	}

	/**
	 * Start the receive loop. Has to be called only once.
	 */
	private void receiveLoop() {
		try {
			while (s != null && !s.isClosed()) {
				int sender = dis.readInt();
				int dest = dis.readInt();
				int length = dis.readInt();
				byte[] data = new byte[length];
				dis.readFully(data);

				if (sender == ServerMsg.SERVER_CLIENTID && dest == this.identifier) {
					ByteBuffer buffer = ByteBuffer.wrap(data);
					// Suppose que le serveur envoie un byte pour définir le type de réponse.
					byte responseType = buffer.get();

					if (responseType == 1) { // Si le type de réponse est 1, cela signifie la création de groupe.
						int groupId = buffer.getInt();
						System.out.println("Le groupe numéro " + groupId + " a été créé.");
					}
				} else {
					notifyMessageListeners(new Packet(sender, dest, data));
				}
			}
		} catch (IOException e) {
			// En cas d'erreur, fermer la connexion
			e.printStackTrace();
		}
		closeSession();
	}

	public void closeSession() {
		try {
			if (s != null)
				s.close();
		} catch (IOException e) {
		}
		s = null;
		notifyConnectionListeners(false);
	}

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		ClientMsg c = new ClientMsg("localhost", 1666);

		// add a dummy listener that print the content of message as a string
		c.addMessageListener(p -> System.out.println(p.srcId + " says to " + p.destId + ": " + new String(p.data)));

		// add a connection listener that exit application when connection closed
		c.addConnectionListener(active ->  {if (!active) System.exit(0);});

		c.startSession();
		System.out.println("Vous êtes : " + c.getIdentifier());

		// Thread.sleep(5000);

		Scanner sc = new Scanner(System.in);
		String lu = null;
		while (!"\\quit".equals(lu)) {
			try {
				System.out.println("\nQue souhaitez-vous faire? \n0 : envoyer un message\n1 : créer un groupe\n2 : supprimer un groupe\n3 : ajouter un membre à un groupe\n4 : supprimer un membre d'un groupe\n");
				int code = Integer.parseInt(sc.nextLine());
				if (code == 0) { //envoyer un msg
					System.out.println("\nA qui voulez vous écrire ? ");
					int dest = Integer.parseInt(sc.nextLine());
					System.out.println("\nVotre message ? ");
					lu = sc.nextLine();
					c.sendPacket(dest, lu.getBytes());
				}
				else if (code == 1) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);

					// byte 1 : create group on server
					dos.writeByte(1);
					System.out.println("\nhow many members?");
					int nb = Integer.parseInt(sc.nextLine()); // nb members
					dos.writeInt(nb);

					// list of members
					for (int i = 0 ; i < nb ; i++) {
						System.out.println("add a member : (type 0 to exit)");
						dos.writeInt(Integer.parseInt(sc.nextLine()));
					}

					dos.flush();

					c.sendPacket(0, bos.toByteArray());
				}
				else if (code == 2) { //supprimer un groupe
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);

					// byte 2 : delete group on server
					dos.writeByte(2);
					// id group
					System.out.println("quel groupe ?");
					dos.writeInt(Integer.parseInt(sc.nextLine()));
					dos.flush();
					c.sendPacket(0, bos.toByteArray());
				}

				else if (code == 3) { //ajouter un member à un groupe
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);

					// byte à 3 : ajouter un membre au serveur
					dos.writeByte(3);
					System.out.println("\nDans quel groupe voulez-vous ajouter un membre?");
					int idGroup = Integer.parseInt(sc.nextLine()); // idGroup
					dos.writeInt(idGroup);

					System.out.println("\nQuel utilisateur voulez-vous ajouter ?");
					int userId = Integer.parseInt(sc.nextLine());
					dos.writeInt(userId);
					dos.flush();
					c.sendPacket(0, bos.toByteArray());

				}
				else if (code == 4) { //supprimer un membre d'un groupe
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);

					// byte 4 : delete member from group on server
					dos.writeByte(4);
					// id group
					System.out.println("quel groupe ?");
					dos.writeInt(Integer.parseInt(sc.nextLine()));
					// id user
					System.out.println("quel utilisateur ?");
					dos.writeInt(Integer.parseInt(sc.nextLine()));
					dos.flush();
					c.sendPacket(0, bos.toByteArray());
				}
			} catch (InputMismatchException | NumberFormatException e) {
				System.out.println("Mauvais format");
			}
			}



		/*
		 * int id =1+(c.getIdentifier()-1) % 2; System.out.println("send to "+id);
		 * c.sendPacket(id, "bonjour".getBytes());
		 * 
		 * 
		 * Thread.sleep(10000);
		 */

		c.closeSession();

	}

}

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

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.server.ServerMsg;

import javax.imageio.ImageIO;

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
	private String username;
	private String password;
	private volatile boolean isAuthenticated = false;


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
	public ClientMsg(int id, String address, int port, String username, String password) {
		if (id < 0)
			throw new IllegalArgumentException("id must not be less than 0");
		if (port <= 0)
			throw new IllegalArgumentException("Server port must be greater than 0");
		serverAddress = address;
		serverPort = port;
		identifier = id;
		mListeners = new ArrayList<>();
		cListeners = new ArrayList<>();
		this.username = username;
		this.password = password;
	}

	/**
	 * Create a client without id, the server will provide an id during the the
	 * session start
	 *
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(String address, int port) {
		this(0, address, port, "defaultUsername", "password");
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
	public String getUsername() { return username; }
	public void setUsername(String username) {
		this.username = username;

		//send packet to the server; the server will update the username.
		//1byte for the type (4), 4bytes (an int) for the length of the username, then the username
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeByte(5);
			dos.writeInt(username.length());
			dos.write(username.getBytes(StandardCharsets.UTF_8));
			dos.flush();
			sendPacket(0, bos.toByteArray());
			System.out.println("packet for username update sent to server");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public boolean sendLoginRequest(String username, String password) {
		//DON'T UPDATE USERNAME AND PASSWORD HERE, BC AUTHENTICATION COULD FAIL. instead see receiveLoop()
		//this.username = username;
		//this.password = password;

		//send packet to the server; the server will update the username.
		//1byte for the type (6), 4 bytes for the username length,
		//then the username, 4bytes for the password length, then the password
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeByte(6);
			dos.writeInt(username.length());
			dos.write(username.getBytes(StandardCharsets.UTF_8));
			dos.writeInt(password.length());
			dos.write(password.getBytes(StandardCharsets.UTF_8));
			dos.flush();
			sendPacket(0, bos.toByteArray());
			//System.out.println("login packet sent from sendLoginRequest() in ClientMsg");


//			// READ THE SERVER'S RESPONSE to return a boolean. i'm not sure it's the right place to do it.
//			int length = dis.readInt();
//			byte[] data = new byte[length];
//			dis.readFully(data);
///* TRACE */	System.out.println("TRACE : data received from server : " + new String(data));
//			if (data.length > 0) {
//				// The first byte of the data is the response type
//				int responseType = data[0]; //PROBLEM : LENGTH 0
//
//				if (responseType == 0) { // Authentication failed
//					System.out.println("Successfully authenticated.");
//					return true;
//				} else if (responseType == 1) { // Authentication succeeded
//					System.out.println("Authentication failed. Please try again.");
//					return false;
//				} else {
//					System.out.println("Received unexpected response type.");
//					return false;
//				}
//			} else { //empty response
///* TRACE */		System.out.println("ClientMsg received an empty reponse from the server");
//				return false;
//			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;

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
				//	dos.writeUTF(username);
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

				Packet packet = new Packet(sender, dest, data); // Create a packet to receive an image if there is one
				BufferedImage image = packet.getImage(); // Get the image from the packet
				if (image != null) {
					// TODO: The packet contains an image, send it to the listeners
					notifyMessageListeners(new Packet(sender, dest, data, image));
				} else {
					if (sender == ServerMsg.SERVER_CLIENTID && dest == this.identifier) {
						ByteBuffer buffer = ByteBuffer.wrap(data);
						// Suppose que le serveur envoie un byte pour définir le type de réponse.
						byte responseType = buffer.get();

						if (responseType == 1) { // Si le type de réponse est 1, cela signifie la création de groupe.
							int groupId = buffer.getInt();
							System.out.println("Le groupe numéro " + groupId + " a été créé.");
						}
						else if (responseType == 10) { //authentication successful
							System.out.println("You've been successfully authenticated. Type anything to continue.");
							isAuthenticated = true ;

						}
						else if (responseType == 11) {
							System.out.println("Authentication failed. Please try again.");
							isAuthenticated = false ;
						}

					} else {
						notifyMessageListeners(new Packet(sender, dest, data));
					}
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

		c.addMessageListener(p -> {
//			String username = c.getUsername();
			System.out.println(p.srcId + " says to " + p.destId + ": " + new String(p.data));
		});

		// add a connection listener that exit application when connection closed
		c.addConnectionListener(active ->  {if (!active) System.exit(0);});

		c.startSession();
		Scanner sc = new Scanner(System.in);

		// AUTHENTIFICATION
		String rep = "a" ;

		//while the user is trying to authenticate, and is not authenticated
		while (!rep.equalsIgnoreCase("N") && !c.isAuthenticated) {
			System.out.println("Voulez vous vous connecter? Y/N");
			rep = sc.nextLine();
			if (rep.equals("Y") || rep.equals("y")) { //the user is trying to authenticate
				System.out.println("Entrez votre nom d'utilisateur : ");
				String username = sc.nextLine();
				System.out.println("Entrez votre mot de passe : ");
				String password = sc.nextLine();
				c.sendLoginRequest(username, password);
				System.out.println(c.getUsername());
				if (c.isAuthenticated) {
					c.setUsername(username);
					break;
				}
			}
		}
		//now, either the user is authenticated, or they chose not to authenticate
		System.out.println("Hello "+ c.getUsername() + "!");



//		while (!rep.equals("Y") && !rep.equals("N")) {
//			System.out.println("Voulez vous vous connecter? Y/N");
//			rep = sc.nextLine();
//		}
//		if (rep.equals("Y")) {
//			System.out.println("Entrez votre nom d'utilisateur : ");
//			String username = sc.nextLine();
//			c.setUsername(username);
//			System.out.println("Entrez votre mot de passe : ");
//			String password = sc.nextLine();
//			c.sendLoginRequest(username, password);
//			System.out.println("Vous êtes " + c.getUsername());
//
//		}
//		else {
//			System.out.println("\nVous êtes : " + c.getUsername());
//		}

		String lu = null;
		while (!"\\quit".equals(lu)) {
			try {
				System.out.println("\n" + c.getUsername()+ ", que souhaitez-vous faire? \n0 : envoyer un message\n1 : créer un groupe\n2 : supprimer un groupe\n3 : ajouter un membre à un groupe\n4 : supprimer un membre d'un groupe\n5 : changer de nom\n7 : changer de mot de passe\n8 : Ajouter un contact\\n\")");
				int code = Integer.parseInt(sc.nextLine());

				if (code == 0) { //envoyer un msg
					System.out.println("\nA qui voulez vous écrire ? ");
					int dest = Integer.parseInt(sc.nextLine());

					System.out.println("\n Voulez-vous envoyer une image? \n0 : oui\n1 : non");
					int codeI = Integer.parseInt(sc.nextLine());
					if (codeI == 0) { // Send an image
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(bos);

						dos.writeByte(6);
						System.out.println("Adresse de l'image - format jpg:");
						String imagePath = sc.nextLine();
						BufferedImage image = ImageIO.read(new File(imagePath));
						Packet packet = new Packet(c.getIdentifier(), dest, bos.toByteArray(), image);
						c.sendPacket(dest, packet.toByteArray());
					}
					System.out.println("\nVotre message ? ");
					lu = sc.nextLine();
					c.sendPacket(dest, lu.getBytes());

				}

				else if (code == 1) { //creer un groupe
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);

					// byte 1 : create group on server
					dos.writeByte(1);

					//empty int list
					List<Integer> members = new ArrayList<>();

					int member = 1;
					// list of members
					while (member != 0) {
						System.out.println("add a member : (type 0 to exit)");
						member = Integer.parseInt(sc.nextLine());
						members.add(member);
					}
					dos.writeInt(members.size()); //nb de membres envoyé dans le paquet
					//on envoie dans le paquet chaque userId, le sender compris
					dos.writeInt(c.getIdentifier()); //on envoie le sender
					for (int i = 0 ; i < members.size() ; i++) {
						dos.writeInt(members.get(i)); //on envoie tous les autres membres
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

					dos.writeByte(4); //premier byte à 4 pour supprimer un membre
					System.out.println("\nDans quel groupe voulez-vous supprimer un membre?");
					int idGroup = Integer.parseInt(sc.nextLine()); // idGroup
					dos.writeInt(idGroup);

					System.out.println("\nQuel utilisateur voulez-vous supprimer ?");
					int userId = Integer.parseInt(sc.nextLine());
					dos.writeInt(userId);
					dos.flush();
					c.sendPacket(0, bos.toByteArray());
				}

				else if (code == 5) { //changer de nom
					System.out.println("\nNouveau nom d'utilisateur : ");
					String newUsername = sc.nextLine();
					c.setUsername(newUsername);
					System.out.println("Vous êtes " + c.getUsername());
				}

				else if (code == 7) { //change password
					System.out.println("\nSaisissez votre ancien mot de passe : ");
					String oldPassword = sc.nextLine();
					//get password associated with the username, without using the server
					boolean isAuthenticated = c.getPassword().equals(oldPassword);
					//if the password is correct, the server will ask for the new password
					//AUTHENTIFICATION
					if (isAuthenticated) {
						System.out.println("Mot de passe correct. Nouveau mot de passe : ");
						String newPassword = sc.nextLine();
						//send packet to the server; the server will update the password.
						//1byte for the type (7), 4bytes (an int) for the length of the password, then the password
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(bos);
						try {
							dos.writeByte(7);
							dos.writeInt(newPassword.length());
							dos.write(newPassword.getBytes(StandardCharsets.UTF_8));
							dos.flush();
							c.sendPacket(0, bos.toByteArray());
							System.out.println("packet for password update sent to server. depuis ClientMsg");
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.out.println("Mot de passe non reconnu. Veuillez réessayer.");
						continue;
					}
				} else if (code == 8) { // ajouter contact à un utilisateur
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);

					// Type 8 : Ajout de contact sur le serveur
					dos.writeByte(8);

					// Demander à l'utilisateur les informations sur le contact à ajouter
					System.out.println("\nEntrez l'identifiant du contact : ");
					int contactId = Integer.parseInt(sc.nextLine());
					try {
						dos.writeInt(contactId);
						dos.flush();
						c.sendPacket(0, bos.toByteArray());
						System.out.println("Packet for adding contact sent to server.");
					} catch (IOException e) {
						e.printStackTrace();
					}
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
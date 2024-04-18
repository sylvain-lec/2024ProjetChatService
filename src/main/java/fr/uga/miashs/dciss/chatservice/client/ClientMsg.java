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

	/**
	 * sets username client-side, and sends a packet to the server to update the username server-side.
	 * @param username
	 */
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets password client side, and sends a packet to the server to update the password server-side.
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;

		//send packet to the server; the server will update the password.
		//1byte for the type (7), 4bytes (an int) for the length of the password, then the password
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeByte(7);
			dos.writeInt(password.length());
			dos.write(password.getBytes(StandardCharsets.UTF_8));
			dos.flush();
			sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public String getPassword() {
		return password;
	}

	/**
	 * Method to be called to establish the connection.
	 *
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void startSession(String password) throws UnknownHostException {
		if (s == null || s.isClosed()) {
			try {
				s = new Socket(serverAddress, serverPort);
				dos = new DataOutputStream(s.getOutputStream());
				dis = new DataInputStream(s.getInputStream());
				dos.writeInt(identifier);
				dos.writeUTF(password);
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
						int groupId = buffer.get();
						System.out.println("Le groupe numéro " + groupId + " a été créé.");

					}
					else if (responseType == 9) {
						System.out.println("packet received in receiveloop()");
						//the packet contains an int for length and the username
						int usernameLength = buffer.getInt();
						byte[] usernameBytes = new byte[usernameLength];
						buffer.get(usernameBytes);
						String username = new String(usernameBytes, StandardCharsets.UTF_8); //retrieve the username
						this.username = username; //set the username
						System.out.println("username received : " + username);
					}
					else if (responseType == 10) { //authentication successful
						System.out.println("You've been successfully authenticated. Type anything to continue.");
						//set userId to the userId received in the packet
						int newUserId = buffer.getInt();
	/* SET HERE	*/		this.identifier = newUserId;
						System.out.println("new id : "+ this.getIdentifier());
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
			System.out.println(p.srcId + " says to " + p.destId + ": " + new String(p.data));
		});

		// add a connection listener that exit application when connection closed
		c.addConnectionListener(active ->  {if (!active) System.exit(0);});

		Scanner sc = new Scanner(System.in);

		/* ----------------- AUTHENTIFICATION ------------------------- */
		String rep = "b" ;
		c.isAuthenticated = false ;

		while (!c.isAuthenticated && !rep.equalsIgnoreCase("R")) {
			//ask to register or authenticate
			System.out.println("Do you want to authenticate or register as a new member? (A/R)");
			rep = sc.nextLine();

			//AUTHENTIFICATION :
			if (rep.equalsIgnoreCase("A")) {
				System.out.println("what is your id ?");
				int id = Integer.parseInt(sc.nextLine());
				c.identifier = id;
				System.out.println("Enter your password: ");
				String password = sc.nextLine();
				c.startSession(password); //prendre en paramètre un mot de passe

				//send packet to retrieve username and password from server
				c.askInfos();
				c.isAuthenticated = true ;

			//NEW USER : registers with an id given by the server. username and password chosen by the user
			} else if (rep.equalsIgnoreCase("R")) {
				System.out.println("Enter your password: ");
				String password = sc.nextLine();
				c.startSession(password);
				System.out.println("Enter your username: ");
				String username = sc.nextLine();
				c.username = username;
				c.password = password;

				System.out.println("you are now registered as " + c.getUsername() + " with id " + c.getIdentifier());
				c.isAuthenticated = true ;
			}
		}

		//wait for 1s
		Thread.sleep(1000);
		//now, either the user registered, or the user is authenticated
		System.out.println("Hello "+ c.getUsername() + "!");

		String lu = null;
		while (!"\\quit".equals(lu)) {
			try {
				System.out.println("\n" + c.getUsername()+ ", que souhaitez-vous faire? \n0 : envoyer un message\n1 : créer un groupe\n2 : supprimer un groupe\n3 : ajouter un membre à un groupe\n4 : supprimer un membre d'un groupe\n5 : changer de nom\n7 : changer de mot de passe\n8 : Ajouter un contact\n");
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
					c.creationGroupe();
				}

				else if (code == 2) { //supprimer un groupe
					c.supprimerGroupe();
				}

				else if (code == 3) { //ajouter un member à un groupe
					c.addMember();
				}

				else if (code == 4) { //supprimer un membre d'un groupe
					c.removeMember();
				}

				else if (code == 5) { //changer de nom
					System.out.println("\nNew username : ");
					String newUsername = sc.nextLine();
					c.setUsername(newUsername);
					System.out.println("\nYou are " + c.getUsername());
				}

				else if (code == 7) { //change password
					boolean reussi = c.updatePassword();
					if (!reussi) {
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
					System.out.println("\nEntrez le nom du contact : ");
					String contactName = sc.nextLine();
					try {
						dos.writeInt(contactId);
						dos.writeInt(contactName.getBytes(StandardCharsets.UTF_8).length);
						dos.write(contactName.getBytes(StandardCharsets.UTF_8));
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

		c.closeSession();

	}

	/**
	 * Update the password of the user
	 * @return true if the password has been updated, false otherwise
	 */
	private boolean updatePassword() {
		Scanner sc = new Scanner(System.in);

		System.out.println("\nSaisissez votre ancien mot de passe : ");
		String oldPassword = sc.nextLine();
		System.out.println("old password : " + oldPassword);
		System.out.println("this password : " + this.getPassword());
		//get password associated with the username, without using the server
		boolean isAuthenticated = this.getPassword().equals(oldPassword);
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
				this.sendPacket(0, bos.toByteArray());
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.password = newPassword;
			return true;
		} else {
			System.out.println("Mot de passe non reconnu. Veuillez réessayer.");
			return false;
		}
	}

	/**
	 * Remove a member from a group on the server
	 */
	private void removeMember() {
		try {
		Scanner sc = new Scanner(System.in);

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
		this.sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a member to a group on the server

	 */
	private void addMember() {
		try {
			Scanner sc = new Scanner(System.in);
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
			this.sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Delete a group on the server

	 */
	private void supprimerGroupe() {
		try {
			Scanner sc = new Scanner(System.in);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);

			// byte 2 : delete group on server
			dos.writeByte(2);
			// id group
			System.out.println("quel groupe ?");
			dos.writeInt(Integer.parseInt(sc.nextLine()));
			dos.flush();
			this.sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a group on the server
	 */
	private void creationGroupe() {
		Scanner sc = new Scanner(System.in);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		// byte 1 : create group on server
		try {
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
		dos.writeInt(this.getIdentifier()); //on envoie le sender
		for (int i = 0 ; i < members.size() ; i++) {
			dos.writeInt(members.get(i)); //on envoie tous les autres membres
		}
		dos.flush();
		this.sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Ask the server for the username and password associated with the userId
	 */
	private void askInfos() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeByte(11);
			dos.writeInt(this.getIdentifier());
			dos.flush();
			sendPacket(0, bos.toByteArray());
			System.out.println("packet sent from askInfos");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}

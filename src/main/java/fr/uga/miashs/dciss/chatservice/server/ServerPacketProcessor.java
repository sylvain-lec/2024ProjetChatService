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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fr.uga.miashs.dciss.chatservice.common.Packet;

public class ServerPacketProcessor implements PacketProcessor {
	private final static Logger LOG = Logger.getLogger(ServerPacketProcessor.class.getName());
	private ServerMsg server;

	public ServerPacketProcessor(ServerMsg s) {
		this.server = s;
	}

	@Override
	public void process(Packet p) throws IOException {

		LOG.info("PACKET RECU DANS process() de ServerPacketProcessor");


		// ByteBufferVersion. On aurait pu utiliser un ByteArrayInputStream + DataInputStream à la place
		ByteBuffer buf = ByteBuffer.wrap(p.data);
		byte type = buf.get();

		//PAQUET BIEN ENVOYE dans setUsername de ClientMsg mais PAS RECU ICI
		if (type == 1) { // cas creation de groupe
			createGroup(p.srcId, buf);
		}
		else if (type == 2) { //cas suppression de groupe
			removeGroup(p);
		}
		else if (type == 3) { // cas ajout de membre dans un groupe
			int groupId = buf.getInt(); // ID du groupe
			int userId = buf.getInt(); // ID de l'utilisateur
			addMember(p.srcId, groupId, userId);
		}
		else if (type == 4) { // cas suppression de membre dans un groupe
			removeMember(p.srcId, buf);
		}
		else if (type == 5) { //cas mettre a jour le username
			updateUsername(p, buf);
		}

		else if (type == 7) { //update password
			updatePassword(p, buf);
		}

		else if (type == 8) { //addContact
			addContact(p, buf);

		} else if (type == 9) { //cas demande de liste de contacts
			sendContactsList(p.srcId);
		}
		//dans le cas où le type n'est pas déterminé

		else if (type == 11) { //CASE INFORMATION RETRIEVAL
			sendInfos(p, buf);
		}

    	else if (type == 9) { //cas envoi de fichier
			sendFile(p.srcId, buf);
		}

			//dans le cas où le type n'est pas déterminé
		else {
			LOG.warning("Server message of type=" + type + " not handled by procesor");
		}
	}

    private void addContact(Packet p, ByteBuffer buf) {
        int userId = p.srcId;
        int contactNameLength = buf.getInt();
        byte[] contactNameBytes = new byte[contactNameLength];
        buf.get(contactNameBytes);
        String contactName = new String(contactNameBytes, StandardCharsets.UTF_8);
        UserMsg user = server.getUser(userId);
        if (user != null) {
            user.addContact(contactName);
            LOG.info("Contact added successfully for user with ID: " + userId +  ", contact Name: " + contactName);
        } else {
            LOG.warning("User with ID " + userId + " not found. Contact not added.");
        }
    }

    /**
	 *  Protocol to send a file to another user
	 *  packet format : type (1 byte) + destId (4 bytes) + filename length (4 bytes) + filename + file length (4 bytes) + file
	 *  @param userId
	 *  @param data
	 */
	private void sendFile(int userId, ByteBuffer data) {
		LOG.info("ServerPacketProcessor : sendFile() called");
		int destId = data.getInt();
		int length = data.getInt();
		// Get the filename
		byte[] filenameBytes = new byte[length];
		data.get(filenameBytes);
		String filename = new String(filenameBytes, StandardCharsets.UTF_8);
		// Get the file length
		length = data.getInt();
		byte[] fileBytes = new byte[length];
		data.get(fileBytes);
		// Send the file to the destination user
		Packet response = new Packet(userId, destId, fileBytes);
		server.getUser(destId).process(response);
	}

	/**
	 * Sends the username of the user to the client when asked to. used by the client to get back its username after login
	 * @param p
	 * @param buf
	 */
	private void sendInfos(Packet p, ByteBuffer buf) {
		int userId = p.srcId; //id du user
		String username = server.getUser(userId).getUsername(); //on récupère le username
		byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
		int lengthU = username.getBytes().length; //longueur du username

		String password = server.getUser(userId).getPassword(); //on récupère le password
		byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
		int lengthP = password.getBytes().length; //longueur du password

		// Create a byte buffer with 4 extra bytes for the length
		ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + lengthU + 4 + lengthP);
		buffer.put((byte) 9);
		buffer.putInt(lengthU);
		buffer.put(usernameBytes);
		buffer.putInt(lengthP);
		buffer.put(passwordBytes);

		// nv tableau qui concatène la longueur du msg et le msg lui-même
		byte[] data = buffer.array();
		Packet reponse = new Packet(0, userId, data); //on forme le packet
		server.getUser(userId).process(reponse); //on l'envoie
	}

	/**
	 * Reads the new password from the packet and updates the user's password
	 * @param p
	 * @param buf
	 */
	private void updatePassword(Packet p, ByteBuffer buf) {
		int userId = p.srcId; //id du user
		int length = buf.getInt(); //on recupere la longueur du password
		byte[] passwordBytes = new byte[length]; //on recupere le password
		buf.get(passwordBytes);
		String password = new String(passwordBytes, StandardCharsets.UTF_8);

		//on met à jour le password côté serveur (setPassword() de la classe UserMsg)
		server.getUser(userId).setPassword(password);
		LOG.info("userId " + userId + " updated their password to : " + password);
	}

	/**
	 * Reads the new username from the packet and updates the user's username
	 * @param p
	 * @param buf
	 */
	private void updateUsername(Packet p, ByteBuffer buf) {
		int userId = p.srcId;
		int length = buf.getInt();
		byte[] usernameBytes = new byte[length];
		buf.get(usernameBytes);
		String username = new String(usernameBytes, StandardCharsets.UTF_8);

		//on met à jour le username côté serveur (setUsername() de la classe UserMsg)
		server.getUser(userId).setUsername(username);
		LOG.info("userId " + userId + " a mis à jour son username en " + username);
		//TRACE : print every userid and their username
		LOG.info(server.getUsers());
	}


	private void sendContactsList(int userId) {
		LOG.info("List of contacts for user ");
		UserMsg user = server.getUser(userId);
		if (user != null) {
			List<String> contacts = user.getContacts();


			ByteBuffer buffer = ByteBuffer.allocate(1024);
			buffer.putInt(contacts.size());


			for (String contact : contacts) {
				byte[] contactBytes = contact.getBytes(StandardCharsets.UTF_8);
				buffer.putInt(contactBytes.length);
				buffer.put(contactBytes);
			}

			buffer.flip();

			server.sendPacketToUser(userId, buffer.array());
		} else {
			LOG.warning("User with ID " + userId + " not found. Unable to display the list of contacts.");
		}
	}




	public void createGroup(int ownerId, ByteBuffer data) throws IOException {
		int nb = data.getInt();
		GroupMsg g = server.createGroup(ownerId);
		for (int i = 0; i < nb; i++) {
			g.addMember(server.getUser(data.getInt()));
		}
		//Packet qui informe le owner que le groupe a été créé
		String msgOwner = "Vous avez créé le groupe " + g.getId() ;
		byte[] msgOwnerBytes = msgOwner.getBytes(StandardCharsets.UTF_8);//msg à envoyer, converti en bytes
		int length = msgOwner.getBytes().length;
		// bytebuffer
		ByteBuffer buffer = ByteBuffer.allocate(1+ 4 + 4 + length);
		buffer.put((byte) 1);
		buffer.putInt(g.getId());
		buffer.putInt(length);
		buffer.put(msgOwnerBytes);
		// nv tableau qui concatène la longueur du msg et le msg lui-même
		byte[] dataMsg = buffer.array();

		Packet reponse = new Packet(0, ownerId, dataMsg);
		server.getUser(ownerId).process(reponse);

		// packet qui informe les autres membres du groupe de la création du groupe
		String msg = "Le groupe " + g.getId() + " a été créé par le user " + ownerId + ". Les membres sont : ";
		for (UserMsg u : g.getMembers()) { //ajouter à la string le userid de chaque membre.
			msg += u.getId() + ", ";
		}
		//remove last ","
		msg = msg.substring(0, msg.length() - 2);

		byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8); //msg à envoyer, converti en bytes
		int length2 = msg.getBytes().length; //longueur du msg à envoyer

// bytebuffer
		ByteBuffer buffer2 = ByteBuffer.allocate(1+ 4 + 4 + length2);
		buffer2.put((byte) 1);
		buffer2.putInt(g.getId()); //on envoie l'id du groupe (pour que les membres puissent l'identifier
		buffer2.putInt(length2);
		buffer2.put(msgBytes);
// nv tableau qui concatène la longueur du msg et le msg lui-même
		byte[] dataMsg2 = buffer2.array();

		//send to everyone, except to the owner
		for (UserMsg u : g.getMembers()) {
			if (u.getId() != ownerId) {
				Packet reponse2 = new Packet(0, u.getId(), dataMsg2);
				u.process(reponse2);
			}
		}

	}

	/**
	 * Suppression d'un groupe
	 * packet format : type (1 byte) + groupId (4 bytes)
	 * @param p : Packet qui demande la suppression d'un groupe
	 */
	public void removeGroup(Packet p) {
		ByteBuffer buf = ByteBuffer.wrap(p.data);
		buf.get(); //on se débarasse du premier byte, qui indique le type de protocole
		int src = p.srcId;
		int groupId = buf.getInt(); //on récupère le numéro de groupe
		//on récupère le groupe associé à ce groupId
		GroupMsg groupe = server.getGroup(groupId);

		if (groupe == null) {
			// informer le sender par un packet que le groupe n'existe pas
			String msg = "Le groupe " + groupId + " n'existe pas";
			byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8); //msg à envoyer, converti en bytes
			int length = msg.getBytes().length; //longueur du msg à envoyer

			// créer un buffer avec 4 extra bytes pour la longueur
			ByteBuffer buffer = ByteBuffer.allocate(1+4 + length);
			buffer.put((byte) 2);
			buffer.putInt(length);
			buffer.put(msgBytes);
			// nv tableau qui concatène la longueur du msg et le msg lui-même
			byte[] data = buffer.array();

			Packet reponse = new Packet(0, src, data); //on forme le packet
			server.getUser(src).process(reponse); //on l'envoie

			LOG.info("userId " + src + " a essayé de supprimer le groupe " + groupId + " qui n'existe pas"); //Trace pour le serveur

		} else { //le groupe existe
			// vérifier que le sender est le owner
			if (src != groupe.getOwner().getId()) {
				LOG.info("le sender " + src + " n'est pas le propriétaire du groupe " + groupId);
				//informer le sender par un packet qu'il n'a pas les droits pour supprimer les groupes
				String msg = "Vous n'avez pas les droits pour supprimer le groupe " + groupId;
				byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
				int length = msg.getBytes().length; //longueur du msg à envoyer

				// Create a byte buffer with 4 extra bytes for the length
				ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);
				// Put the length and the text bytes into the buffer
				buffer.put((byte) 2);
				buffer.putInt(length);
				buffer.put(msgBytes);
				// nv tableau qui concatène la longueur du msg et le msg lui-même
				byte[] data = buffer.array();

				Packet reponse = new Packet(0, src, data);
				server.getUser(src).process(reponse);

			}
			else { //le sender est le owner
				//msg à envoyer, converti en bytes
				String msg = "Le groupe " + groupId + " a été supprimé";
				byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);

				//longueur du msg à envoyer
				int length = msg.getBytes().length;

				// Create a byte buffer with 4 extra bytes for the length
				ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);

				// Put the length and the text bytes into the buffer
				buffer.put((byte) 2);
				buffer.putInt(length);
				buffer.put(msgBytes);

				// nv tableau qui concatène la longueur du msg et le msg lui-même
				byte[] data = buffer.array();

				for (UserMsg u : groupe.getMembers()) {
					Packet reponse = new Packet(0, u.getId(), data);

					u.process(reponse);
				}
				server.removeGroup(groupId);
			}
		}
	}

	/**
	 * Add a member to a group. The sender must be the owner of the group.
	 * packet format : type (1 byte) + groupId (4 bytes) + userId (4 bytes)
	 * @param srcId
	 * @param groupId
	 * @param userId
	 */
	private void addMember(int srcId, int groupId, int userId) {
		GroupMsg group = server.getGroup(groupId);
		if (group != null) {
			UserMsg user = server.getUser(userId);
			if (user != null) {
				group.addMember(user);
				// Envoyer une notification ou une confirmation si nécessaire
				//msg à envoyer, converti en bytes
				String msg = "User " + userId + " has been added to group " + groupId;
				byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
				int length = msg.getBytes().length;

				// Create a byte buffer with 4 extra bytes for the length
				ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);

				// Put the length and the text bytes into the buffer
				buffer.put((byte) 3);
				buffer.putInt(length);
				buffer.put(msgBytes);

				// nv tableau qui concatène la longueur du msg et le msg lui-même
				byte[] data = buffer.array();

				for (UserMsg u : group.getMembers()) {
					Packet reponse = new Packet(0, u.getId(), data);
					u.process(reponse);
				}
			} else {
				LOG.warning("Attempt to add non-existent user " + userId + " to group " + groupId);
				String msgNoUser = "User " + userId + " does not exist";
				byte[] msgNoUserBytes = msgNoUser.getBytes(StandardCharsets.UTF_8);
				int length = msgNoUser.getBytes().length;
				ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);
				buffer.put((byte) 3);
				buffer.putInt(length);
				buffer.put(msgNoUserBytes);
				byte[] data = buffer.array();
				Packet reponse = new Packet(0, srcId, data);
				server.getUser(srcId).process(reponse);
			}
		} else {
			LOG.warning("Group " + groupId + " not found");
			String msgNoGroup = "Group " + groupId + " does not exist";
			byte[] msgNoGroupBytes = msgNoGroup.getBytes(StandardCharsets.UTF_8);
			int length = msgNoGroup.getBytes().length;
			ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);
			buffer.put((byte) 3);
			buffer.putInt(length);
			buffer.put(msgNoGroupBytes);
			byte[] data = buffer.array();
			Packet reponse = new Packet(0, srcId, data);
			server.getUser(srcId).process(reponse);
		}
	}

	/**
	 * Remove a member from a group.
	 * packet format : type (1 byte) + groupId (4 bytes) + userId (4 bytes)
	 * @param ownerId
	 * @param data
	 */
	private void removeMember(int ownerId, ByteBuffer data) {
		int groupId = data.getInt();
		int userId = data.getInt();
		GroupMsg group = server.getGroup(groupId);
		String msg = null;
		UserMsg user = server.getUser(userId);
		if (group != null && user != null) {
			group.removeMember(user);
			// Envoyer une notification aux autres membres du groupe
			msg = "User " + userId + " has been removed from group " + groupId;
			byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8); //msg à envoyer, converti en bytes
			int length = msg.getBytes().length; //longueur du msg à envoyer

			ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);
			buffer.put((byte) 4);
			buffer.putInt(length);
			buffer.put(msgBytes);
			byte[] dataArray = buffer.array(); // nv tableau qui concatène la longueur du msg et le msg lui-même

			for (UserMsg u : group.getMembers()) {
				Packet reponse = new Packet(0, u.getId(), dataArray);
				u.process(reponse);
			}
			return;
		}
		if (group != null) { //user doesn't exist
			msg = "User " + userId + " does not exist or isn't in this group";
		} else { //group == null
			msg = "Group " + groupId + " does not exist"; }
		byte[] msgBytes2 = msg.getBytes(StandardCharsets.UTF_8); //msg à envoyer, converti en bytes
		int length2 = msg.getBytes().length; //longueur du msg à envoyer
		ByteBuffer buffer2 = ByteBuffer.allocate(1 + 4 + length2);
		buffer2.put((byte) 4);
		buffer2.putInt(length2);
		buffer2.put(msgBytes2);
		byte[] dataArray2 = buffer2.array();
		Packet reponse = new Packet(0, ownerId, dataArray2);
		server.getUser(ownerId).process(reponse);
	}

//	private void login(int userId, ByteBuffer buf) {
//		LOG.info("on est dans login()");
//		int usernameLength = buf.getInt();
//		byte[] usernameBytes = new byte[usernameLength];
//		buf.get(usernameBytes);
//		String username = new String(usernameBytes, StandardCharsets.UTF_8);
//
//		int passwordLength = buf.getInt();
//		byte[] passwordBytes = new byte[passwordLength];
//		buf.get(passwordBytes);
//		String password = new String(passwordBytes, StandardCharsets.UTF_8);
//
//		// Authenticate the user
//		int userIdTentative = server.authenticateUser(username, password);
//		//AUTHENTICATION SUCCEEDED
//		if (userIdTentative != 0) {
//			LOG.info("from ServerPacketProcessor, login() : User " + username + " authenticated successfully");
//
//			// Send a message to the user to confirm the authentication
//			//AND SEND THEM THEIR USERID
//			byte confirmationCode = 10; // 10 is the confirmation code for successful authentication
//			//get userId associated with this username and password
//
//			ByteBuffer buffer = ByteBuffer.allocate(1 + 4 );
//			buffer.put(confirmationCode);
//			buffer.putInt(userIdTentative);
//			byte[] data = buffer.array();
//			Packet reponse = new Packet(0, userId, data);
//			server.getUser(userId).process(reponse); //getUser() in ServerMsg
//			LOG.info("old user id found : " + userIdTentative);
//
//
//			//AUTHENTICATION FAILED
//		} else { //authenticateUser returned 0, which means the userId wasn't found
//			LOG.info("\nfrom ServerPacketProcessor, login() : Authentication failed for user " + username);
//
//			// Send a message to the user to inform that the authentication failed
//			//MESSAGE INUTILE
//			String msg = "\nfrom ServerPacketProcessor, login() : Authentication failed";
//			byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
//			int length = msg.getBytes().length;
//			byte confirmationCode = 11; // 11 is the code for authentication failure
//			ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);
//			buffer.put(confirmationCode);
//			buffer.putInt(length);
//			buffer.put(msgBytes);
//			byte[] data = buffer.array();
//			Packet reponse = new Packet(0, userId, data);
//			server.getUser(userId).process(reponse);
//		}
//	}
}

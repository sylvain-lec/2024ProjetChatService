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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import fr.uga.miashs.dciss.chatservice.common.Packet;

public class ServerPacketProcessor implements PacketProcessor {
	private final static Logger LOG = Logger.getLogger(ServerPacketProcessor.class.getName());
	private ServerMsg server;

	public ServerPacketProcessor(ServerMsg s) {
		this.server = s;
	}

	@Override
	public void process(Packet p) {
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
			int userId = p.srcId; //id du user
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
		else if (type == 6) { //cas login
			//call method :
			login(p.srcId, buf);
		}
		else if (type == 7) { //update password
			int userId = p.srcId; //id du user
			int length = buf.getInt(); //on recupere la longueur du password
			byte[] passwordBytes = new byte[length]; //on recupere le password
			buf.get(passwordBytes);
			String password = new String(passwordBytes, StandardCharsets.UTF_8);

			//on met à jour le password côté serveur (setPassword() de la classe UserMsg)
			server.getUser(userId).setPassword(password);
			LOG.info("userId " + userId + " a mis à jour son password en " + password);
		}
			//dans le cas où le type n'est pas déterminé
		else {
				LOG.warning("Server message of type=" + type + " not handled by procesor");
		}
	}

	public void createGroup(int ownerId, ByteBuffer data) {
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
		ByteBuffer buffer = ByteBuffer.allocate(4 + length);
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
		ByteBuffer buffer2 = ByteBuffer.allocate(4 + length2);
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
	 *
	 * @param p : Packet qui demande la suppression d'un groupe
	 */
	public void removeGroup(Packet p) {
		ByteBuffer buf = ByteBuffer.wrap(p.data);
		buf.get(); //on se débarasse du premier byte, qui indique le type de protocole
		int src = p.srcId;

		//on récupère le numéro de groupe
		int groupId = buf.getInt();
		//on récupère le groupe associé à ce groupId
		GroupMsg groupe = server.getGroup(groupId);
		if (groupe == null) {

			// informer le sender par un packet que le groupe n'existe pas
			String msg = "Le groupe " + groupId + " n'existe pas";
			byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8); //msg à envoyer, converti en bytes
			int length = msg.getBytes().length; //longueur du msg à envoyer

			// créer un buffer avec 4 extra bytes pour la longueur
			ByteBuffer buffer = ByteBuffer.allocate(4 + length);
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

				//longueur du msg à envoyer
				int length = msg.getBytes().length;

				// Create a byte buffer with 4 extra bytes for the length
				ByteBuffer buffer = ByteBuffer.allocate(4 + length);

				// Put the length and the text bytes into the buffer
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
				ByteBuffer buffer = ByteBuffer.allocate(4 + length);

				// Put the length and the text bytes into the buffer
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

//	addMember(p.srcId, groupId, userId);
	private void addMember(int srcId, int groupId, int userId) {
		//int groupId = data.getInt();
		//int userId = data.getInt();
		GroupMsg group = server.getGroup(groupId);
		if (group != null) {
			UserMsg user = server.getUser(userId);
			if (user != null) {
				group.addMember(user);
				// Envoyer une notification ou une confirmation si nécessaire
				//msg à envoyer, converti en bytes
				String msg = "L'utilisateur " + userId + " a été ajouté au groupe " + groupId;
				byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);

				//longueur du msg à envoyer
				int length = msg.getBytes().length;

				// Create a byte buffer with 4 extra bytes for the length
				ByteBuffer buffer = ByteBuffer.allocate(4 + length);

				// Put the length and the text bytes into the buffer
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
			}
		} else {
			LOG.warning("Group " + groupId + " not found");
		}
	}
	private void removeMember(int ownerId, ByteBuffer data) {
		int groupId = data.getInt();
		int userId = data.getInt();
		GroupMsg group = server.getGroup(groupId);
		if (group != null) {
			UserMsg user = server.getUser(userId);
			if (user != null) {
				group.removeMember(user);

				// Envoyer une notification aux autres membres du groupe
				String msg = "L'utilisateur " + userId + " a été retiré du groupe " + groupId;
				byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8); //msg à envoyer, converti en bytes
				int length = msg.getBytes().length; //longueur du msg à envoyer

				ByteBuffer buffer = ByteBuffer.allocate(4 + length);
				buffer.putInt(length);
				buffer.put(msgBytes);

			// nv tableau qui concatène la longueur du msg et le msg lui-même
				byte[] dataArray = buffer.array();

				for (UserMsg u : group.getMembers()) {
					Packet reponse = new Packet(0, u.getId(), dataArray);
					u.process(reponse);
				}

			} else {
				LOG.warning("Attempt to remove non-existent user " + userId + " from group " + groupId);
			}
		} else {
			LOG.warning("Group " + groupId + " not found");
		}


	}

	private void login(int userId, ByteBuffer buf) {
		int usernameLength = buf.getInt();
		byte[] usernameBytes = new byte[usernameLength];
		buf.get(usernameBytes);
		String username = new String(usernameBytes, StandardCharsets.UTF_8);

		int passwordLength = buf.getInt();
		byte[] passwordBytes = new byte[passwordLength];
		buf.get(passwordBytes);
		String password = new String(passwordBytes, StandardCharsets.UTF_8);

		// Authenticate the user
		boolean authenticated = server.authenticateUser(userId, password);
		if (authenticated) {
			LOG.info("from ServerPacketProcessor, login() : User " + username + " authenticated successfully");

			// Send a message to the user to confirm the authentication
			String msg = "from ServerPacketProcessor, login() : Authentication successful";
			byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
			int length = msg.getBytes().length;
			int confirmationCode = 0;
			ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + length);
			buffer.putInt(confirmationCode);
			buffer.putInt(length);
			buffer.put(msgBytes);
			byte[] data = buffer.array();
			Packet reponse = new Packet(0, userId, data);
			server.getUser(userId).process(reponse); //getUser() in ServerMsg
		} else {
			LOG.info("from ServerPacketProcessor, login() : Authentication failed for user " + username);

			// Send a message to the user to inform that the authentication failed
			String msg = "from ServerPacketProcessor, login() : Authentication failed";
			byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
			int length = msg.getBytes().length;
			ByteBuffer buffer = ByteBuffer.allocate(4 + length);
			buffer.putInt(length);
			buffer.put(msgBytes);
			byte[] data = buffer.array();
			Packet reponse = new Packet(0, userId, data);
			server.getUser(userId).process(reponse);
		}
	}
}

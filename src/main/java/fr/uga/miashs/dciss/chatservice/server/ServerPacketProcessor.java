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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
	public void process(Packet p) throws IOException {
		// ByteBufferVersion. On aurait pu utiliser un ByteArrayInputStream + DataInputStream à la place
		ByteBuffer buf = ByteBuffer.wrap(p.data);
		byte type = buf.get();

		if (type == 1) { // cas creation de groupe
			createGroup(p.srcId, buf);
		} else if (type == 2) { //cas suppression de groupe
			removeGroup(p);
		} else if (type == 3) { // cas ajout de membre dans un groupe
			int groupId = buf.getInt(); // ID du groupe
			int userId = buf.getInt(); // ID de l'utilisateur
			addMember(p.srcId, groupId, userId);
		}
		else if (type == 4) { // cas suppression de membre dans un groupe
			removeMember(p.srcId, buf);
		}
		
		//dans le cas où le type n'est pas déterminé
		else {
			LOG.warning("Server message of type=" + type + " not handled by procesor");
		}
	}

	public void createGroup(int ownerId, ByteBuffer data) throws IOException {
		int nb = data.getInt();
		GroupMsg g = server.createGroup(ownerId);
		for (int i = 0; i < nb; i++) {
			g.addMember(server.getUser(data.getInt()));
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeByte(1); // Byte pour le type de réponse
		dos.writeInt(g.getId()); // ID du groupe
		dos.flush();
		byte[] groupCreatedResponse = baos.toByteArray();

		// Envoyez le paquet de réponse au client
		Packet responsePacket = new Packet(ServerMsg.SERVER_CLIENTID, ownerId, groupCreatedResponse);
		UserMsg owner = server.getUser(ownerId);
		if (owner != null) {
			owner.process(responsePacket);
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
			/* TODO : informer le sender que le groupe n'existe pas */
			LOG.info("le groupe " + groupId + " n'existe pas");
		} else { //le groupe existe
			// vérifier que le sender est le owner
			if (src != groupe.getOwner().getId()) {
				//TODO : informer le sender qu'il n'a pas les droits pour supprimer les groupes
				LOG.info("le sender " + src + " n'est pas le propriétaire du groupe " + groupId);
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
			} else {
				LOG.warning("Attempt to remove non-existent user " + userId + " from group " + groupId);
			}
		} else {
			LOG.warning("Group " + groupId + " not found");
		}
	}
}

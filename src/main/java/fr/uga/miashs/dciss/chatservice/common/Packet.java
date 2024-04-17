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

package fr.uga.miashs.dciss.chatservice.common;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import javax.imageio.ImageIO;

/*
 * Data structure to represent a packet
 */
public class Packet {

	public final int srcId;
	public final int destId;
	public final byte[] data;
	public final byte[] imageData; // New field to store image data

	public Packet(int srcId, int destId, byte[] data) {
		super();
		this.srcId = srcId;
		this.destId = destId;
		this.data = data;
		this.imageData = null;
	}

	// New constructor to initialize image data
	public Packet(int srcId, int destId, byte[] data, BufferedImage image) throws IOException {
		super();
		this.srcId = srcId;
		this.destId = destId;
		this.data = data;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", baos);
		this.imageData = baos.toByteArray();
	}

	// 	Method to convert the packet to a byte array (needed for ClientMsg)
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(this);
		return baos.toByteArray();
	}

	public byte getType() {
		return data[0];
	}
	
	
}

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
import java.io.*;
import javax.imageio.ImageIO;

/*
 * Data structure to represent a packet
 */
public class Packet implements Serializable {

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

	// 	Method to convert the packet to a byte array (needed to send an image)
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(this);
		return baos.toByteArray();
	}

	// 	Method to convert a byte array to a packet (needed to receive an image)
	public BufferedImage getImage() throws IOException {
		if (imageData == null) {
			return null;
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
		return ImageIO.read(bais);
	}

	public byte getType() {
		return data[0];
	}
	
	
}

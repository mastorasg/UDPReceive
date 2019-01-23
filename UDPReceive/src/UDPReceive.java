import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Enumeration;

public class UDPReceive {
	private static final String FILENAME = "interface.svg";
	private static final String WORKFILENAME = "new.svg";

	/**
	 * Returns the local ip
	 * 
	 * @return the local ip
	 */
	public static String WhatIsMyIp() {
		String ip;
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				// filters out 127.0.0.1 and inactive interfaces like Virtual Machines
				if (iface.isLoopback() || !iface.isUp() || iface.getDisplayName().contains("VMware")
						|| iface.getDisplayName().contains("Virtual"))
					continue;

				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					ip = addr.getHostAddress();
					if (!ip.contains(":")) { // if not ipv6
						return (ip);
					}
				}
			}
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
		return ("127.0.0.1");
	}

	private static void copyFile(File source, File dest) throws IOException {
	    Files.copy(source.toPath(), dest.toPath());
	}
	
	/**
	 * Deletes closing tag </svg>
	 * 
	 */
	public static void initializeSVGFile() {
		try {
			File file = new File(FILENAME);
			File temp = new File(WORKFILENAME);
			temp.delete();
			copyFile(file, temp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deletes closing tag </svg>
	 * 
	 */
	public static void deleteClosingSvgTag() {
		try {
			File file = new File(WORKFILENAME);
			File temp = File.createTempFile("temp", ".svg", file.getParentFile());
			String charset = "UTF-8";
			String delete = "</svg>";

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(temp), charset));

			for (String line; (line = reader.readLine()) != null;) {
				line = line.replace(delete, "");
				writer.println(line);
			}
			reader.close();
			writer.close();
			file.delete();
			temp.renameTo(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Append new <svg> circle indicating the coordinates on map
	 * 
	 */
	public static void showPositionOnMap(int x, int y) {
		BufferedWriter bw = null;
		FileWriter fw = null;
		deleteClosingSvgTag();

		/* Append new <svg> objects */
		try {
			fw = new FileWriter(WORKFILENAME, true);
			bw = new BufferedWriter(fw);

			String rec_start = "<circle cx=\"" + x + "\" cy=\"" + y
					+ "\" r=\"5\" stroke=\"black\" stroke-width=\"1\" fill=\"red\"/>\n";
			bw.write(rec_start);
			String endsvg = "</svg>";
			bw.write(endsvg);
			try {
				Desktop desktop = null;
				if (Desktop.isDesktopSupported()) {
					desktop = Desktop.getDesktop();
				}
				desktop.open(new File(WORKFILENAME));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {
		try {
			int port = 5550;
			System.out.println("On ip: " + WhatIsMyIp() + " port " + port);
			initializeSVGFile();

			// Create a socket to listen on the port.
			DatagramSocket dsocket = new DatagramSocket(port);

			// Create a buffer to read datagrams into. If a
			// packet is larger than this buffer, the
			// excess will simply be discarded!
			byte[] buffer = new byte[2048];

			// Create a packet to receive data into the buffer
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			// Now loop forever, waiting to receive packets and printing them.
			while (true) {
				// Wait to receive a datagram
				dsocket.receive(packet);

				// Convert the contents to a string, and display them
				String msg = new String(buffer, 0, packet.getLength());
				if (msg.contains(",")) {
					int p = msg.indexOf(",");
					int a = Integer.parseInt(msg.substring(0, p).trim());
					int b = Integer.parseInt(msg.substring(p+1, msg.length()).trim());
					System.out.println(String.valueOf(a) + ", " + String.valueOf(b));
					showPositionOnMap(a, b);
				}

				// Reset the length of the packet before reusing it.
				packet.setLength(buffer.length);
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}
package org.ziggrid.utils.serialization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.exceptions.UtilException;

@SuppressWarnings("serial")
public class Endpoint implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger("Endpoint");
	private final String host;
	private final int port;

	public static Endpoint forPort(int port)
	{
		return new Endpoint(getLocalHostAddr(), port);
	}
	
	public Endpoint(InetAddress addr, int port) {
		String host = addr.getHostAddress();
		if (host.equals("0.0.0.0"))
			host = getLocalHostAddr();
		this.host = host;
		this.port = port;
	}

	public Endpoint(ServerSocket s) {
		this(s.getInetAddress(), s.getLocalPort());
	}
	
	public Endpoint(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
	
	private enum Type { DEFAULT, LOOPBACK, LINKLOCAL, N192, N172, N10, GLOBAL };

	/** Try and determine the machine's IP address.  There's no guarantee that this is local,
	 * and if this method does not find the most appropriate one, use -Dorg.ziniki.claim.endpoint=
	 * to specify your preferred address.
	 * 
	 * This method returns a Global address if it can, otherwise it returns the most global site local address it can find; if there
	 * are none of these, it returns a link local address or the loopback address.
	 * 
	 * In the worst of all cases, this method throws an exception.
	 * 
	 * @return the best IP address we can find
	 */
	public static String getLocalHostAddr() {
		try {
			String host = System.getProperty("org.ziniki.claim.endpoint");
			if (host == null || host.length() == 0) {
				ListMap<Type,InetAddress> map = new ListMap<Type,InetAddress>();
				InetAddress in = InetAddress.getLocalHost();
				logger.debug("Considering addresses for host " + in.getHostName() + ": " + in.getHostAddress());
				InetAddress[] all = InetAddress.getAllByName(in.getHostName());
				for (int i=0;i<all.length;i++)
				{
					logger.debug(MarkerFactory.getMarker("finer"), "resolving local address:" + all[i] + " " + all[i].isSiteLocalAddress() + " " + all[i].isLoopbackAddress());
					InetAddress ai = all[i];
					byte[] addr = ai.getAddress();
					logger.debug("resolving local address:" + ai + " ipv4:" + (addr.length == 4) + " site:" + ai.isSiteLocalAddress() + " link:" + ai.isLinkLocalAddress() + " lo:" + ai.isLoopbackAddress());
					if (ai.isMulticastAddress() || addr.length != 4) {
						logger.debug("Ignoring multicast or IPv6 address:" + ai);
						continue;
					}
					Type type;
					if (ai.isLoopbackAddress())
						type = Type.LOOPBACK;
					else if (ai.isLinkLocalAddress())
						type = Type.LINKLOCAL;
					else if (addr[0] == 10)
						type = Type.N10;
					else if (addr[0] == 172 && (addr[1]&0xf0) == 1)
						type = Type.N172;
					else if (addr[0] == 192 && (addr[1] == 168))
						type = Type.N192;
					else
						 type = Type.GLOBAL;
					map.add(type, ai);
				}
				for (Type t : new Type[] { Type.GLOBAL, Type.N10, Type.N172, Type.N192, Type.LINKLOCAL, Type.LOOPBACK, Type.DEFAULT })
					if (map.contains(t) && map.get(t).size() > 0) {
						host = map.get(t).get(0).getHostAddress();
						break;
					}
				if (host == null)
					throw new UtilException("Could not find any host address");
			}
			logger.debug("Identifying local host as " + host);
			return host;
		} catch (UnknownHostException e) {
			throw UtilException.wrap(e);
		}
	}

	public static Endpoint parse(String s) {
		try {
			int idx = s.indexOf(":");
			if (idx < 1 || idx > s.length()-2)
				throw new UtilException("Invalid address: " + s);
			String address = s.substring(0, idx);
			InetAddress addr = InetAddress.getByName(address);
			int port = Integer.parseInt(s.substring(idx+1));
			if (port < 0 || port > 65535)
				throw new UtilException("Invalid port: " + port);
			return new Endpoint(addr, port);
		} catch (Exception e) {
			System.out.println("Failed to interpret address: " + s);
			if (e instanceof UtilException)
				System.out.println("  " + e.getMessage());
			else
				System.out.println("  " + e.toString());
			return null;
		}
	}

	@Override
	public String toString() {
		return host+":"+port;
	}

	public void checkExists() {
		try
		{
			Socket socket = new Socket(host, port);
			socket.close();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public EndpointConnection open() {
		try
		{
			Socket socket = new Socket(host, port);
			return new EndpointConnection(socket); 
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public Object tell(ControlRequest msg) {
		EndpointConnection open = open();
		open.send(msg);

		try
		{
			if (!msg.waitForResponse())
			{
				Thread.sleep(10);
				return null;
			}
			ObjectInputStream ois = open.getOIS();
			return ois.readObject();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
		finally
		{
			open.close();
		}
	}

	public void send(String string) {
		try {
			Socket conn = new Socket(host, port);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			bw.write(string);
			bw.flush();
			conn.close();
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Endpoint))
			return false;
		Endpoint other = (Endpoint) obj;
		return other.host.equals(host) && other.port == port;
	}
	
	@Override
	public int hashCode() {
		return host.hashCode() ^ port;
	}
	
	public static void main(String[] args) throws SecurityException, IOException {
		getLocalHostAddr();
	}
}

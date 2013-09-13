package org.ziggrid.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;

import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.http.RemoteIO.Connection;
import org.ziggrid.utils.utils.FileUtils;

public class ConnectionThread extends Thread {
	private final InputStream is;
	private final OutputStream os;
	private final InlineServer inlineServer;
	private boolean closeConnection;
	private boolean isWebSocket;
	private final Connection conn;

	public ConnectionThread(InlineServer inlineServer, Connection conn) throws IOException {
		super("InlineServer.Connection");
		this.inlineServer = inlineServer;
		this.conn = conn;
		this.is = conn.getInputStream();
		this.os = conn.getOutputStream();
	}
	
	@Override
	public void run()
	{
		GPResponse response = null;
		closeConnection = true;
		isWebSocket = false;
		InlineServer.logger.debug(Thread.currentThread().getName()+ ": " + "Processing Incoming Request");
		Date now = new Date();
		try {
			boolean keptAlive = false;
			for (;;) {
				try
				{
					response = handleOneRequest(keptAlive);
					if (response == null)
						closeConnection = true;
					else if (response.getHeader("Content-Length") != null && response.getHeader("Content-Length").equals("-1"))
						closeConnection = true;
					if (closeConnection)
						break;
					keptAlive = true;
				}
				finally
				{
					if (response != null)
					{
						if (response.getStatus() == 0 && closeConnection)
							response.setStatus(200, "OK");
						response.commit();
						try
						{
							response.getWriter().flush();
							conn.doneSending();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			InlineServer.logger.error("Uncaught exception processing request", e);
			try {
				conn.errorSending();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		finally
		{
			InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Closing Connection");
			inlineServer.requestTime(now, new Date());
			try
			{
				is.close();
				os.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private GPResponse handleOneRequest(boolean keptAlive) throws Exception {
		GPResponse response;
		String s;
		GPRequest request = null;
		while ((s = readLine()) != null && s.trim().length() > 0)
		{
			InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Header - " + s);
			if (request == null)
				request = inlineServer.requestFor(s, is);
			else
				request.addHeader(s);
		}
		if (request == null)
		{
			return null;
//			if (keptAlive)
//				return null;
//			throw new UtilException(Thread.currentThread().getName()+ ": " + "There was no incoming request");
		}
		InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Done Headers");
		request.endHeaders();
		InlineServer.logger.info("Handling request for " + request.getRequestURI());
		
		String connhdr;
		{
			connhdr = request.getHeader("connection");
			if (connhdr == null)
				connhdr = "close";
			else if (connhdr.equalsIgnoreCase("keep-alive"))
				closeConnection = false;
			else if (connhdr.equalsIgnoreCase("upgrade"))
				closeConnection = false;
			else if (connhdr.equalsIgnoreCase("close"))
				closeConnection = true;
		}
		
		response = new GPResponse(request, os, connhdr);
		if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
			response.setStatus(200);
			request.getInputStream().flush();
			return response;
		}
		
		{
			String upghdr = request.getHeader("upgrade");
			if (upghdr != null && (upghdr.equalsIgnoreCase("websocket")))
			{
				isWebSocket = true;
				response.setWebSocket(true);
			}
		}
		
		HttpServlet servlet = request.getServlet();
		InlineServer.logger.debug("Request URI: " + request.getRequestURI() + " - " + servlet);
		if (servlet != null)
		{
			InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Handling through servlet");
			servlet.service(request, response);
			InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Finished servlet handling");
			if (isWebSocket)
			{
				if (response.getStatus() == 0)
					response.setStatus(101, "Web Socket Protocol Handshake");
				response.commit();
				try
				{
					response.getWriter().flush();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				dealWithWebsocket(request, response);
				return null;
			}
		}
		else if (request.getMethod().equalsIgnoreCase("GET")) {
			GPStaticResource staticResource = request.getStaticResource();
			String pathInfo = request.getPathInfo();
			if (staticResource != null)
			{
				InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Found static resource");
				response.setStatus(200);
				response.setContentLength((int)staticResource.len);
				guessContentType(response, pathInfo);
				FileUtils.copyStream(staticResource.stream, response.getOutputStream());
				staticResource.close();
			}
			else
			{
				InlineServer.logger.info(Thread.currentThread().getName()+ ": " +"404: Not found - " + pathInfo);
				response.setStatus(404);
			}
		}
		request.getInputStream().flush();
		return response;
	}

	public void guessContentType(GPResponse response, String pathInfo) {
		if (pathInfo.endsWith(".html"))
			response.setContentType("text/html");
		else if (pathInfo.endsWith(".js"))
			response.setContentType("application/javascript");
		else if (pathInfo.endsWith(".css"))
			response.setContentType("text/css");
	}

	private void dealWithWebsocket(GPRequest request, GPResponse response) {
		if (request.wshandler == null)
			throw new UtilException("No websocket handler has been set on InlineServer");
		try {
			ServletInputStream is = request.getInputStream();
			request.wshandler.onOpen(response);
			for (;;)
			{
				GPFrame frame = readFrame(is);
				if (frame == null)
				{
					request.wshandler.onClose(1000);
					break;
				}
				InlineServer.logger.info("Read " + frame + " telling listener");
				if (frame.opcode == 0x1)
					request.wshandler.onTextMessage(new String(frame.data));
				else if (frame.opcode == 0x2)
					request.wshandler.onBinaryMessage(frame.data);
				else if (frame.opcode == 0x8)
				{
					request.wshandler.onClose((frame.data[0]&0xff)<<8|(frame.data[1]&0xff));
					break;
				}
				else
					throw new UtilException("Can't handle " + frame.opcode);
			}
			is.close();
			InlineServer.logger.info(Thread.currentThread().getName()+ ": " + "End of async stream");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private GPFrame readFrame(ServletInputStream is) {
		try
		{
			int b1 = is.read();
			if (b1 == -1)
				return null;
			@SuppressWarnings("unused") // is this the final frame?
			boolean fin = (b1&0x80) != 0;
			int rsv = (b1&0x70);
			if (rsv != 0)
				throw new UtilException("RSV must be 0");
			int opcode = b1&0xf;
			int b2 = is.read();
			boolean isMasked = (b2&0x80) != 0;
			if (!isMasked)
				throw new UtilException("Client must mask frames");
			long len = (b2&0x7f);
			if (len == 126) {
				int b3 = is.read() & 0xff;
				int b4 = is.read() & 0xff;
				len = (b3 << 8) | b4;
			} else if (len == 127) {
				len = 0;
				for (int i=0;i<8;i++) {
					int bl = is.read() & 0xff;
					len = (len << 8) | bl;
				}
			}
			byte[] mask = new byte[4];
			is.read(mask);
			
			byte[] data = new byte[(int)len];
			is.read(data);
			
			for (int i=0;i<data.length;i++)
			{
				data[i] ^= mask[i%4];
			}
			
			if (opcode == 0x8)
				return null;
			return new GPFrame(opcode, data);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			 return null;
		}
	}

	private String readLine() throws IOException {
		StringBuilder sb = new StringBuilder();
		int b;
		while ((b = is.read()) != -1 && b != '\n') {
			sb.append((char)b);
		}
		for (int i=sb.length();i>0;i--)
		{
			if (sb.charAt(i-1) != '\r' && sb.charAt(i-1) != '\n')
				break;
			sb.deleteCharAt(i-1);
		}
		return sb.toString();
	}
}

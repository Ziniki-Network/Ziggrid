package org.ziggrid.utils.http;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.exceptions.UtilException;

public class GPResponse implements HttpServletResponse {

	private int status;
	private String statusMsg;
	private final ListMap<String, String> headers = new ListMap<String, String>();
	private final ServletOutputStream sos;
	private PrintWriter pw;
	private boolean committed;
	private SimpleDateFormat dateFormat;
	private final String connectionState;
	private String encoding = "iso-8859-1";
	private boolean isWebSocket;

	public GPResponse(GPRequest request, OutputStream os, String connhdr) {
		this.connectionState = connhdr;
		request.setResponse(this);
		sos = new GPServletOutputStream(os);
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public String status() {
		return "HTTP/1.1 " + status + " " + statusMsg;
	}

	@Override
	public void flushBuffer() throws IOException {
		sos.flush();
	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String getCharacterEncoding() {
		return encoding;
	}

	@Override
	public String getContentType() {
		return getHeader("Content-Type");
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (!headers.contains("Content-Length"))
			setContentLength(-1);
		commit();
		return sos;
	}

	private void reply(List<String> sendHeaders) {
		for (String s : sendHeaders)
			reply(s);
	}

	private void reply(String string) {
		InlineServer.logger.debug(Thread.currentThread().getName()+ ": Response: " + string);
		pw.print(string +"\r\n");
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (pw == null) {
			commit();
		}
		return pw;
	}

	@Override
	public boolean isCommitted() {
		return committed;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setCharacterEncoding(String arg0) {
		encoding = arg0;
	}

	@Override
	public void setContentLength(int arg0) {
		addIntHeader("Content-Length", arg0);
	}

	@Override
	public void setContentType(String arg0) {
		addHeader("Content-Type", arg0);
	}

	@Override
	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void addCookie(Cookie arg0) {
		addHeader("Set-Cookie", arg0.toString());
	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
		addHeader(arg0, Long.toString(arg1));
	}

	@Override
	public void addHeader(String arg0, String arg1) {
		headers.add(arg0, arg1);
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		addHeader(arg0, Integer.toString(arg1));
	}

	@Override
	public boolean containsHeader(String arg0) {
		return headers.contains(arg0);
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String encodeURL(String arg0) {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String encodeUrl(String arg0) {
		throw new UtilException("Not Implemented");
	}

	@Override
	public void sendError(int arg0) throws IOException {
		setStatus(arg0);
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {
		setStatus(arg0, arg1);
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		setHeader(arg0, Long.toString(arg1));
	}

	@Override
	public void setHeader(String arg0, String arg1) {
		headers.removeAll(arg0);
		headers.add(arg0, arg1);
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		setHeader(arg0, Integer.toString(arg1));
	}

	@Override
	public void setStatus(int arg0) {
		String message;
		if (arg0 >= 500 && arg0 < 600)
			message = "Internal Server Error";
		else if (arg0 == 404)
			message = "Not Found";
		else if (arg0 >= 400)
			message = "Denied";
		else if (arg0 >= 300) {
			if (arg0 == 301)
				message = "Moved Permanently";
			else if (arg0 == 302)
				message = "Found";
			else if (arg0 == 303)
				message = "See Other";
			else
				message = "Moved";
		}
		else if (arg0 >= 200) {
			if (arg0 == 204)
				message = "No Content";
			else
				message = "OK";
		} else
			message = null;
		setStatus(arg0, message);
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		status = arg0;
		statusMsg = arg1;
		InlineServer.logger.debug("Setting status to " + status());
	}

	private List<String> sendHeaders() {
		List<String> ret = new ArrayList<String>();
		for (String s : headers.keySet()) {
			addHeader(ret, s);
		}
		return ret;
	}

	private List<String> sendHeaders(String s) {
		List<String> ret = new ArrayList<String>();
		addHeader(ret, s);
		return ret;
	}

	private void addHeader(List<String> ret, String s) {
		if (s.equals("Set-Cookie"))
		{
			for (String v : headers.get(s))
			{
				StringBuilder buf = new StringBuilder();
				buf.append(s);
				buf.append(": ");
				buf.append(v);
				ret.add(buf.toString());
			}
		}
		else
		{
			StringBuilder buf = new StringBuilder();
			buf.append(s);
			String sep = ": ";
			for (String v : headers.get(s))
			{
				buf.append(sep);
				buf.append(v);
				sep = ", ";
			}
			ret.add(buf.toString());
		}
	}

	public void commit() {
		try {
			if (!committed)
			{
				pw = new PrintWriter(new OutputStreamWriter(sos, encoding));
				InlineServer.logger.trace(Thread.currentThread().getName() + " Writing to " + sos);
				if (status == 0)
					setStatus(200, "OK");
				if (status >= 300)
					InlineServer.logger.debug("Servlet Request returned " + status());
				reply(status());
				if (headers.contains("Upgrade"))
					reply(sendHeaders("Upgrade"));
				if (headers.contains("Connection"))
					reply(sendHeaders("Connection"));
				else
					reply("Connection: " + connectionState);
				
				reply("Server: InlineServer/1.1");
				reply("Date: " + dateFormat.format(new Date())); /* Sat, 18 Jun 2011 21:52:27 GMT */
				// TODO: this should be an option, set on the InlineServer, to which we should have a pointer
				// The option should include the option to specify a list of servers.
				reply("Access-Control-Allow-Origin: *");
				reply("Access-Control-Allow-Methods: POST, GET, PUT, DELETE, OPTIONS");
				reply("Access-Control-Allow-Headers: Content-Type, X-Ziniki-Token, X-Cache-Date"); 
				reply("Access-Control-Expose-Headers: X-Ziniki-Token"); 
				for (String r : sendHeaders())
					if (!r.toLowerCase().startsWith("upgrade") && !r.toLowerCase().startsWith("connection"))
						reply(r);
				if (!isWebSocket && !headers.contains("Content-Length"))
					reply("Content-Length: 0");
				reply("");
				pw.flush();
				committed = true;
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getStatus() {
		return status;
	}

	@Override
	public String getHeader(String arg0) {
		if (!headers.contains(arg0))
			return null;
		return headers.get(arg0).get(0);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headers.keySet();
	}

	@Override
	public Collection<String> getHeaders(String arg0) {
		if (!headers.contains(arg0))
			return null;
		return headers.get(arg0);
	}

	public void setWebSocket(boolean b) {
		isWebSocket = b;
	}

	public void writeTextMessage(String data) {
		byte[] bytes = data.getBytes();
		write(0x1, bytes, 0, bytes.length);
	}

	public void writeBinaryMessage(byte[] data, int offset, int length) {
		write(0x2, data, offset, length);
	}
	
	public void writeClose(int code, String reason) {
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(code);
			dos.writeChars(reason);
			byte[] bytes = baos.toByteArray();
			write(0x8, bytes, 0, bytes.length);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	private void write(int opcode, byte[] data, int offset, int length) {
		try {
			sos.write(0x80|opcode);
			if (length < 126)
				sos.write(length);
			else if (length < 65536)
			{
				sos.write(126);
				sos.write((length>>8)&0xff);
				sos.write(length&0xff);
			}
			else
				throw new UtilException("Message too long");
			sos.write(data, offset, length);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void flush() {
		try {
			sos.flush();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void close() {
		try {
			sos.close();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
}

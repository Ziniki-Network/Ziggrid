package org.ziggrid.utils.http.ws;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AsynchronousProcessor;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.websocket.WebSocket;

import sun.misc.BASE64Encoder;

import org.ziggrid.utils.http.GPRequest;
import org.ziggrid.utils.utils.Crypto;

public class AsyncProcessor extends AsynchronousProcessor {

	public AsyncProcessor(AtmosphereConfig config) {
		super(config);
	}
	
	@Override
	public boolean supportWebSocket() {
		return true;
	}

	@Override
	public Action service(AtmosphereRequest req, AtmosphereResponse resp) throws IOException, ServletException {
		boolean suspend = false;
		
		if (req.getAttribute(WebSocket.WEBSOCKET_SUSPEND) != null)
			suspend = true;
		else if (!haveHeader(req, "Upgrade", "websocket"))
			suspend = true;
		else if (!haveHeader(req, "Connection", "upgrade"))
			suspend = true;
		else if (!haveHeader(req, "sec-websocket-version", "13")) {
			resp.sendError(501, "Websocket protocol not supported");
			return new Action(Action.TYPE.CANCELLED);
		}
		if (suspend)
		{
			Action ret = suspended(req, resp);
			if (ret.type() == Action.TYPE.RESUME)
				req.setAttribute(WebSocket.WEBSOCKET_RESUME, true);
			return ret;
		}
		
		String key = req.getHeader("Sec-WebSocket-Key");
		if (key == null) {
			resp.sendError(501, "No security key provided");
			return new Action(Action.TYPE.CANCELLED);
		}
		
        resp.setHeader("Upgrade", "websocket");
        resp.setHeader("Connection", "upgrade");
        resp.setHeader("Sec-WebSocket-Accept", new BASE64Encoder().encode(Crypto.computeHash(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")));
        
        unwrap(req).setWebSocketHandler(new AtmosphereHandler(req, resp, config.framework(), config.framework().getWebSocketProtocol()));
        return new Action(Action.TYPE.SUSPEND);

	}

	private GPRequest unwrap(HttpServletRequest req) {
        while (req instanceof HttpServletRequestWrapper)
            req = (HttpServletRequest) ((HttpServletRequestWrapper) req).getRequest();
        return (GPRequest) req;
	}

	private static boolean haveHeader(AtmosphereRequest req, String headerName, String target) {
    	@SuppressWarnings("unchecked")
		Enumeration<String> headers = req.getHeaders(headerName);
		while (headers.hasMoreElements()) {
			String header = headers.nextElement();
			String[] tokens = header.split(",");
			for (String token : tokens) {
				if (target.equalsIgnoreCase(token.trim())) {
					return true;
				}
			}
		}
		return false;
	}
}

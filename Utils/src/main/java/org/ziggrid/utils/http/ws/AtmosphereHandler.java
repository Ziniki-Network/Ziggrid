package org.ziggrid.utils.http.ws;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.websocket.DefaultWebSocketProcessor;
import org.atmosphere.websocket.WebSocketEventListener;
import org.atmosphere.websocket.WebSocketProtocol;

import org.ziggrid.utils.http.GPResponse;

public class AtmosphereHandler implements InlineServerWebSocketHandler {
	private final Logger logger = LoggerFactory.getLogger("AtmosphereHandler");
	private final AtmosphereRequest request;
	private final AtmosphereFramework framework;
	private DefaultWebSocketProcessor webSocketProcessor;
	private WSWriter webSocket;
	private final AtmosphereResponse response;

	public AtmosphereHandler(AtmosphereRequest request, AtmosphereResponse response, AtmosphereFramework framework, WebSocketProtocol webSocketProtocol) {
		this.request = request;
		this.response = response;
		this.framework = framework;
	}
	
	@Override
	public void onOpen(GPResponse gpresp) {
        try {
        	webSocket = new WSWriter(gpresp, framework.getAtmosphereConfig());
        	response.asyncIOWriter(webSocket);
        	webSocketProcessor = new DefaultWebSocketProcessor(framework);
        	webSocketProcessor.open(webSocket, request, response);
//        	webSocketProcessor.dispatch(webSocket, request, response);
        	webSocketProcessor.notifyListener(webSocket, new WebSocketEventListener.WebSocketEvent<String>("", WebSocketEventListener.WebSocketEvent.TYPE.CONNECT, webSocket));
        } catch (Exception e) {
        	logger.warn("failed to connect to web socket", e);
        }
	}

	@Override
	public void onBinaryMessage(byte[] message) throws IOException {
        try {
        	webSocketProcessor.invokeWebSocketProtocol(webSocket, message, 0, message.length);
            webSocketProcessor.notifyListener(webSocket, new WebSocketEventListener.WebSocketEvent<String>(new String(message, "UTF-8"), WebSocketEventListener.WebSocketEvent.TYPE.MESSAGE, webSocket));
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException", e);
        }
	}

	@Override
	public void onTextMessage(String string) throws IOException {
        webSocketProcessor.invokeWebSocketProtocol(webSocket, string);
        webSocketProcessor.notifyListener(webSocket, new WebSocketEventListener.WebSocketEvent<String>(string, WebSocketEventListener.WebSocketEvent.TYPE.MESSAGE, webSocket));
	}

	@Override
	public void onClose(int closeCode) {
        request.destroy();
        if (webSocketProcessor == null) return;

        webSocketProcessor.notifyListener(webSocket, new WebSocketEventListener.WebSocketEvent<String>("", WebSocketEventListener.WebSocketEvent.TYPE.CLOSE, webSocket));
        webSocketProcessor.close(webSocket, closeCode);
	}

}

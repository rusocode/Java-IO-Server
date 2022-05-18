package com.silent.server;

import java.io.Serializable;
import java.util.*;

/**
 * Serializa el paquete para poder enviarlo a travez de la red como una secuencia de bytes.
 *
 * @author Ruso
 */

public class Packet implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean connected;
	private String nickOrig, nickDest, ipOrig, ipDest, port, message;
	private Map<String, String> connections;

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public String getNickOrig() {
		return nickOrig;
	}

	public void setNickOrig(String nickOrig) {
		this.nickOrig = nickOrig;
	}

	public String getNickDest() {
		return nickDest;
	}

	public void setNickDest(String nickDest) {
		this.nickDest = nickDest;
	}

	public String getIpOrig() {
		return ipOrig;
	}

	public void setIpOrig(String ipOrig) {
		this.ipOrig = ipOrig;
	}

	public String getIpDest() {
		return ipDest;
	}

	public void setIpDest(String ipDest) {
		this.ipDest = ipDest;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public Map<String, String> getConnections() {
		return connections;
	}

	public void setConnections(Map<String, String> connections) {
		this.connections = connections;
	}

}

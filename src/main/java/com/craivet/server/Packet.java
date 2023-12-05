package com.craivet.server;

import java.io.Serializable;
import java.util.*;

import lombok.Getter;
import lombok.Setter;

/**
 * Serializa el paquete para poder enviarlo a travez de la red como una secuencia de bytes.
 *
 * @author Ruso
 */

@Getter
@Setter
public class Packet implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean connected;
	private String nickOrig, nickDest, ipOrig, ipDest, port, message;
	private Map<String, String> connections;

}

package com.silent.server;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import java.awt.HeadlessException;

import net.miginfocom.swing.MigLayout;

import static com.silent.util.Constants.*;

/**
 * Clase encargada de gestionar las conexiones.
 * <p>
 * Es importante aclarar que el servidor hace broadcast cuando un cliente se conecta o desconecta, no cuando envia
 * mensajes, sino el combo para elegir clientes seria innecesario. Por lo tanto, el envio de mensajes es unicast.
 * <p>
 * Se podria agregar un buffer para el servidor, pero por cuestiones de que esto es solamente un experimento lo dejo
 * asi. Para este caso, es mucho mas facil agregar un buffer usando NIO.
 * <p>
 * En la clase Estados.java, el termino pausa es romper y supender seria como una pausa.
 * <p>
 * <a href="https://es.wikipedia.org/wiki/Difusi%C3%B3n_amplia">...</a>
 * https://masteringnetworks.com/que-es-el-broadcast-en-redes/
 * <a href="https://aleph.org.mx/por-que-se-dice-que-el-internet-es-bidireccional#:~:text=Podemos%20definir%20la%20comunicaci%C3%B3n%20bidireccional,una%20conversaci%C3%B3n%20en%20ambas%20direcciones">...</a>.
 * <a href="https://es.sawakinome.com/articles/words/difference-between-pause-and-stop.html">...</a>
 * <p>
 * TODO Es necesaria la sincronizacion en el metodo stop y isStopped?
 * TODO Agregar un switch con los tres tipos de direccion de destino: unicast, multicas y broadcast.
 * TODO El servidor se pausa (stop) o se detiene?
 * TODO Comentar en pildoras
 * TODO Agregar la ip local del server en el titulo de la ventana
 * TODO Aplicar MVC en un futuro...
 * <p>
 * <a href="https://stackoverflow.com/questions/56112598/proper-way-to-close-an-autocloseable">...</a>
 *
 * @author Ruso
 */

public class Server extends JFrame implements Runnable {

	private static final long serialVersionUID = 1L;

	private JTextArea console;
	private JButton btnStop;
	private JButton btnOpen;

	// Socket del servidor
	private ServerSocket server;
	// Conexion de entrada y salida
	private Socket socketIn, socketOut;
	// Flujo de salida
	private ObjectOutputStream streamOut;
	// Paquete del flujo
	private Packet packet;
	// Coleccion para guardar las conexiones (nick/ip)
	private final Map<String, String> connections = new HashMap<>();
	private boolean suspendido;

	public Server() throws HeadlessException, UnknownHostException {

		super("Server #" + InetAddress.getLocalHost().getHostAddress());
		setResizable(false);
		setSize(402, 439);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initialize();

	}

	private void initialize() {

		// GUI
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("fill", "", "[grow][]")); // columna/fila

		console = new JTextArea();
		console.setEditable(false);
		JScrollPane scroll = new JScrollPane(console);
		panel.add(scroll, "spanx, grow, wrap");

		btnStop = new JButton("Stop server");
		btnStop.setFocusable(false);
		btnStop.addActionListener(evt -> suspender());
		panel.add(btnStop, "split 2");

		btnOpen = new JButton("Open server");
		btnOpen.setEnabled(false);
		btnOpen.setFocusable(false);
		btnOpen.addActionListener(evt -> reOpen());
		panel.add(btnOpen);

		JButton btnClean = new JButton("Clean");
		btnClean.setFocusable(false);
		btnClean.addActionListener(evt -> console.setText(null));
		panel.add(btnClean, "alignx right");

		getContentPane().add(panel);

		setLocationRelativeTo(null);

	}

	@Override
	public void run() {

		openServer();

		console.append("Waiting for a connection on the port " + SERVER_PORT + "...\n");

		// Mientras el servidor no este detenido...
		while (!isStopped()) {

			console.setCaretPosition(console.getDocument().getLength()); // TODO Hace falta poner esto aca?

			listenRequest();

			processRequest(); // TODO hand request to worker thread

		}

	}

	/**
	 * Abre el servidor.
	 */
	private void openServer() {

		try {

			/* Crea un servidor abriendo el puerto especificado, es decir que crea como un "enchufe" (socket en ingles) para recibir
			 * conexiones. */
			server = new ServerSocket(SERVER_PORT);
			suspendido = false;

		} catch (IOException e) {
			Info.showServerMessage(e.toString());
		} catch (IllegalArgumentException e) {
			Info.showServerMessage(
					"The port is outside the specified range of valid port values, which is between 0 and 65535, inclusive\n" + e);
			System.exit(0);
		}

	}

	/**
	 * Acepta una conexion.
	 */
	private void listenRequest() {

		try {
			/* Bloquea el servidor hasta que se establezca una conexion, es decir que hasta que no se acepte una conexion, el flujo
			 * del programa no se ejecuta. */
			socketIn = server.accept(); // CADA CONEXION ENTRANTE ES MANEJADA POR UN NUEVO HILO (ineficiencia)

		} catch (IOException e) {
			/* ¿Por que cuando detengo el sv sigue buscando conexiones? Porque el punto del programa cuando se detiene el sv,
			 * esta en la linea de "server.accept()". */
			if (isStopped()) console.append("Server stopped!\n");
		}

	}

	/**
	 * Procesa la conexion.
	 */
	private void processRequest() {

		// Pregunta si el servidor no esta detenido, ya que el socketIn seria null
		if (!isStopped()) {

			try {

				// Crea un flujo de entrada para recibir paquetes de la conexion
				ObjectInputStream streamIn = new ObjectInputStream(socketIn.getInputStream());

				// Deserializa del flujo el paquete
				packet = (Packet) streamIn.readObject();

				/* ¿Por que vuelve a verificar si el cliente se conecto? Si se supone que el servidor ya acepto una conexion.
				 * Porque el cliente se puede conectar o desconectar, es decir que el servidor no puede comprobar si se desconecto,
				 * por lo tanto necesita aceptar una conexion y ver el estado de la conexion por parte de ese cliente, es decir si es
				 * true (conectado) o false (desconectado). */
				if (packet.isConnected()) { // Si se conecto...

					console.append(packet.getNickOrig() + " /" + packet.getIpOrig() + ":" + packet.getPort() + " connected!\n");

					if (packet.getMessage() != null) { // Si envio un mensaje...

						console.append(packet.getNickOrig() + ": " + packet.getMessage() + " >> " + packet.getNickDest() + "\n");
						// Crea un puente virtual con el cliente especificado
						socketOut = new Socket(packet.getIpDest(), CLIENT_PORT);
						// Crea un flujo de salida para enviar el paquete
						streamOut = new ObjectOutputStream(socketOut.getOutputStream());
						// Escribe el paquete serializado en el flujo
						streamOut.writeObject(packet);

					} else { // Si solo se conecto...

						// Agrega a la coleccion el nick asociado con la ip
						connections.put(packet.getNickOrig(), packet.getIpOrig());

						// Agrega la coleccion actualizada al paquete
						packet.setConnections(connections);

						broadcast();

					}

				} else { // Si se desconecto...

					console.append(packet.getNickOrig() + " /" + packet.getIpOrig() + ":" + packet.getPort() + " was disconnected!\n");

					// Elimina la conexion de la coleccion
					for (String nick : connections.keySet())
						if (nick.equalsIgnoreCase(packet.getNickOrig())) connections.remove(nick);

					packet.setConnections(connections);

					broadcast();
					disconnect();

				}

			} catch (SocketException e) {
				Info.showServerMessage("Socket accessing failed\n" + e);
			} catch (IOException e) {
				Info.showServerMessage("I/O error\n" + e);
			} catch (ClassNotFoundException e) {
				Info.showServerMessage("Class of a serialized object cannot be found\n" + e);
			}
		}

	}

	/**
	 * Le avisa a todos los clientes cuando un cliente se conecta o desconecta.
	 * <p>
	 * TODO: Esto realmente es simultaneo? Por que lo hace nodo por nodo usando un for, en todo caso seria multicast o no?
	 */
	private void broadcast() throws IOException {
		for (String ip : connections.values()) {
			socketOut = new Socket(ip, CLIENT_PORT);
			streamOut = new ObjectOutputStream(socketOut.getOutputStream());
			streamOut.writeObject(packet);
		}
	}

	private void disconnect() throws IOException {
		if (socketIn != null && socketOut != null) {
			socketIn.close();
			socketOut.close();
		}
	}

	private synchronized void suspender() {

		suspendido = true;

		btnOpen.setEnabled(true);
		btnStop.setEnabled(false);

		try {
			// wait();
			server.close(); // TODO Hace falta cerrarlo? Si en realidad se pone en pausa

		} catch (IOException e) {
			Info.showServerMessage("Error closing server\n" + e);
		}

	}

	/* ¿Hay alguna forma de ejecutar el segundo subproceso que acepta conexiones sin tener que crear un nuevo subproceso?
	 * Creo que se podria poner a la espera ese subproceso con el metodo wait() y despertarlo cuando re abro el server
	 * usando un notify(). */
	private void reOpen() {

		new Thread(this).start();

		suspendido = false;
		// notify();

		btnStop.setEnabled(true);
		btnOpen.setEnabled(false);
	}

	private synchronized boolean isStopped() {
		return suspendido;
	}

	public static void main(String[] args) throws Exception {

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		Server server = new Server();
		// Crea un hilo para aceptar y procesar conexiones en segundo plano
		new Thread(server).start();
		server.setVisible(true);

	}

}

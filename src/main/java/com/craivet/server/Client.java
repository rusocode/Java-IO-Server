package com.craivet.server;

import java.net.*;
import java.util.Map;
import java.awt.event.*;
import java.io.*;
import java.util.Objects;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import static com.craivet.util.Constants.*;

/**
 * Clase encargada de conectarse con un servidor usando el protocolo TCP.
 * Tambien esta clase recibe conexiones por parte de otros clientes, por lo tanto necesita estar a la escucha.
 * <p>
 * Si el servidor este alojado en la misma maquina que el cliente, se utilizaria 127.0.0.1 o localhost como ip local.
 * En caso de que el servidor este en una maquina diferente a la del cliente pero en la misma red (lan), habria que
 * utilizar la ip remota.
 * <p>
 * Ahora mismo se necesita crear una nueva conexion cada vez que quiera enviar mensajes, entonces no estaria entendido
 * la logica de por que no puedo usar la misma conexion para esto.
 * <p>
 * TODO Se puede reemplazar la conexion unidireccional por bidireccional usando los sockets de java? Con este concepto estaria utilizando la misma conexion para enviar y recibir mensajes, siendo un proceso mas eficiente.
 * <p>
 * TODO Agregar un icono de enchufe al boton connect.
 * <p>
 * TODO Encadenar metodos
 *
 * @author Ruso
 */

public class Client extends JFrame implements Runnable {

    private static final long serialVersionUID = 1L;

    private JTextField txtNick, txtServer, txtMessage;
    private JButton btnConnect;
    private JComboBox<String> nicks;
    private JTextArea console;

    private Socket socketOut, socketIn;
    private ObjectOutputStream streamOut;
    private Packet packet;

    private Map<String, String> connections;

    public Client() {
        super("Client");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initialize();
    }

    private void initialize() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout());

        JLabel lblNick = new JLabel("Nick");
        panel.add(lblNick);

        txtNick = new JTextField();
        txtNick.addKeyListener(new Listener());
        panel.add(txtNick, "w 100!");

        JLabel lblServidor = new JLabel("Server");
        panel.add(lblServidor);

        txtServer = new JTextField("localhost");
        txtServer.addKeyListener(new Listener());
        panel.add(txtServer, "w 100!");

        btnConnect = new JButton("Connect");
        btnConnect.addActionListener(new Listener());
        btnConnect.setFocusable(false);
        btnConnect.setEnabled(false);
        /* Le paso un ancho de 93px (lo que ocupa "Desconectar") para que cuando se cambie el texto a "Desconectar" no se vea el
         * agrandamiento de la ventana, ya que "Conectar" ocupa 77px. */
        panel.add(btnConnect, "w 93!, wrap");

        nicks = new JComboBox<>();
        nicks.addActionListener(new Listener());
        nicks.setFocusable(false);
        nicks.setEnabled(false);
        panel.add(nicks, "span, grow, wrap");

        console = new JTextArea();
        console.setEditable(false);
        JScrollPane scroll = new JScrollPane(console);
        panel.add(scroll, "h 300!, span, grow, wrap");

        txtMessage = new JTextField();
        txtMessage.addKeyListener(new Listener());
        txtMessage.setEnabled(false);
        panel.add(txtMessage, "span, grow");

        add(panel);

        pack();
        setLocationRelativeTo(null);
    }

    private class Listener extends KeyAdapter implements ActionListener {
        // Sirve para comprobar el estado del boton
        private boolean flag;

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() == btnConnect) connect();
        }

        public void keyReleased(KeyEvent evt) {
            if (evt.getSource() == txtMessage && evt.getKeyChar() == KeyEvent.VK_ENTER) sendMessage();
            if (evt.getSource() == txtNick || evt.getSource() == txtServer) enableBTN();
        }

        /**
         * Crea un puente virtual con el servidor en caso de que el cliente se conecte o desconecte.
         */
        private void connect() {
            try {

                socketOut = new Socket(txtServer.getText(), SERVER_PORT);
                streamOut = new ObjectOutputStream(socketOut.getOutputStream());

                packet = new Packet();
                packet.setNickOrig(txtNick.getText());

                // System.out.println(socketOut.getPort()); // 7666
                // System.out.println(socketOut.getLocalPort()); // 60176
                // System.out.println(socketOut.getLocalSocketAddress()); // /127.0.0.1:60176
                // System.out.println(socketOut.getLocalAddress()); // /127.0.0.1
                // System.out.println(socketOut.getRemoteSocketAddress()); // localhost/127.0.0.1:7666
                // System.out.println(socketOut.getInetAddress()); // localhost/127.0.0.1
                // System.out.println(socketOut.getInetAddress().getHostName()); // localhost
                // System.out.println(socketOut.getInetAddress().getHostAddress()); // 127.0.0.1
                // System.out.println(socketOut.getInetAddress().getAddress()); // [B@2dd809be
                // System.out.println(InetAddress.getLocalHost()); // Juan2/192.168.1.6
                // System.out.println(InetAddress.getLocalHost().getHostName()); // Juan2
                // System.out.println(InetAddress.getLocalHost().getHostAddress()); // 192.168.1.6

                packet.setIpOrig(InetAddress.getLocalHost().getHostAddress());
                packet.setPort("" + socketOut.getLocalPort());

                /* Si el cliente se conecto, envia el paquete con el tipo de conexion, deshabilita los campos de texto (para evitar que
                 * se modifiquen mientras este conectado), cambia el nombre del boton a "Disconnect", habilita el combo y habilita el
                 * campo de mensajes y le pasa el foco. */
                if (btnConnect.getText().equals("Connect")) {

                    packet.setConnected(true);
                    streamOut.writeObject(packet);

                    txtNick.setEnabled(false);
                    txtServer.setEnabled(false);
                    btnConnect.setText("Disconnect");
                    nicks.setEnabled(true);
                    txtMessage.setEnabled(true);
                    txtMessage.requestFocus();

                    flag = false;

                }

                /* Si el cliente se desconecto, envia el paquete con el tipo de conexion, cierra la conexion, habilita los campos de
                 * texto, cambia el nombre del boton a "Connect", limpia el combo y lo habilita, limpia la consola y limpia el campo de
                 * mensajes y lo habilita. */
                if (btnConnect.getText().equals("Disconnect") && flag) {

                    // Obviamente envia el paquete antes de cerrar la conexion para avisarle al servidor que se desconecto
                    packet.setConnected(false);
                    streamOut.writeObject(packet);

                    if (socketOut != null) socketOut.close();

                    if (socketOut != null && socketOut.isClosed()) {

                        txtNick.setEnabled(true);
                        txtServer.setEnabled(true);
                        btnConnect.setText("Connect");
                        nicks.removeAllItems();
                        nicks.setEnabled(false);
                        console.setText(null);
                        txtMessage.setText("");
                        txtMessage.setEnabled(false);

                    }

                }

                flag = true;

            } catch (ConnectException e) {
                Info.showClientMessage("The server is off\n" + e);
            } catch (UnknownHostException e) {
                Info.showClientMessage("Could not determine host IP address\n" + e);
            } catch (IOException e) {
                Info.showClientMessage("I/O error\n" + e);
            } finally {
                try {
                    if (socketOut != null) socketOut.close();
                } catch (IOException e) {
                    Info.showClientMessage("Error closing the connection\n" + e);
                }
            }
        }

        private void enableBTN() {
            if (txtNick != null && txtServer != null)
                btnConnect.setEnabled(!txtNick.getText().isEmpty() && !txtServer.getText().isEmpty());
        }

        private void sendMessage() {
            try {

                // Si el mensaje no esta vacio...
                if (!txtMessage.getText().isEmpty()) {

                    console.append(txtNick.getText() + ": " + txtMessage.getText() + "\n");

                    /* ¿Por que tengo que crear una nueva conexion cuando necesito enviar un mensaje, si se tendria que usar la misma
                     * conexion para esto? */
                    socketOut = new Socket(txtServer.getText(), SERVER_PORT);
                    streamOut = new ObjectOutputStream(socketOut.getOutputStream());
                    packet = new Packet();
                    packet.setConnected(true);
                    packet.setNickOrig(txtNick.getText());
                    packet.setIpOrig(InetAddress.getLocalHost().getHostAddress());
                    packet.setPort("" + socketOut.getLocalPort());
                    packet.setNickDest(Objects.requireNonNull(nicks.getSelectedItem()).toString());
                    packet.setIpDest(connections.get(nicks.getSelectedItem().toString()));
                    packet.setMessage(txtMessage.getText());
                    streamOut.writeObject(packet);
                    txtMessage.setText("");

                    // Desplaza el scroll hacia abajo cuando hay mucho texto
                    console.setCaretPosition(console.getDocument().getLength());

                }

            } catch (ConnectException e) {
                Info.showClientMessage("The server is off\n" + e);
            } catch (UnknownHostException e) {
                Info.showClientMessage("Could not determine host IP address\n" + e);
            } catch (IOException e) {
                Info.showClientMessage("I/O error\n" + e);
            } finally {
                try {
                    if (socketOut != null) socketOut.close();
                } catch (IOException e) {
                    Info.showClientMessage("Error closing the connection\n" + e);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        // Crea un servidor desde el cliente para poder recibir conexiones de otros clientes
        try (ServerSocket server = new ServerSocket(CLIENT_PORT)) {

            while (true) { // https://stackoverflow.com/questions/28113856/while-statement-cannot-complete-without-throwing-an-exception-android

                socketIn = server.accept();
                ObjectInputStream streamIn = new ObjectInputStream(socketIn.getInputStream());
                packet = (Packet) streamIn.readObject();

                // Si recibio un mensaje...
                if (packet.getMessage() != null)
                    console.append(packet.getNickOrig() + ": " + packet.getMessage() + "\n");
                else { // Si un cliente se conecto o desconecto...
                    // Actualiza los nicks del combo
                    nicks.removeAllItems();
                    connections = packet.getConnections();
                    for (String nick : connections.keySet())
                        nicks.addItem(nick);
                }

            }

        } catch (BindException e) {
            Info.showClientMessage("You already have another process linked to the same port\n" + e);
            System.exit(0);
        } catch (IOException e) {
            Info.showClientMessage("I/O error\n" + e);
        } catch (IllegalArgumentException e) {
            Info.showClientMessage(
                    "The port is outside the specified range of valid port values, which is between 0 and 65535, inclusive\n" + e);
        } catch (ClassNotFoundException e) {
            Info.showClientMessage("Class of a serialized object cannot be found\n" + e);
        } finally {
            try {
                if (socketIn != null) socketIn.close();
            } catch (IOException e) {
                Info.showClientMessage("Error closing the connection\n" + e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Client client = new Client();
        new Thread(client).start();
        client.setVisible(true);
    }

}

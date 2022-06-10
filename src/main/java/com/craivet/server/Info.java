package com.craivet.server;

import javax.swing.JOptionPane;

public class Info {

	public static void showClientMessage(String msg) {
		JOptionPane.showMessageDialog(null, msg, "Client error", JOptionPane.ERROR_MESSAGE);
	}

	public static void showServerMessage(String msg) {
		JOptionPane.showMessageDialog(null, msg, "Server error", JOptionPane.ERROR_MESSAGE);
	}

}

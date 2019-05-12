package com.example.battleships;

import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Connection {
	
	private BufferedReader in;
	private PrintWriter out;
	private BluetoothSocket opponent;

	public Connection(BluetoothSocket opponent)  {
		try {
			this.opponent = opponent;
			in = new BufferedReader(new InputStreamReader(opponent.getInputStream()));
			out = new PrintWriter(opponent.getOutputStream(), true);
		} catch(IOException e) {
			System.out.println("Connection failed");
		}
	}
	
	public void send(String msg) {
		out.println(msg);
	}
	
	public String receive() {
		try {
			return in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public void close() {
		try {
			in.close();
			out.close();
			opponent.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

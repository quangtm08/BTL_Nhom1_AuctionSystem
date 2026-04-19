package com.nhom1.auction.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true)
        ) {
            String message;

            while ((message = in.readLine()) != null) {
                System.out.println("Client send: " + message);

                String response = "Server receive: " + message;

                out.println(response);
            }

        } catch (IOException e) {
            System.out.println("Client disconnect");
        }
    }
}
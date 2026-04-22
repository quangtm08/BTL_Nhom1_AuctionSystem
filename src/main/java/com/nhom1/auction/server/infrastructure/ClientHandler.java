package com.nhom1.auction.server.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
Each client interacts with the server through a ClientHandler
It reads from the socket, asks the MessageRouter for an answer, and writes back.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final MessageRouter router;

    public ClientHandler(Socket socket, MessageRouter router) {
        this.socket = socket;
        this.router = router;
    }


    //Read JSON request from socket with BufferedReader and write JSON response to socket with PrintWriter
    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String inputLine;
            //Continue to read till reaches the end (return null)
            while ((inputLine = in.readLine()) != null) { 
                // Pass raw JSON to message router and get raw JSON response back
                String response = router.handleRequest(inputLine);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("ClientHandler Error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

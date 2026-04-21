package com.nhom1.auction.server.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
Each client interacts with the server through a ClientHandler
It reads from the socket, asks the Router for an answer, and writes back.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final MessageRouter router;

    public ClientHandler(Socket socket, MessageRouter router) {
        this.socket = socket;
        this.router = router;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String inputLine;
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

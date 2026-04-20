package com.nhom1.auction.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ClientHandler implements Runnable {
    private final Socket socket;
    private final MessageRouter router;

    public ClientHandler(Socket socket, MessageRouter router) {
        this.socket = socket;
        this.router = router;
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
                // Ignore empty lines
                if (message.trim().isEmpty()) continue;
                
                System.out.println("Received request: " + message);

                // Pass the raw JSON to the router and get a raw JSON response
                String response = router.handleRequest(message);

                // Send response back (one line per message)
                out.println(response);
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
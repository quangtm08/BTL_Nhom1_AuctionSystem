package com.nhom1.auction.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.nhom1.auction.server.infrastructure.ClientHandler;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.ServerContext;

/*
 - The entry point of the Auction System. Run this file will run the server
 - It initializes the ServerContext (Shared infrastructure + Modules) and manages the Socket network connections.
 */
public class Server {
    public static void main(String[] args) {
        // Use the PORT environment variable if available (required for Railway deployment)
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null) ? Integer.parseInt(portEnv) : 41177;
        int clientCount = 0;
        ExecutorService pool = Executors.newFixedThreadPool(10);

        try {
            // 1. Initialize Infrastructure & Features via Context
            // This builds the Database connection, Router, and all Modules (Auth, etc.)
            ServerContext context = new ServerContext();
            MessageRouter router = context.getRouter();
            var clientRegistry = context.getClientRegistry();

            // 2. Start Network Listening
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("========================================");
                System.out.println("   Listening on port: " + port);
                System.out.println("========================================");

                while (true) {
                    //Blocking call: the server waits here until a client connects and wakes it up
                    Socket socket = serverSocket.accept();
                    clientCount++;

                    System.out.println("[" + clientCount + "] New client connection from: " + socket.getInetAddress());

                    // Each thread gets a reference to the same router and registry
                    pool.execute(new ClientHandler(socket, router, clientRegistry));
                }
            }
        } catch (Exception e) {
            System.err.println("CRITICAL: Server failed to start: " + e.getMessage());
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}


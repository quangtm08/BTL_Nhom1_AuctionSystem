package com.nhom1.auction.server;

import com.nhom1.auction.server.database.DBConnection;
import com.nhom1.auction.server.handler.AuthHandler;
import com.nhom1.auction.server.repository.UserRepository;
import com.nhom1.auction.server.service.AuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        int port = 12345;
        int clientCount = 0;
        ExecutorService pool = Executors.newFixedThreadPool(10);

        try {
            // 1. Initialize Infrastructure (Composition Root)
            // We create these ONCE and share them across all threads
            Connection conn = DBConnection.getConnection();
            if (conn == null) {
                System.err.println("CRITICAL: Could not establish database connection. Exiting.");
                return;
            }


            UserRepository userRepository = new UserRepository(conn);
            AuthService authService = new AuthService(userRepository);
            AuthHandler authHandler = new AuthHandler(authService);
            MessageRouter router = new MessageRouter(authHandler);

            // 2. Start Network Listening
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Auction Server is running on port: " + port);
                System.out.println("Ready to accept client connections...");

                while (true) {
                    Socket socket = serverSocket.accept();
                    clientCount++;
                    
                    System.out.println("[" + clientCount + "] New client connected from: " + socket.getInetAddress());
                    
                    // Each thread gets a reference to the same router (Thread-Safe)
                    pool.execute(new ClientHandler(socket, router));
                }
            }
        } catch (IOException e) {
            System.err.println("Server critical error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

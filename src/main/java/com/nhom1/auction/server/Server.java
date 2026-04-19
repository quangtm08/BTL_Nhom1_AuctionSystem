package com.nhom1.auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    public static void main(String[] args) {
        int port = 12345 , ClientConnected = 0;
        ExecutorService pool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server run with port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                pool.execute(new ClientHandler(socket));
                ClientConnected++;
                System.out.println("Client connected to Server: " + ClientConnected + " client.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

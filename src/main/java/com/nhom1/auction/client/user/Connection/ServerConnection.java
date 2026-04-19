package com.nhom1.auction.client.user.Connection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection extends Thread {
    

    // Attributes

    private Socket socket;
    private boolean isListening;
    private ObjectInputStream input;  
    private ObjectOutputStream output;


    // Constructor
    public ServerConnection(String ip, int port) {
        isListening = false;

        try {
            socket = new Socket(ip, port);
            output = new ObjectOutputStream(socket.getOutputStream());

            } catch (IOException e) {
                e.printStackTrace();
            }
    }


    // Getter , Setter
    public Socket getSocket() {
        return socket;
    }

    public ObjectInputStream getInput() {
        return input;
    }

    public ObjectOutputStream getOutput() {
        return output;
    }

    public void setListening(boolean listening) {
        isListening = listening;
    }

    public void setInput(ObjectInputStream input) {
        this.input = input;
    }
    
    public void setOutput(ObjectOutputStream output) {
        this.output = output;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    // Methods

    public void stopListening(){
        isListening = false;
    }

    public void startListenting(){
        isListening = true;
    }

}

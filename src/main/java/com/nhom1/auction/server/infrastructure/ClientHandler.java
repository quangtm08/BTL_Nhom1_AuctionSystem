package com.nhom1.auction.server.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
Each client interacts with the server through a ClientHandler
It reads from the socket, asks the MessageRouter for an answer, and writes back.
 */
public class ClientHandler implements Runnable {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Socket socket;
    private final MessageRouter router;
    private PrintWriter out;
    private final UUID clientId;
    private final ClientRegistry registry;

    public ClientHandler(
        Socket socket,
        MessageRouter router,
        ClientRegistry registry
    ) throws IOException {
        this.socket = socket;
        this.router = router;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.registry = registry;
        this.clientId = UUID.randomUUID();
    }

    @Override
    public void run() {
        registry.register(this);

        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String response = router.handleRequest(inputLine);
                push(response);
                
                try {
                    JsonNode respNode = mapper.readTree(response);
                    if (respNode.has("success") && respNode.get("success").asBoolean()) {
                        if (respNode.has("type")) {
                            String typeStr = respNode.get("type").asText();
                            if ("LOGIN".equals(typeStr) || "REGISTER".equals(typeStr)) {
                                JsonNode payload = respNode.get("payload");
                                if (payload != null && payload.has("userID")) {
                                    UUID userId = UUID.fromString(payload.get("userID").asText());
                                    registry.linkUser(userId, clientId);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore linking error
                }
            }
        } catch (IOException e) {
            System.err.println(
                "ClientHandler Error (" + clientId + "): " + e.getMessage()
            );
        } finally {
            // Hủy đăng ký khi kết nối đóng
            registry.unregister(clientId);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void push(String json) {
        if (out != null) {
            out.println(json);
        }
    }

    public UUID getClientId() {
        return clientId;
    }
}

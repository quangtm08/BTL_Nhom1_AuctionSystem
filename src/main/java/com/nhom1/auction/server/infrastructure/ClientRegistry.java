package com.nhom1.auction.server.infrastructure;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// ClientRegistry keeps track of the active clients and their client handlers
public class ClientRegistry {
    private final Map<UUID, ClientHandler> activeClients = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> userToClientMap = new ConcurrentHashMap<>();

    public void register(ClientHandler handler) {
        activeClients.put(handler.getClientId(), handler);
        System.out.println("[Registry] New connection: " + handler.getClientId());
    }

    // Remove user/client from the list when they are offline
    public void unregister(UUID clientId) {
        activeClients.remove(clientId);
        userToClientMap.values().removeIf(id -> id.equals(clientId));
        System.out.println("[Registry] Connection closed: " + clientId);
    }

    // Match userId to clientId after the user signed in
    public void linkUser(UUID userId, UUID clientId) {
        userToClientMap.put(userId, clientId);
        System.out.println("[Registry] User " + userId + " linked to client " + clientId);
    }

    // Send message to every online clients
    public void broadcast(String json) {
        activeClients.values().forEach(client -> client.push(json));
    }

    // Send message to a specific user
    public void sendToUser(UUID userId, String json) {
        UUID clientId = userToClientMap.get(userId);
        if (clientId != null) {
            ClientHandler handler = activeClients.get(clientId);
            if (handler != null) {
                handler.push(json);
            }
        }
    }
}

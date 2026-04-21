package com.nhom1.auction.server.auth;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import java.sql.Connection;

/**
 * AuthModule: The "Lego Kit" for the Authentication feature.
 * 
 * This class is responsible for building all the parts of the Auth feature
 * (Repository, Service, Handler) and "plugging" them into the Server.
 */
public class AuthModule {

    /**
     * The "Instant Setup" for Authentication.
     * 
     * @param connection The shared database connection from ServerContext.
     * @param router The shared MessageRouter from ServerContext.
     */
    public static void init(Connection connection, MessageRouter router) {
        // 1. Build the "Database Engine" (Repository)
        UserRepository repository = new UserRepository(connection);
        
        // 2. Build the "Business Brain" (Service)
        AuthService service = new AuthService(repository);
        
        // 3. Build the "Translator" (Handler)
        AuthHandler handler = new AuthHandler(service);
        
        // 4. THE WIRING: Tell the handler to register its routes in the Router
        handler.register(router);
        
        System.out.println("AuthModule: Feature initialized successfully.");
    }
}

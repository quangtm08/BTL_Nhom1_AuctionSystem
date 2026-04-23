package com.nhom1.auction.server.auth;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import java.sql.Connection;

/*
 - This class is responsible for building all the parts of the Auth feature
 (Repository, Service, Handler) and "plugging" them into the Server.
 */
public class AuthModule {

    /*
     * @param connection The shared database connection from ServerContext.
     * @param router The shared MessageRouter from ServerContext.
     * @return The UserRepository instance, so ServerContext can share it with other modules.
     */
    public static UserRepository init(Connection connection, MessageRouter router) {
        // 1. Build Repository
        UserRepository repository = new UserRepository(connection);
        
        // 2. Build Service
        AuthService service = new AuthService(repository);
        
        // 3. Build Handler
        AuthHandler handler = new AuthHandler(service);
        
        // 4. Tell the handler to register its routes in the Router
        handler.register(router);
        
        System.out.println("AuthModule: Feature initialized successfully.");
        return repository;
    }
}

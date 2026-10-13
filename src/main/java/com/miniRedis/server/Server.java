package com.miniRedis.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.miniRedis.command.CommandDispatcher;
import com.miniRedis.store.ExpirySweeper;
import com.miniRedis.store.Store;

public class Server {
    private final ServerSocket server;
    private final int port = 6379;
    private final Store store = new Store();
    private final CommandDispatcher commandDispatcher = new CommandDispatcher();

    public Server() throws IOException {
        this.server = new ServerSocket(port);
    }

    public void start() throws IOException {
        ExpirySweeper expirySweeper = new ExpirySweeper(store, 1);
        expirySweeper.start();
        while (true) {
            Socket socket = server.accept();
            ClientHandler clientHandler = new ClientHandler(socket, store, commandDispatcher);
            Thread.startVirtualThread(clientHandler);
            

        }
    }

}

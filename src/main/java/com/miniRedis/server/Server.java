package com.miniRedis.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.miniRedis.server.ClientHandler;

public class Server {
    private final ServerSocket server;
    private final int port = 6379;

    public Server() throws IOException {
        this.server = new ServerSocket(port);
    }

    public void start() throws IOException {
        while (true) {
            Socket socket = server.accept();
            ClientHandler clientHandler = new ClientHandler(socket);
            Thread.startVirtualThread(clientHandler);
        }
    }

}

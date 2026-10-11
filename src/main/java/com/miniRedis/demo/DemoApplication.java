package com.miniRedis.demo;

import java.io.IOException;

import com.miniRedis.server.Server;

public class DemoApplication {

	public static void main(String[] args) throws IOException {
		Server server = new Server();
		server.start();
		
	}
}
package com.miniRedis.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.miniRedis.command.CommandDispatcher;
import com.miniRedis.persistence.AofWriter;
import com.miniRedis.protocol.RespParser;
import com.miniRedis.protocol.RespValue;
import com.miniRedis.protocol.RespWriter;
import com.miniRedis.store.Store;
public class ClientHandler implements  Runnable {
    


    private final Socket socket;
    private final Store store;
    private final CommandDispatcher dispatcher;
    private final AofWriter aofWriter;
   public ClientHandler(Socket socket, Store store, CommandDispatcher dispatcher , AofWriter aofWriter) {
    this.socket = socket;
    this.store = store;
    this.dispatcher = dispatcher;
    this.aofWriter = aofWriter;
}
    @Override
    public void run() {
            try (socket){
                    RespParser parser = new RespParser(socket.getInputStream());
                    RespWriter writer = new RespWriter(socket.getOutputStream());
                    while(true){
                        RespValue value = parser.parse();
                        if(value==null) break;
                        if (value instanceof RespValue.Array array ){     // modern casting
                            List<String> tokens = new ArrayList<>();
                            for(int i=0;i<array.value().size();i++){
                                tokens.add(((RespValue.Bulk)array.value().get(i)).value());
                            }
                            RespValue response = dispatcher.dispatch(tokens, store);
                            writer.write(response);
                            String cmd = tokens.get(0).toUpperCase();
                            boolean appendable = isWriteCommand(cmd);
                            if (appendable &&!(response instanceof RespValue.Error)){
                                aofWriter.append(tokens);
                            }

                            }
                            
                            
                        }
                        
                            

                    }
            catch(IOException e){
                e.printStackTrace();
            }
    }
    private boolean isWriteCommand(String cmd) {
        return switch(cmd) {
            case "SET", "DEL", "EXPIRE", "INCR" -> true;
            default -> false;
        };
    }
}

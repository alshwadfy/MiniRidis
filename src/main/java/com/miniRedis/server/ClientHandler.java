package com.miniRedis.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.miniRedis.command.CommandDispatcher;
import com.miniRedis.protocol.RespParser;
import com.miniRedis.protocol.RespValue;
import com.miniRedis.protocol.RespWriter;
import com.miniRedis.store.Store;
public class ClientHandler implements  Runnable {
    


    private final Socket socket;
    private final Store store;
    private final CommandDispatcher dispatcher;
   public ClientHandler(Socket socket, Store store, CommandDispatcher dispatcher) {
    this.socket = socket;
    this.store = store;
    this.dispatcher = dispatcher;
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
                            
                        }
                        
                            


                    }

            }
            catch(IOException e){
                e.printStackTrace();
            }
    }

    
}

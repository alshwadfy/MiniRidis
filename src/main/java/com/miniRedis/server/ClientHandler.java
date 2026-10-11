package com.miniRedis.server;

import java.io.IOException;
import java.net.Socket;
import com.miniRedis.protocol.RespParser;
import com.miniRedis.protocol.RespValue;
import com.miniRedis.protocol.RespValue.SimpleString;
import com.miniRedis.protocol.RespWriter;
public class ClientHandler implements  Runnable {
    


    private final Socket socket;

    public ClientHandler(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
            try (socket){
                    RespParser parser = new RespParser(socket.getInputStream());
                    RespWriter writer = new RespWriter(socket.getOutputStream());
                    while(true){
                        RespValue value = parser.parse();
                        if(value==null) break;
                        if (value instanceof RespValue.Array ){
                            RespValue.Array array = (RespValue.Array) value;
                            String command = ((RespValue.Bulk)array.value().get(0)).value().toUpperCase();
                            if (command.equals("PING")){
                                writer.write(new SimpleString("PONG"));
                            }
                            else{
                                writer.write(new RespValue.Error("Unknown command"));
                            }
                            
                        }
                        
                            


                    }

            }
            catch(IOException e){
                e.printStackTrace();
            }
    }

    
}

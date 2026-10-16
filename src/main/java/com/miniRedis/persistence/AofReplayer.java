package com.miniRedis.persistence;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.miniRedis.command.CommandDispatcher;
import com.miniRedis.store.Store;

public class AofReplayer {
    private final String path;
    private final CommandDispatcher dispatcher;

    public AofReplayer(String path, CommandDispatcher dispatcher) { 
        this.path = path;
        this.dispatcher = dispatcher;
        

     }

    public void replay(Store store) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;

        while ((line = reader.readLine()) != null) {
            
            if (!line.startsWith("*")) continue; 
    
            int tokenCount = Integer.parseInt(line.substring(1)); 
    
            List<String> tokens = new ArrayList<>();
    
            for (int i = 0; i < tokenCount; i++) {
                String lengthLine = reader.readLine(); // "$3"
                String token = reader.readLine();      // "SET"
                tokens.add(token);
    }
    
    dispatcher.dispatch(tokens, store);
        }

    }





    }  


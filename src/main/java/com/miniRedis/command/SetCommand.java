package com.miniRedis.command;

import java.util.List;

import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public class SetCommand implements Command {

    @Override
    public RespValue execute(List<String> args, Store store) {
        

        if(args.size()!=2){
            return new 
            RespValue.Error("ERR wrong number of arguments for 'set' command");

        }
        String key=args.get(0);
        String value=args.get(1);
        store.set(key, value);
        return new RespValue.SimpleString("OK");
    }

    
}

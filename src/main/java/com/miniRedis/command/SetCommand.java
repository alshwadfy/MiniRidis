package com.miniRedis.command;

import java.util.List;

import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public class SetCommand implements Command {

    @Override
    public RespValue execute(List<String> args, Store store) {
        

        if(args.size()!=4 && args.size()!=2){
            return new 
            RespValue.Error("ERR wrong number of arguments for 'set' command");

        }
        String key=args.get(0);
        String value=args.get(1);

        if(args.size()==2){
            store.set(key, value,null);
            return new RespValue.SimpleString("OK");
        }
      else if (args.get(2).equalsIgnoreCase("EX")) {
    try {
        long seconds = Long.parseLong(args.get(3));
        if (seconds <= 0) {
            return new RespValue.Error("ERR invalid expire time in 'set' command");
        }
        store.set(key, value, System.currentTimeMillis() + (seconds * 1000));
        return new RespValue.SimpleString("OK");
    } catch (NumberFormatException e) {
        return new RespValue.Error("ERR value is not an integer or out of range");
    }
}
    return new RespValue.Error("ERR syntax error");

    }
    
}

package com.miniRedis.command;

import java.util.List;

import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public class TtlCommand implements Command {

    @Override
    public RespValue execute(List<String> args, Store store) {
        if(args.size()!=1){
            return new RespValue.Error("ERR wrong number of arguments for 'ttl' command");
        }
        long ttl=store.ttl(args.get(0));
        return new RespValue.Integer(ttl);
    }


    
}

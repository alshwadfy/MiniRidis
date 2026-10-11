package com.miniRedis.command;

import java.util.List;

import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public class DelCommand implements Command {

    @Override
    public RespValue execute(List<String> args, Store store) {
    if(args.size()!=1){
        return new RespValue.Error("ERR wrong number of arguments for 'del' command");
    }
  boolean deleted =  store.delete(args.get(0));
    return new RespValue.Integer(deleted?1L:0L);
    }
    
}

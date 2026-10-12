package com.miniRedis.command;

import java.util.List;

import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public class IncrCommand implements Command {
    @Override
    public RespValue execute(List<String> args, Store store) {
        if (args.size() != 1) {
            return new RespValue.Error("ERR wrong number of arguments for 'incr' command");
        }

        try {
            long value = store.incr(args.get(0));
            return new RespValue.Integer(value);
        } catch (NumberFormatException e) {
            return new RespValue.Error("ERR value is not an integer or out of range");
        }
    }
}




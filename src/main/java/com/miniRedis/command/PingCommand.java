package com.miniRedis.command;

import java.util.List;

import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public class PingCommand implements Command {

    @Override
    public RespValue execute(List<String> args, Store store) {

        if (!args.isEmpty()) {
            return new RespValue.Bulk(args.get(0));
        }
        return new RespValue.SimpleString("PONG");

    }
}

package com.miniRedis.command;

import java.util.List;

import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public class ExpireCommand implements Command {
    @Override
    public RespValue execute(List<String> args, Store store) {
        if (args.size() != 2) {
            return new RespValue.Error("ERR wrong number of arguments for 'expire' command");
        }

        try {
            long seconds = Long.parseLong(args.get(1));
            if (seconds < 0) {
                return new RespValue.Error("ERR invalid expire time");
            }
            boolean result = store.expire(args.get(0), seconds);
            return new RespValue.Integer(result ? 1L : 0L);
        } catch (NumberFormatException e) {
            return new RespValue.Error("ERR invalid expire time");
        }
    }
}

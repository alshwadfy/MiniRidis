package com.miniRedis.command;

import java.util.List;
import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public interface Command {
    RespValue execute(List<String> args, Store store);
}

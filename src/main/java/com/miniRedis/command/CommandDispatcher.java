package com.miniRedis.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.miniRedis.protocol.RespValue;
import com.miniRedis.store.Store;

public class CommandDispatcher {
    
    private final Map<String, Command> commands = new HashMap<>();

    public CommandDispatcher() {
        register("SET", new SetCommand());
        register("GET", new GetCommand());
        register("DEL", new DelCommand());
        register("EXISTS", new ExistsCommand());
        register("PING", new PingCommand());
        register("INCR", new IncrCommand());
        
    }

    public void register(String name, Command command) {
        commands.put(name.toUpperCase(), command);
    }

    public Command getCommand(String name) {
        return commands.get(name.toUpperCase());
    }
    public RespValue dispatch(List<String> tokens, Store store){
        if(tokens.isEmpty()){
        return new RespValue.Error("ERR Empty command");
        }
        String commandName=tokens.get(0).toUpperCase();
        Command command=getCommand(commandName);
        if(command==null){
            return new RespValue.Error("ERR unknown command '"+commandName+"'");
        }
        List<String> args=tokens.subList(1, tokens.size());
        return command.execute(args, store);
    }

 
}

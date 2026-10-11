package com.miniRedis.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RespWriter {
    private final OutputStream outputStream;

    public RespWriter(OutputStream outputStream){
        this.outputStream = outputStream;

    }

    public void write(RespValue value) throws IOException {
        switch (value) {
            case RespValue.SimpleString simpleString -> {
                outputStream.write(("+" + simpleString.value() + "\r\n").getBytes(StandardCharsets.UTF_8));

            }

            case RespValue.Error error -> {
                outputStream.write(("-" + error.message() + "\r\n").getBytes(StandardCharsets.UTF_8));

            }

            case RespValue.Integer integer -> {
                outputStream.write((":" + integer.value() + "\r\n").getBytes(StandardCharsets.UTF_8));

            }

            case RespValue.Bulk bulk -> {
                if(bulk.value() == null){
                    outputStream.write("$-1\r\n".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                outputStream.write(("$"+ bulk.value().length() +"\r\n").getBytes(StandardCharsets.UTF_8));
                outputStream.write(bulk.value().getBytes(StandardCharsets.UTF_8));
                outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
               

            }

            case RespValue.Array array -> {
                outputStream.write(("*"+array.value().size() + "\r\n").getBytes(StandardCharsets.UTF_8));
                    for(RespValue respValue :array.value())
                        write(respValue);

            }
        }
    }
    
}


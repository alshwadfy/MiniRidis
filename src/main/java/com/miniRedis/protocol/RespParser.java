package com.miniRedis.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RespParser {
    private static final byte CR = '\r';
    private static final byte LF = '\n';
    private final InputStream inputStream;

    public RespParser(InputStream inputStream){
        this.inputStream = inputStream;
    }
    
    private String readLine () throws IOException{
    StringBuilder line = new StringBuilder();

    int read;
    while ((read=inputStream.read())!= -1){
        byte b=(byte)read;
        if(b==CR) continue;
        if (b == LF) break;
        line.append((char) b);
    }
    return line.toString();
    }
    
    
    public RespValue parse()   throws IOException{
            int b = inputStream.read();
            
            if(b==-1) {
                return null;
            }
            return switch(b){
                case '+' -> new RespValue.SimpleString(readLine());
                case '-' -> new RespValue.Error(readLine());
                case ':' -> new RespValue.Integer(Long.parseLong(readLine()));
                case '$' -> {
                    int length = Integer.parseInt(readLine());
                    if(length == -1){
                        yield new RespValue.Bulk(null);
                    }
                    byte [] buffer = inputStream.readNBytes(length);
                    // Consume the \r\n after the bulk data
                    inputStream.readNBytes(2);
                    yield new RespValue.Bulk(new String(buffer,StandardCharsets.UTF_8));
                }
                case'*' -> {
                    int size = Integer.parseInt(readLine());
                    if (size == -1) yield new RespValue.Array(null);
                    
                    List<RespValue> elements =  new ArrayList<>(size);
                    for(int i=0;i<size;i++){
                        elements.add(parse());
                    }
                    yield new RespValue.Array(elements);
                }
                default -> throw new IOException("Unknown RESP type: " + (char) b);
                
            };
    }
    
}
package com.miniRedis.persistence;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AofWriter {
    private final BufferedWriter writer;

    public AofWriter(String path) throws IOException {
        this.writer = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(path, true),
                StandardCharsets.UTF_8
            )
        );
    }
  public void append(List<String> tokens) {
    try {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(tokens.size()).append("\r\n");
        for (String token : tokens) {
            sb.append("$").append(token.length()).append("\r\n");
            sb.append(token).append("\r\n");
        }
        writer.write(sb.toString());
        writer.flush();
    } catch (IOException e) {
        throw new RuntimeException("Failed to append to AOF file", e);
    }
}



}

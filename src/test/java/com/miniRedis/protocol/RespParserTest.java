package com.miniRedis.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RespParserTest {

    private RespParser createParser(String input) {
        return new RespParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parseSimpleString() throws IOException {
        RespParser parser = createParser("+OK\r\n");
        RespValue value = parser.parse();

        assertInstanceOf(RespValue.SimpleString.class, value);
        assertEquals("OK", ((RespValue.SimpleString) value).value());
    }

    @Test
    void parseError() throws IOException {
        RespParser parser = createParser("-ERR unknown command\r\n");
        RespValue value = parser.parse();

        assertInstanceOf(RespValue.Error.class, value);
        assertEquals("ERR unknown command", ((RespValue.Error) value).message());
    }

    @Test
    void parseInteger() throws IOException {
        RespParser parser = createParser(":1000\r\n");
        RespValue value = parser.parse();

        assertInstanceOf(RespValue.Integer.class, value);
        assertEquals(1000L, ((RespValue.Integer) value).value());
    }

    @Test
    void parseBulkString() throws IOException {
        RespParser parser = createParser("$5\r\nhello\r\n");
        RespValue value = parser.parse();

        assertInstanceOf(RespValue.Bulk.class, value);
        assertEquals("hello", ((RespValue.Bulk) value).value());
    }

    @Test
    void parseNullBulkString() throws IOException {
        RespParser parser = createParser("$-1\r\n");
        RespValue value = parser.parse();

        assertInstanceOf(RespValue.Bulk.class, value);
        assertNull(((RespValue.Bulk) value).value());
    }

    @Test
    void parseArrayCommand() throws IOException {
        RespParser parser = createParser("*2\r\n$3\r\nGET\r\n$4\r\nuser\r\n");
        RespValue value = parser.parse();

        assertInstanceOf(RespValue.Array.class, value);
        RespValue.Array array = (RespValue.Array) value;
        assertEquals(2, array.value().size());

        assertEquals("GET", ((RespValue.Bulk) array.value().get(0)).value());
        assertEquals("user", ((RespValue.Bulk) array.value().get(1)).value());
    }
}

package com.miniRedis.protocol;

import java.util.List;

public sealed interface RespValue{


    record SimpleString(String value) implements RespValue{};


   record Error(String message) implements RespValue{};


    record Integer(Long value) implements RespValue{};


    record Bulk(String value)implements RespValue{};


    record Array(List<RespValue> value) implements RespValue{};
    
}

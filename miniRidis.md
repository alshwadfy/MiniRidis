# MiniRedis: Architecture

A Redis-compatible in-memory key-value server written in plain Java 21 (no Spring).
It speaks the real RESP protocol, so `redis-cli` and Jedis work against it unchanged.

## Goals

- Understand what sits under frameworks: sockets, protocol parsing, concurrency, durability.
- Be verifiable: anyone can run it and talk to it with `redis-cli`.
- Be measurable: benchmark against real Redis with `redis-benchmark`.

## Non-goals

Pub/sub, replication, clustering, transactions, Lua, data types beyond strings.

## High-level flow

```
redis-cli / Jedis
      |  TCP :6379 (RESP bytes)
      v
 [ Server ]  accepts connections, one virtual thread per client
      v
 [ RespParser ]  bytes -> command (array of bulk strings)
      v
 [ CommandDispatcher ]  name -> handler
      v
 [ Store ]  ConcurrentHashMap<String, Entry>   <--  [ ExpirySweeper ] (background)
      v
 [ AofWriter ]  appends write commands to disk
      v
 [ RespWriter ]  result -> RESP bytes -> client
```

On startup: `AofReplayer` reads the append-only file and re-executes it against the Store before the server starts accepting clients.

## Package layout

```
miniredis/
  Main.java
  server/
    Server.java            accept loop
    ClientHandler.java     read -> parse -> dispatch -> write, per connection
  protocol/
    RespValue.java         sealed interface: SimpleString, Error, Integer, Bulk, Array
    RespParser.java        stream -> RespValue (handles partial / pipelined input)
    RespWriter.java        RespValue -> bytes
  command/
    Command.java           interface: RespValue execute(List<String> args, Store store)
    CommandDispatcher.java registry: "SET" -> SetCommand, ...
    PingCommand, SetCommand, GetCommand, DelCommand, ...
  store/
    Store.java             get/set/delete/expire, thread-safe
    Entry.java             value + expiresAtMillis (absolute, nullable)
    ExpirySweeper.java     periodic removal of expired keys
  persistence/
    AofWriter.java         append + fsync policy
    AofReplayer.java       rebuild state on startup
```

## Key design decisions

| Decision | Choice | Why |
|---|---|---|
| Concurrency model | Virtual thread per connection | Simple, scales well, real concurrency problems to solve |
| Storage | `ConcurrentHashMap` + `compute()` | Atomic read-modify-write for `INCR` without a global lock |
| Expiry | Lazy (on read) + background sweeper | Lazy alone leaks memory for keys never read again |
| Expiry timestamps | Absolute epoch millis | Relative TTLs in the AOF would revive keys with a fresh TTL after restart |
| Persistence | Append-only file of write commands | Easy to reason about, easy to replay |
| fsync policy | `everysec` (default), configurable | Durability vs. speed tradeoff; document the choice in the README |
| Parser | Incremental, buffer-based | TCP has no message boundaries; commands arrive split or glued together |

## Commands (v1)

`PING`, `SET key value [EX seconds]`, `GET`, `DEL`, `EXISTS`, `EXPIRE`, `TTL`, `INCR`, `INFO`

## Error handling rules

- Unknown command: reply `-ERR unknown command 'X'`, keep the connection open.
- Wrong arg count: reply `-ERR wrong number of arguments for 'X'`.
- Malformed RESP: reply an error and close that connection only; never crash the server.
- Half-written last AOF line on replay: ignore it and log a warning.

## Milestones

1. **M1: PING over real TCP.** `redis-cli PING` returns `PONG`.
2. **M2: SET / GET / DEL / EXISTS** in memory.
3. **M3: EXPIRE / TTL / SET EX / INCR**, plus the sweeper and concurrency tests.
4. **M4: AOF persistence** and crash-recovery test.
5. **M5: Benchmark + README + CI.** Numbers vs. real Redis.
6. **Stretch:** replace thread-per-connection with an NIO event loop and compare benchmarks.

## Testing strategy

- **Unit:** RESP parser (fragmented input, pipelined commands, malformed input), Store, expiry logic.
- **Concurrency:** N threads calling `INCR` on one key; final value must equal total increments.
- **Integration:** start the server on a random port, drive it with Jedis.
- **Crash:** write data, `kill -9`, restart, verify data and TTLs.
- **CI:** GitHub Actions running `mvn -B verify`.

## Benchmark plan

Run `redis-benchmark -t set,get -n 100000 -c 50` against MiniRedis and real Redis.
Record throughput and p50/p95/p99 in the README. Re-run after each major change.
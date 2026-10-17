# MiniRedis

A Redis-compatible in-memory key-value store written from scratch in Java 21 without external networking frameworks or libraries.

I built this project to understand the low-level mechanics that frameworks usually hide: raw TCP socket handling, incremental protocol parsing, thread-safety under concurrent load, and append-only persistence. Because it implements the standard Redis Serialization Protocol (RESP), you can interact with it directly using `redis-cli`, Jedis, or any other official Redis client.

---

## Table of Contents

- [How It Works](#how-it-works)
- [Supported Commands](#supported-commands)
- [Design Decisions & Trade-offs](#design-decisions--trade-offs)
- [Building & Running](#building--running)
- [Verifying Persistence](#verifying-persistence)
- [Project Layout](#project-layout)

---

## How It Works

At a high level, the server runs a standard accept loop and hands each client off to a dedicated virtual thread.

```text
redis-cli / Client
       |
       |  TCP (port 6379)
       v
 [ Server ]  Accepts connection -> assigns to a Virtual Thread
       v
 [ ClientHandler ]  Manages socket read/write loop
       |
       +---> [ RespParser ]        Reads bytes from socket, parses RESP array
       |
       +---> [ CommandDispatcher ] Matches command name to its implementation
       |
       +---> [ Store ]             Thread-safe storage (ConcurrentHashMap)
       |       ^
       |       +--- [ ExpirySweeper ] (Background daemon cleaning expired keys)
       |
       +---> [ AofWriter ]         Appends successful write commands to disk
       |
       +---> [ RespWriter ]        Serializes response back to RESP bytes
```

### Crash Recovery Flow

When the server starts, it checks for an existing `aof.txt` before opening the TCP port:

```text
Server Startup
      |
      v
[ AofReplayer ]
      |-- Checks if aof.txt exists (if not, starts with empty store)
      |-- Reads each stored RESP command sequentially
      \-- Dispatches commands to Store to restore pre-crash state
      |
      v
[ ExpirySweeper ] starts running in background
      |
      v
[ Server ] binds to port 6379 and starts accepting client traffic
```

---

## Supported Commands

| Command | Syntax | Description | Recorded to AOF? |
|---|---|---|:---:|
| **PING** | `PING [msg]` | Checks connection or echoes message | No |
| **SET** | `SET key value [EX sec]` | Stores a string, with optional TTL | Yes |
| **GET** | `GET key` | Retrieves a value (or nil if expired/missing) | No |
| **DEL** | `DEL key` | Removes a key | Yes |
| **EXISTS** | `EXISTS key` | Checks if a key exists and is not expired | No |
| **INCR** | `INCR key` | Increments an integer string by 1 atomically | Yes |
| **EXPIRE** | `EXPIRE key sec` | Sets a timeout in seconds on an existing key | Yes |
| **TTL** | `TTL key` | Returns remaining seconds (-1 if persistent, -2 if missing) | No |

---

## Design Decisions & Trade-offs

### Virtual Threads (Project Loom)
Rather than setting up an asynchronous NIO event loop with callbacks or configuring fixed thread pools, each incoming socket is handled by a Java 21 virtual thread. This keeps the socket read/write logic straightforward and blocking, while remaining lightweight enough to handle thousands of concurrent client connections without exhausting OS threads.

### Concurrency Without Global Locks
All key-value data lives in a `ConcurrentHashMap<String, Entry>`.
- Read operations (`GET`, `EXISTS`, `TTL`) do not take locks.
- `INCR` uses `ConcurrentHashMap.compute()` to read, increment, and write back the integer atomically in place, preventing race conditions when multiple clients increment the same counter at the same time.

### Expiry Strategy: Lazy + Active Sweeper
Relying only on lazy deletion (checking expiry on `GET`) leaves keys that are never queried again sitting in memory indefinitely.
- **Lazy check**: Handled inside `Store.get()`. If the current time is past `expiresAtMillis`, the key is removed immediately and `null` is returned.
- **Active sweep**: An `ExpirySweeper` thread runs once every second, iterating through the map and removing keys whose expiry timestamp has passed.
- **Absolute timestamps**: Expiry is stored as epoch milliseconds (`System.currentTimeMillis() + ttlMillis`). If relative TTLs were written to disk, restarting the server would reset the countdown.

### Persistence: Append-Only File (AOF)
To keep durability simple and inspectable:
- Write commands (`SET`, `DEL`, `INCR`, `EXPIRE`) are serialized back into standard RESP arrays and written to `aof.txt`.
- `AofWriter.append` is marked `synchronized` to ensure virtual threads don't interleave byte writes when running concurrently.
- `AofReplayer` runs once at startup before sockets are accepted, feeding the file through the exact same `CommandDispatcher` used by live clients.

---

## Building & Running

### Prerequisites
- Java 21 or later
- Maven (or use the included `./mvnw`)

### 1. Build the project
```bash
./mvnw clean compile
```

### 2. Start the server
Run `DemoApplication`:
```bash
./mvnw exec:java -Dexec.mainClass="com.miniRedis.demo.DemoApplication"
```
Or run `DemoApplication.java` directly in your IDE. The server will bind to port `6379`.

### 3. Connect with redis-cli
You can connect using the official Redis CLI:
```bash
redis-cli -p 6379
```

Example session:
```text
127.0.0.1:6379> PING
PONG

127.0.0.1:6379> SET user:name "Ahmed"
OK

127.0.0.1:6379> GET user:name
"Ahmed"

127.0.0.1:6379> INCR visits
(integer) 1

127.0.0.1:6379> SET temp_token "abc" EX 15
OK

127.0.0.1:6379> TTL temp_token
(integer) 13
```

---

## Verifying Persistence

To confirm crash recovery works:

1. Insert some test data:
   ```bash
   redis-cli -p 6379 SET mykey "hello"
   redis-cli -p 6379 INCR mycounter
   ```
2. Stop the server (`Ctrl+C`).
3. Open `aof.txt` in the root folder to see your commands saved in standard RESP format.
4. Restart the server.
5. Query your keys again:
   ```bash
   redis-cli -p 6379 GET mykey
   redis-cli -p 6379 GET mycounter
   ```
The keys will still be there.

---

## Project Layout

```text
src/main/java/com/miniRedis/
├── demo/
│   └── DemoApplication.java       # Main entry point
├── server/
│   ├── Server.java                # ServerSocket accept loop & startup wiring
│   └── ClientHandler.java         # Per-connection virtual thread handler
├── protocol/
│   ├── RespValue.java             # Sealed interface for RESP data types
│   ├── RespParser.java            # Incremental byte-stream parser
│   └── RespWriter.java            # RESP byte serializer
├── command/
│   ├── Command.java               # Command interface
│   ├── CommandDispatcher.java     # Command name mapping and execution
│   ├── PingCommand.java
│   ├── SetCommand.java
│   ├── GetCommand.java
│   ├── DelCommand.java
│   ├── IncrCommand.java
│   ├── ExistsCommand.java
│   ├── ExpireCommand.java
│   └── TtlCommand.java
├── store/
│   ├── Store.java                 # In-memory storage backed by ConcurrentHashMap
│   ├── Entry.java                 # Value and expiration timestamp
│   └── ExpirySweeper.java         # Background cleanup task
└── persistence/
    ├── AofWriter.java             # Thread-safe RESP append logger
    └── AofReplayer.java           # Startup AOF parser and replay
```
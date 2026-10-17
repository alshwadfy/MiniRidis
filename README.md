# MiniRedis 🚀

A lightweight, high-performance, Redis-compatible in-memory key-value server built from scratch in **pure Java 21** using **Virtual Threads** and raw TCP sockets. No Spring, no external network frameworks — just core Java, low-level socket handling, and concurrency primitives.

MiniRedis speaks the official **RESP (Redis Serialization Protocol)**, allowing any standard Redis client (like `redis-cli`, Jedis, Lettuce, or redis-py) to interact with it seamlessly without changes.

---

## 📑 Table of Contents

- [Features](#-features)
- [Architecture & Request Flow](#-architecture--request-flow)
- [Supported Commands](#-supported-commands)
- [Key Engineering Decisions](#-key-engineering-decisions)
- [Getting Started](#-getting-started)
- [Project Structure](#-project-structure)
- [Roadmap](#-roadmap)

---

## ✨ Features

- **Protocol Compatibility**: Full custom RESP parser & writer (supporting Bulk Strings, Simple Strings, Integers, Arrays, and Errors).
- **Modern Concurrency**: Leverages Java 21 **Virtual Threads** (Project Loom) to achieve high throughput with a lightweight thread-per-connection model.
- **Lock-Free Read Operations**: High-concurrency in-memory storage using `ConcurrentHashMap` with atomic `compute()` operations for atomic updates (`INCR`).
- **Hybrid Expiry Engine**:
  - **Lazy Expiration**: Expired keys are evicted on-demand when accessed.
  - **Active Sweeper**: Background sweeper thread periodically cleans up abandoned expired keys.
  - **Epoch Timestamps**: Uses absolute epoch milliseconds to ensure expiration survives server restarts.
- **AOF (Append-Only File) Persistence**:
  - Automatically records mutating commands (`SET`, `DEL`, `INCR`, `EXPIRE`) to disk in standard RESP format.
  - Auto-replays commands on server startup to fully rebuild state after a crash or restart.

---

## 🏛 Architecture & Request Flow

```text
redis-cli / Client Application
       |
       |  TCP Connection (port 6379)
       v
 [ Server ]  Accepts connections -> Spawns 1 Virtual Thread per client
       v
 [ ClientHandler ]  Manages socket I/O loop
       |
       +---> [ RespParser ]        Decodes incoming raw byte stream into RESP tokens
       |
       +---> [ CommandDispatcher ] Routes command name to handler
       |
       +---> [ Store ]             ConcurrentHashMap with atomic operations
       |       ^
       |       +--- [ ExpirySweeper ] (Background daemon cleaning expired keys)
       |
       +---> [ AofWriter ]         Persists successful write commands to disk
       |
       +---> [ RespWriter ]        Encodes responses and flushes back to TCP socket
```

### Crash Recovery Flow
```text
Server Startup
      ↓
[ AofReplayer ]  Reads aof.txt (if present)
      ↓
Parses RESP commands and dispatches into Store (rebuilding state)
      ↓
[ ExpirySweeper ]  Starts background expiry loop
      ↓
Server binds to port 6379 and starts accepting client connections
```

---

## 💻 Supported Commands

| Command | Syntax | Description | Mutates State (AOF)? |
|---|---|---|:---:|
| **PING** | `PING [message]` | Tests connection liveness or echoes message | ❌ |
| **SET** | `SET key value [EX seconds]` | Sets key to string value with optional TTL | ✅ |
| **GET** | `GET key` | Retrieves the value of a key (or nil if expired/missing) | ❌ |
| **DEL** | `DEL key` | Deletes a key from the store | ✅ |
| **EXISTS** | `EXISTS key` | Checks if a key exists and has not expired | ❌ |
| **INCR** | `INCR key` | Atomically increments numeric value by 1 | ✅ |
| **EXPIRE** | `EXPIRE key seconds` | Sets a timeout (TTL in seconds) on an existing key | ✅ |
| **TTL** | `TTL key` | Returns remaining time to live in seconds (-1 if no TTL, -2 if missing) | ❌ |

---

## 🧠 Key Engineering Decisions

### 1. Concurrency: Virtual Threads (Project Loom)
Instead of relying on heavy OS thread pools or complex asynchronous callback chains, MiniRedis spawns a virtual thread for each client connection (`Thread.startVirtualThread(...)`). Virtual threads are managed by the JVM and consume minimal RAM, allowing thousands of simultaneous open connections with clean, straightforward blocking socket code.

### 2. Lock-Free Atomic State (`ConcurrentHashMap`)
To prevent global synchronization bottlenecks, state is kept in a `ConcurrentHashMap<String, Entry>`.
- Read operations (`GET`, `EXISTS`, `TTL`) run lock-free.
- Read-modify-write commands like `INCR` utilize `ConcurrentHashMap.compute()` to atomically parse, update, and persist values without acquiring a global lock.

### 3. Expiration: Absolute Milliseconds & Hybrid Sweeping
TTLs in Redis must be durable. MiniRedis stores expiration as an absolute timestamp (`System.currentTimeMillis() + ttlMillis`).
- **Why absolute time?** If relative TTLs were persisted to AOF, a restart would give keys a fresh expiration period.
- **Hybrid Sweeping**: Lazy expiry on `get()` prevents serving stale data, while `ExpirySweeper` prevents memory leaks caused by keys that are never read again.

### 4. Durability & AOF Replay
Every mutating command executed successfully is passed to `AofWriter`, which appends it in standard RESP array format (`*<argc>\r\n$<len>\r\n<arg>...`).
On server startup, `AofReplayer` scans the AOF file sequentially and feeds each saved command through `CommandDispatcher`, restoring the database to its exact pre-shutdown state before any client connection is accepted.

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** or later
- **Maven** (or use the included `./mvnw` wrapper)
- **redis-cli** (optional, for interacting with the server)

### 1. Build the Project
```bash
./mvnw clean compile
```

### 2. Run the Server
Run the `DemoApplication` entry point:
```bash
./mvnw exec:java -Dexec.mainClass="com.miniRedis.demo.DemoApplication"
```
Or run directly from your IDE by executing `DemoApplication.java`.

The server will initialize AOF recovery, start the background sweeper, and listen on port **`6379`**.

### 3. Connect with `redis-cli`
Open a terminal and connect:
```bash
redis-cli -p 6379
```

Try some commands:
```text
127.0.0.1:6379> PING
PONG

127.0.0.1:6379> SET user:100 "Antigravity"
OK

127.0.0.1:6379> GET user:100
"Antigravity"

127.0.0.1:6379> INCR visits
(integer) 1
127.0.0.1:6379> INCR visits
(integer) 2

127.0.0.1:6379> SET session "active" EX 10
OK
127.0.0.1:6379> TTL session
(integer) 9
```

### 4. Verify AOF Persistence
1. Insert some keys in `redis-cli`:
   ```bash
   SET app "MiniRedis"
   INCR counter
   ```
2. Stop the server process (Ctrl+C).
3. Observe `aof.txt` created in the root directory containing the serialized commands.
4. Restart the server.
5. In `redis-cli`, run `GET app` or `GET counter` — your data was preserved!

---

## 📂 Project Structure

```text
src/main/java/com/miniRedis/
├── demo/
│   └── DemoApplication.java       # Server entry point
├── server/
│   ├── Server.java                # TCP ServerSocket loop & startup lifecycle
│   └── ClientHandler.java         # Connection handler (Virtual Thread per client)
├── protocol/
│   ├── RespValue.java             # Sealed hierarchy of RESP data types
│   ├── RespParser.java            # Incremental byte stream decoder
│   └── RespWriter.java            # RESP encoder and serializer
├── command/
│   ├── Command.java               # Command execution interface
│   ├── CommandDispatcher.java     # Command registry & router
│   ├── PingCommand.java           # PING implementation
│   ├── SetCommand.java            # SET with optional EX
│   ├── GetCommand.java            # GET implementation
│   ├── DelCommand.java            # DEL implementation
│   ├── IncrCommand.java           # Atomic INCR implementation
│   ├── ExistsCommand.java         # EXISTS implementation
│   ├── ExpireCommand.java         # EXPIRE implementation
│   └── TtlCommand.java            # TTL implementation
├── store/
│   ├── Store.java                 # Thread-safe in-memory key-value store
│   ├── Entry.java                 # Stored record (value + absolute expiry)
│   └── ExpirySweeper.java         # Periodic background sweeper thread
└── persistence/
    ├── AofWriter.java             # Thread-safe RESP append-only logger
    └── AofReplayer.java           # Crash-recovery state rebuilder
```

---

## 🗺 Roadmap

- [x] **Milestone 1**: TCP Server & RESP protocol implementation (`PING`).
- [x] **Milestone 2**: Basic in-memory storage (`SET`, `GET`, `DEL`, `EXISTS`).
- [x] **Milestone 3**: Key expiry engine (`EXPIRE`, `TTL`, `SET EX`), background sweeper, and atomic `INCR`.
- [x] **Milestone 4**: AOF persistence engine (`AofWriter`, `AofReplayer`, crash recovery).
- [ ] **Milestone 5**: Performance benchmarking with `redis-benchmark` against real Redis.
- [ ] **Stretch Goal**: AOF Compaction (Rewrite) to prevent unbounded file growth.
- [ ] **Stretch Goal**: Java NIO Non-blocking Event Loop comparison.
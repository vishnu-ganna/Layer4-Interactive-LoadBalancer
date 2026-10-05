# High-Concurrency Interactive Layer-4 Network Load Balancer

A high-performance Software Network Load Balancer built from scratch in Java using raw TCP socket primitives. The system combines a multi-threaded data plane with an interactive command-line Control Plane (CLI) for real-time server management.

## 🧱 Core Architectural Pillars

### 1. Computer Networks — Data Plane

- **Raw Socket Networking:** Built using Java `ServerSocket` and `Socket` APIs for Layer-4 TCP communication.
- **TCP Proxying:** Accepts incoming client connections and forwards traffic to backend servers.
- **Dynamic Backend Selection:** Supports multiple backend servers with configurable scheduling policies.

### 2. Operating Systems — Concurrency

- **Multi-Threading:** Each incoming client connection can be handled by an independent worker thread.
- **Atomic Operations:** Uses `AtomicInteger` for thread-safe Round-Robin scheduling.
- **Concurrent Processing:** Multiple client requests can be processed simultaneously.

### 3. Distributed Systems — Fault Tolerance

- **Interactive Control Plane:** Provides a CLI for managing backend servers while the load balancer is running.
- **Thread-Safe Server Pool:** Uses thread-safe data structures for managing backend servers.
- **Server Failure Simulation:** Allows backend servers to be removed and restored at runtime.

---

## 🛠️ System Architecture

```text
                 INBOUND CLIENT
                       |
                       v
             +--------------------+
             | ServerSocket :8080 |
             +---------+----------+
                       |
                       v
              +-----------------+
              | Master Listener |
              +--------+--------+
                       |
                       v
              +-----------------+
              | Worker Threads  |
              +--------+--------+
                       |
                       v
             +-------------------+
             | Scheduling Policy |
             +---------+---------+
                       |
            +----------+----------+
            |          |          |
            v          v          v
       +---------+ +---------+ +---------+
       | :8081   | | :8082   | | :8083   |
       | Backend | | Backend | | Backend |
       +---------+ +---------+ +---------+
            ^          ^          ^
            |          |          |
            +----------+----------+
                       |
                       ^
              +------------------+
              | Interactive CLI |
              | Control Plane   |
              +------------------+




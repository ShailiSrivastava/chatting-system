# 🚀 Antigravity Enterprise Real-Time Chat Platform

An industry-level, production-grade **Real-Time Chat & Telemetry Application** built entirely using **Core Java** (without relying on Spring Boot or external backend frameworks). 

The platform features a **Client-Server Architecture** operating over raw **TCP Socket Programming**, supporting concurrent clients via multithreading, custom object protocol serialization, SHA-256 salted password hashing, AES payload encryption, JDBC database persistence, design patterns, a modern desktop Java Swing GUI, and an interactive **Glassmorphic Web Preview Portal with Live System Metrics**.

---

## 🌟 Key Highlights & Placement-Ready Showcase Features

- ⚡ **Zero-Framework Core Java Backend**: Implemented using pure SE standard libraries (`java.net`, `java.io`, `java.util.concurrent`, `java.sql`, `javax.swing`, `javax.crypto`, `com.sun.net.httpserver`).
- 🔄 **Dual Engine Gateway Architecture**: Synchronizes raw **TCP Sockets** (Port `8888`) and **HTTP Web Gateway** (Port `8080`) to the exact same core Java services.
- 🧵 **Multithreaded Socket Server**: Uses an `ExecutorService` thread pool (50 worker threads) running a `Runnable` socket handler per client with thread-safe `ConcurrentHashMap` client registries.
- 📊 **Real-Time System Metrics Inspector**: Live server telemetry modal displaying active TCP clients, thread pool workers, JVM Heap usage in MB (`ManagementFactory.getMemoryMXBean()`), uptime, and live streaming terminal logs.
- 🔐 **Enterprise Security Layer**: Passwords hashed with SHA-256 + 16-byte random salt and 1000 key-stretching iterations. All SQL queries use `PreparedStatement` to prevent SQL Injection attacks. Optional **AES-128 Symmetric Payload Encryption** for chat content.
- 🎤 **Simulated Voice Notes & Equalizer**: Interactive voice notes audio player with play/pause triggers and animated frequency spectrum bars (`@keyframes eqPulse`).
- 🎨 **Dynamic Glassmorphic Theme Engine**: Ultra-modern glassmorphism design with `backdrop-filter: blur(25px)`, ambient glowing mesh background orbs, spring pop animations, and 3 dynamic themes (**Midnight**, **Neon**, **Emerald**).
- 📁 **File Sharing Capability**: Binary attachment uploads & downloads up to 50MB with server disk repository storage (`server_storage/`) and database metadata tracking.

---

## 📐 System Architecture Diagram

```
 ┌─────────────────────────────────────────┐      ┌─────────────────────────────────────────┐
 │            JAVA SWING CLIENT            │      │           GLASSMORPHIC WEB UI           │
 │  (Desktop GUI - Login/Register/Chat)    │      │    (Browser - http://localhost:8080)     │
 └────────────────────┬────────────────────┘      └────────────────────┬────────────────────┘
                      │ TCP Socket (Port 8888)                         │ HTTP / REST (Port 8080)
                      ▼                                                ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────┐
 │                                 CHAT SERVER GATEWAY                                      │
 │   ServerSocket Acceptor ───> ExecutorService Worker Pool ───> WebChatServer (HttpServer) │
 └────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                              │
 ┌────────────────────────────────────────────▼─────────────────────────────────────────────┐
 │                                   CORE JAVA SERVICES                                     │
 │    AuthService    │    UserService    │    MessageService    │    GroupService           │
 └────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                              │
 ┌────────────────────────────────────────────▼─────────────────────────────────────────────┐
 │                                     JDBC DAO LAYER                                       │
 │  UserDAO │ MessageDAO │ GroupDAO │ FileDAO │ SessionDAO │ NotificationDAO (Prepared Stmts) │
 └────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                              │ SQL
                                              ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────┐
 │                       DATABASE (Embedded H2 / MySQL / PostgreSQL)                        │
 └──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🏗️ Design Patterns Applied

| Pattern | Class / Component | Purpose |
|---|---|---|
| **Singleton** | `DatabaseConnectionManager`, `ServerConfig`, `ClientManager` | Thread-safe connection pooling, server configurations, and concurrent online client registry. |
| **DAO (Data Access Object)** | `UserDAO`, `MessageDAO`, `GroupDAO`, `FileDAO`, `SessionDAO` | Encapsulates all SQL CRUD operations using `PreparedStatement` to separate persistence from business logic. |
| **Factory Pattern** | `PacketFactory`, `UIComponentFactory` | Standardized object creation for network protocol packets and custom painted Swing UI controls. |
| **Observer Pattern** | `ServerEventListener` | Decouples TCP client packet reader thread from Swing UI components, triggering asynchronous UI updates. |

---

## 📂 Package Structure

```
c:\Users\shail\Downloads\JAVA PROJECR
├── src\
│   └── com\
│       └── chat\
│           ├── common\
│           │   ├── model\          # Domain entities (User, Message, ChatGroup, SharedFile, Enums)
│           │   ├── protocol\       # Network protocol (Packet, PacketType, PacketFactory)
│           │   └── util\           # Utilities (AESEncryptionUtil, PasswordHasher, ValidationUtil, FileUtil)
│           ├── server\
│           │   ├── config\         # Configurations (ServerConfig, DatabaseConfig)
│           │   ├── db\             # Database connection manager & schema initializer
│           │   ├── dao\            # DAO interfaces & JDBC implementations
│           │   ├── service\        # Core business services (AuthService, MessageService, UserService)
│           │   ├── network\        # Network infrastructure (ChatServer, ClientHandler, ClientManager, WebChatServer)
│           │   └── ServerMain.java # Server entry point
│           ├── client\
│           │   ├── network\        # Socket client (ChatClient)
│           │   ├── listener\       # Observer event listener interface (ServerEventListener)
│           │   ├── ui\             # Modern Swing GUI (LoginFrame, RegisterFrame, MainDashboardFrame)
│           │   └── ClientMain.java # Client GUI entry point
│           └── test\
│               └── SimpleUnitTestRunner.java # Automated test suite
├── lib\
│   └── h2.jar                      # Embedded H2 JDBC driver
├── schema.sql                      # Production SQL DDL Script (MySQL / PostgreSQL / H2)
├── compile.ps1 / compile.bat       # Compilation scripts
├── run-server.ps1 / run-server.bat # Server launch scripts
├── run-client.ps1 / run-client.bat # Desktop client launch scripts
└── run-tests.ps1 / run-tests.bat   # Unit test execution scripts
```

---

## 🗄️ Database Schema (`schema.sql`)

The database automatically initializes upon server launch (using embedded H2 in MySQL compatibility mode) and can also be deployed to production MySQL / PostgreSQL servers:

```sql
-- Core User Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'OFFLINE',
    bio VARCHAR(255) DEFAULT '',
    avatar_path VARCHAR(255) DEFAULT '',
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat Messages Table
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT,
    group_id BIGINT,
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    status VARCHAR(20) DEFAULT 'SENT',
    is_encrypted BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id)
);
```

---

## 🚀 How to Build and Run

### 1. Compile the Codebase
Open PowerShell in the project directory and run:
```powershell
.\compile.ps1
```
*(Or in CMD: `compile.bat`)*

---

### 2. Run Automated Unit Tests
Verify password hashing, AES encryption, validation rules, and packet serialization:
```powershell
.\run-tests.ps1
```
*(Output: **15 PASSED, 0 FAILED**)*

---

### 3. Launch Chat Server & Web Portal
Start the dual TCP Socket Server (Port `8888`) and Web Portal (Port `8080`):
```powershell
.\run-server.ps1
```

Once running, access the web platform at:
👉 **[http://localhost:8080/](http://localhost:8080/)**

---

### 4. Launch Desktop Swing Client
Open a second terminal window to start native desktop client instances:
```powershell
.\run-client.ps1
```
*(Launch multiple client instances to test 1-to-1 DMs and Group Chat)*

---

## 💻 Technical Interview Deep-Dive Questions

### Q1: How does the application handle concurrent socket connections without blocking?
**Answer**: `ChatServer` uses an `ExecutorService` thread pool with worker threads. When `serverSocket.accept()` receives a connection, it hands off the socket to a `ClientHandler` runnable task. Each handler runs a non-blocking reading loop for serialized `Packet` objects over `ObjectInputStream`. Active online connections are registered in `ClientManager` using thread-safe `ConcurrentHashMap`.

### Q2: How are user passwords secured?
**Answer**: Passwords are never stored as plain text. `PasswordHasher` generates a unique 16-byte random salt using `SecureRandom`. The password and salt are hashed with SHA-256 and stretched over 1000 iterations to protect against rainbow table and brute-force attacks.

### Q3: How is SQL Injection prevented?
**Answer**: All database access goes through the DAO layer (`UserDAOImpl`, `MessageDAOImpl`, etc.), which exclusively uses JDBC `PreparedStatement` with parameterized placeholders (`?`). User inputs are validated and sanitized before binding.

### Q4: How does offline message queueing work?
**Answer**: When a message is sent to an offline user, `MessageService` stores the message in the database with status `SENT`. When the recipient logs in, `ClientHandler` queries pending offline messages and auto-delivers them over the socket stream, updating the status to `DELIVERED`.

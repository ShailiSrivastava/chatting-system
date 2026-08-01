VIEW IT ON https://chatting-system-d0xt.onrender.com/


# 🚀 Antigravity Enterprise Real-Time Chat Platform

An industry-level, production-grade **Real-Time Chat & Telemetry Application** converted into a standard **Maven Project** with automated JUnit 5 unit testing, dual fat-JAR packaging, Maven Wrapper, and containerized Docker deployment ready for **Render**.

The platform features a **Client-Server Architecture** operating over raw **TCP Socket Programming**, supporting concurrent clients via multithreading, custom object protocol serialization, SHA-256 salted password hashing, AES payload encryption, JDBC database persistence, design patterns, a modern desktop Java Swing GUI, and an interactive **Glassmorphic Web Preview Portal with Live System Metrics**.

---

## 🌟 Key Highlights 

- 📦 **Standard Maven Architecture**: Fully converted to Maven standard directory layout (`src/main/java`, `src/test/java`, `src/main/resources`) with dependency management via `pom.xml`.
- 🌐 **Render Production-Ready**: Dynamic `PORT` environment variable binding (`0.0.0.0`), multi-stage production Docker containerization, and `render.yaml` blueprint.
- ⚡ **Zero-Framework Core Java Backend**: Implemented using pure SE standard libraries (`java.net`, `java.io`, `java.util.concurrent`, `java.sql`, `javax.swing`, `javax.crypto`, `com.sun.net.httpserver`).
- 🐳 **Docker Containerization & Compose**: Multi-stage Dockerfile packaging the Chat Server into a lightweight image (`eclipse-temurin:17-jre-alpine`) with non-root security execution.
- 🧪 **JUnit 5 Unit Test Suite**: Comprehensive automated unit testing framework integrated with Maven build lifecycle (`mvn test`).
- 🛠️ **Dual Executable Fat-JAR Packaging**: Automated creation of standalone executable JAR files in `target/`:
  - `chat-server.jar` (`com.chat.server.ServerMain`)
  - `chat-client.jar` (`com.chat.client.ClientMain`)
- 🔄 **Dual Engine Gateway Architecture**: Synchronizes raw **TCP Sockets** and **HTTP Web Gateway** (`0.0.0.0:$PORT`) to the exact same core Java services.
- 🧵 **Multithreaded Socket Server**: Uses an `ExecutorService` thread pool (50 worker threads) running a `Runnable` socket handler per client with thread-safe `ConcurrentHashMap` client registries.
- 📊 **Real-Time System Metrics Inspector**: Live server telemetry modal displaying active TCP clients, thread pool workers, JVM Heap usage in MB (`ManagementFactory.getMemoryMXBean()`), uptime, and live streaming terminal logs.
- 🔐 **Enterprise Security Layer**: Passwords hashed with SHA-256 + 16-byte random salt and 1000 key-stretching iterations. All SQL queries use `PreparedStatement` to prevent SQL Injection attacks. Optional **AES-128 Symmetric Payload Encryption** for chat content.
- 🎨 **Dynamic Glassmorphic Theme Engine**: Ultra-modern glassmorphism design with `backdrop-filter: blur(25px)`, ambient glowing mesh background orbs, spring pop animations, and 3 dynamic themes (**Midnight**, **Neon**, **Emerald**).

---

## 📐 System Architecture Diagram

```
 ┌─────────────────────────────────────────┐      ┌─────────────────────────────────────────┐
 │            JAVA SWING CLIENT            │      │           GLASSMORPHIC WEB UI           │
 │  (Desktop GUI - Login/Register/Chat)    │      │    (Browser - http://0.0.0.0:$PORT)     │
 └────────────────────┬────────────────────┘      └────────────────────┬────────────────────┘
                      │ TCP Socket (Port 8888)                         │ HTTP / REST (Port $PORT)
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

## 📂 Maven Project Structure

```
JAVA PROJECR/
├── pom.xml                                 # Maven Build & Dependency Configuration
├── Dockerfile                              # Production multi-stage Dockerfile for Render
├── render.yaml                             # Render Blueprint deployment configuration
├── docker-compose.yml                      # Docker Compose orchestrator
├── mvnw.cmd                                # Windows Maven Wrapper
├── .mvn/                                   # Maven Wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/chat/
│   │   │       ├── common/                 # Models, Protocols & AES/Hasher Utils
│   │   │       ├── server/                 # DAOs, Services, Configs & ServerMain
│   │   │       └── client/                 # Swing UI & ClientMain
│   │   └── resources/
│   │       └── schema.sql                  # Database Schema DDL
│   └── test/
│       └── java/
│           └── com/chat/test/
│               ├── ChatAppTest.java        # JUnit 5 Automated Unit Test Suite
│               └── SimpleUnitTestRunner.java # Standalone Test Runner
├── compile.ps1 / compile.bat               # Maven compilation wrapper
├── run-server.ps1 / run-server.bat         # Launch Chat Server JAR
├── run-client.ps1 / run-client.bat         # Launch Desktop Client JAR
└── run-tests.ps1 / run-tests.bat           # Run JUnit 5 test suite
```

---

## 🚀 How to Build and Deploy

### 1. Build Project with Maven
To compile the source code, run tests, and package executable fat-JARs in `target/`:
```bash
mvn clean package
```
*(Or use Maven Wrapper: `.\mvnw.cmd clean package`)*

Output artifacts:
- `target/chat-server.jar`
- `target/chat-client.jar`

---

### 2. Run Automated Unit Tests (JUnit 5)
Run all unit tests (Password Hashing, AES Encryption, Validation Rules, Packet Serialization):
```bash
mvn test
```
*(Or in PowerShell: `.\run-tests.ps1`)*

---

### 3. Launch Chat Server & Web Portal Locally
Start the Chat Server and Web Telemetry Portal:
```bash
java -jar target/chat-server.jar
```
*(Or in PowerShell: `.\run-server.ps1`)*

Once running, access the live Web Telemetry Portal at:
👉 **[http://localhost:8080/](http://localhost:8080/)**

---

### 4. Launch Desktop Client
Start instances of the Swing Desktop GUI Client:
```bash
java -jar target/chat-client.jar
```
*(Or in PowerShell: `.\run-client.ps1`)*

---

### 🌐 5. Render Cloud Deployment (Docker)

#### Option A: 1-Click Render Blueprint Deployment
1. Push your repository to GitHub.
2. Log in to [Render Dashboard](https://dashboard.render.com/).
3. Click **New +** -> **Blueprint**.
4. Connect your GitHub repository (`JAVA PROJECR`). Render will automatically detect `render.yaml` and provision the Docker Web Service!

#### Option B: Manual Render Docker Web Service
1. On Render Dashboard, click **New +** -> **Web Service**.
2. Select **Build and deploy from a Git repository**.
3. Choose Language: **Docker**.
4. Set Environment Variables:
   - `PORT`: `10000` (or leave default, Render injects `PORT` automatically).
5. Click **Deploy Web Service**. Render will execute the multi-stage `Dockerfile`, build `chat-server.jar`, bind `WebChatServer` to `0.0.0.0:$PORT`, and expose your application live on the web!

---

### 🐳 6. Local Docker & Docker Compose
Deploy the Chat Server locally via Docker Compose:
```bash
docker compose up -d --build
```

To view running container logs:
```bash
docker compose logs -f
```

To stop the container:
```bash
docker compose down
```

---

## 💻 Technical Interview Deep-Dive Questions

### Q1: How does the application handle dynamic cloud environment ports on platforms like Render?
**Answer**: `WebChatServer` reads the `PORT` environment variable injected by Render (`System.getenv("PORT")`), defaulting to `8080` if absent. It binds the `HttpServer` instance to `new InetSocketAddress("0.0.0.0", port)` so incoming cloud web requests on all network interfaces reach the Java application.

### Q2: How does the application handle concurrent socket connections without blocking?
**Answer**: `ChatServer` uses an `ExecutorService` thread pool with worker threads. When `serverSocket.accept()` receives a connection, it hands off the socket to a `ClientHandler` runnable task. Each handler runs a non-blocking reading loop for serialized `Packet` objects over `ObjectInputStream`. Active online connections are registered in `ClientManager` using thread-safe `ConcurrentHashMap`.

### Q3: How are user passwords secured?
**Answer**: Passwords are never stored as plain text. `PasswordHasher` generates a unique 16-byte random salt using `SecureRandom`. The password and salt are hashed with SHA-256 and stretched over 1000 iterations to protect against rainbow table and brute-force attacks.

### Q4: How is SQL Injection prevented?
**Answer**: All database access goes through the DAO layer (`UserDAOImpl`, `MessageDAOImpl`, etc.), which exclusively uses JDBC `PreparedStatement` with parameterized placeholders (`?`). User inputs are validated and sanitized before binding.

### Q5: How is Maven configured to output two separate executable JARs?
**Answer**: The `pom.xml` uses the `maven-assembly-plugin` with two execution blocks (`build-server-jar` and `build-client-jar`). Each execution specifies `jar-with-dependencies` descriptor and configures its respective manifest `mainClass` (`com.chat.server.ServerMain` and `com.chat.client.ClientMain`), outputting `chat-server.jar` and `chat-client.jar` in `target/`.

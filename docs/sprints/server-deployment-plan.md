# Server Deployment Plan (Railway + PostgreSQL)

## Objective
Deploy the Java Socket Server to Railway using PostgreSQL for persistent data. The approach focuses on simplicity, ensuring the server runs autonomously in the cloud while remaining accessible for client connections. 

We will keep the deployment layer as thin as possible, using industry-standard but beginner-friendly tools: Environment Variables, a Fat JAR build, and a basic `Procfile`.

---

## Task 1: Database Migration to PostgreSQL
To ensure data is not lost when the Railway server restarts, we will switch from local SQLite to a managed PostgreSQL database.

**Sub-tasks:**
1. **Update `pom.xml`:** Add the PostgreSQL JDBC driver dependency.
2. **Refactor `DBConnection.java`:**
   - Update the connection logic to read connection details from Environment Variables (which Railway provides automatically, like `PGHOST`, `PGUSER`, `PGPASSWORD`, `PGPORT`, `PGDATABASE` or a single `DATABASE_URL`).
   - *Optional but recommended:* Keep a fallback mechanism to SQLite if the environment variables are missing, so local development doesn't break.

## Task 2: Dynamic Port Binding
Cloud platforms assign network ports dynamically. A hardcoded port will cause the deployment to fail.

**Sub-tasks:**
1. **Update `Server.java`:** 
   - Modify the initialization logic to read the `PORT` environment variable.
   - Example: `String portEnv = System.getenv("PORT"); int port = (portEnv != null) ? Integer.parseInt(portEnv) : 12345;`

## Task 3: Build Configuration (Fat JAR)
Railway needs a single, executable file to run your application. 

**Sub-tasks:**
1. **Update `pom.xml`:** Add the `maven-shade-plugin`. This plugin packages your server code and all its dependencies (like the Postgres driver and Jackson) into one single `.jar` file.

## Task 4: Railway Execution Config
Railway needs to know *how* to start the application after building it.

**Sub-tasks:**
1. **Create a `Procfile`:** Add a file named `Procfile` (no extension) to the root directory.
2. **Define the start command:** Add the execution command for the Fat JAR (e.g., `web: java -jar target/auction-app-1.0-SNAPSHOT.jar`).

## Task 5: Railway Dashboard Setup (Infrastructure)
The actual deployment steps on the Railway website.

**Sub-tasks:**
1. **Create PostgreSQL Service:** Spin up a new PostgreSQL database in your Railway project.
2. **Deploy Code:** Link your GitHub repository and deploy the `server-deployment` branch.
3. **Configure TCP Proxy:** Since this is a raw Socket server (not HTTP), go to the app's Networking settings in Railway and generate a **TCP Proxy**. This will provide the public domain and port for your clients.

## Task 6: Client Integration
Once the server is live in the cloud, the client needs to know where to find it.

**Sub-tasks:**
1. **Update `ServerConnection.java`:** Replace the hardcoded `"localhost"` and `12345` with the public TCP Proxy Domain and Port provided by Railway in Task 5.
2. **Test Multi-threading/Concurrency:** Connect multiple clients locally to the cloud server to verify the `ExecutorService` thread pool handles concurrent cloud requests properly.

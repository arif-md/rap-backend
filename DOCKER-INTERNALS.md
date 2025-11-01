# Spring Boot Docker Internals

This document explains the internal mechanics of how Spring Boot applications run in Docker containers, specifically covering WAR file structure, layer extraction, and ENTRYPOINT configuration.

---

## 1. Spring Boot WAR Layer Extraction

### What is Layer Extraction?

When you run the layertools extraction command in the Dockerfile:

```dockerfile
RUN java -Djarmode=layertools -jar backend-0.0.1-SNAPSHOT.war extract
```

Spring Boot extracts the WAR file into separate directories that maintain **Spring Boot's expected structure**.

### Extracted Directory Structure

After extraction, you get these directories:

```
/app/target/
├── dependencies/
│   └── BOOT-INF/
│       └── lib/
│           ├── spring-boot-starter-web-3.5.5.jar
│           ├── spring-boot-starter-security-3.5.5.jar
│           ├── mssql-jdbc-12.10.1.jre11.jar
│           ├── mybatis-spring-boot-starter-3.0.5.jar
│           └── [100+ other dependency JARs]
│
├── spring-boot-loader/
│   └── org/
│       └── springframework/
│           └── boot/
│               └── loader/
│                   ├── launch/
│                   │   ├── WarLauncher.class
│                   │   ├── JarLauncher.class
│                   │   └── PropertiesLauncher.class
│                   └── [other loader classes]
│
├── snapshot-dependencies/
│   └── BOOT-INF/
│       └── lib/
│           └── [any SNAPSHOT version JARs]
│
└── application/
    ├── BOOT-INF/
    │   └── classes/
    │       └── x/
    │           └── y/
    │               └── z/
    │                   └── backend/
    │                       ├── BackendApplication.class
    │                       ├── controller/
    │                       │   └── HealthController.class
    │                       └── ServletInitializer.class
    ├── META-INF/
    │   ├── MANIFEST.MF
    │   └── maven/
    └── WEB-INF/
        └── [web application resources]
```

### Why This Structure?

Spring Boot maintains this structure to:

1. **Preserve WAR file organization**: `BOOT-INF/classes/` and `BOOT-INF/lib/` are standard Spring Boot WAR conventions
2. **Enable proper ClassLoading**: The `WarLauncher` knows to look for classes in `BOOT-INF/classes/` and JARs in `BOOT-INF/lib/`
3. **Maintain metadata**: `META-INF/MANIFEST.MF` contains critical information like `Start-Class`
4. **Support web features**: `WEB-INF/` contains servlet and web application resources

---

## 2. How Layers Merge in the Runtime Container

### Dockerfile Copy Operations

```dockerfile
# In runtime stage
WORKDIR /app

COPY --from=builder /app/target/dependencies/ ./
COPY --from=builder /app/target/spring-boot-loader/ ./
COPY --from=builder /app/target/snapshot-dependencies/ ./
COPY --from=builder /app/target/application/ ./
```

### Merged Directory Structure in `/app`

When these layers are copied to the same destination (`./` which is `/app`), Docker **merges** them into a unified directory structure:

```
/app/
├── BOOT-INF/
│   ├── classes/                              ← From application/
│   │   └── x/y/z/backend/
│   │       ├── BackendApplication.class
│   │       └── controller/
│   │           └── HealthController.class
│   └── lib/                                  ← From dependencies/ + snapshot-dependencies/
│       ├── spring-boot-starter-web-3.5.5.jar
│       ├── spring-boot-starter-security-3.5.5.jar
│       ├── mssql-jdbc-12.10.1.jre11.jar
│       ├── mybatis-spring-boot-starter-3.0.5.jar
│       └── [all other JARs merged here]
│
├── org/                                      ← From spring-boot-loader/
│   └── springframework/
│       └── boot/
│           └── loader/
│               └── launch/
│                   └── WarLauncher.class
│
├── META-INF/                                 ← From application/
│   ├── MANIFEST.MF
│   └── maven/
│
└── WEB-INF/                                  ← From application/
    └── [web resources]
```

### How the Merge Works

```
Step 1: COPY dependencies/
/app/
└── BOOT-INF/
    └── lib/
        └── [dependency JARs]

Step 2: COPY spring-boot-loader/
/app/
├── BOOT-INF/lib/
└── org/springframework/boot/loader/          ← Added

Step 3: COPY snapshot-dependencies/
/app/
├── BOOT-INF/
│   └── lib/                                  ← Merged with existing
│       └── [+ snapshot JARs]
├── org/springframework/boot/loader/
└── ...

Step 4: COPY application/
/app/
├── BOOT-INF/
│   ├── classes/                              ← Added
│   └── lib/
├── org/springframework/boot/loader/
├── META-INF/                                 ← Added
└── WEB-INF/                                  ← Added
```

### Why This Matters for Docker Layer Caching

Each `COPY` creates a separate Docker layer:

- **Layer 1** (dependencies): 85MB - Changes only when pom.xml changes
- **Layer 2** (spring-boot-loader): 10MB - Changes only when Spring Boot version changes
- **Layer 3** (snapshot-dependencies): 0-5MB - Changes when SNAPSHOT dependencies update
- **Layer 4** (application): 5MB - Changes every time you modify code

**Result**: When you change application code, Docker only needs to rebuild Layer 4 (5MB) instead of downloading everything (100MB).

---

## 3. Docker ENTRYPOINT Options: Pros and Cons

### Option 1: Shell Form (Implicit `sh -c`)

```dockerfile
ENTRYPOINT java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher
```

#### How It Works
Docker automatically wraps this in: `/bin/sh -c "java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher"`

#### Pros
✅ **Simple syntax**: No need for array notation or explicit shell  
✅ **Automatic variable expansion**: `$JAVA_OPTS` is automatically substituted  
✅ **Shell features available**: Can use pipes (`|`), redirection (`>`), and command chaining (`&&`)  

#### Cons
❌ **Shell is PID 1**: The shell process becomes PID 1, not your Java application  
❌ **Poor signal handling**: SIGTERM sent to container goes to shell, not Java (can cause unclean shutdowns)  
❌ **Zombie processes**: Child processes may not be properly reaped  
❌ **Extra process overhead**: Shell process consumes resources unnecessarily  

#### Process Tree
```
PID 1: /bin/sh -c "java $JAVA_OPTS ..."
  └─ PID 7: java org.springframework.boot.loader.launch.WarLauncher
```

#### When to Use
- Quick prototypes or development
- When you need complex shell logic in ENTRYPOINT
- **Not recommended for production**

---

### Option 2: Exec Form (No Variables)

```dockerfile
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.WarLauncher"]
```

#### How It Works
Executes the command directly without a shell. Each array element is a separate argument.

#### Pros
✅ **Java is PID 1**: Application receives signals directly from Docker  
✅ **Proper signal handling**: SIGTERM properly triggers graceful shutdown  
✅ **No zombie processes**: Kernel handles orphaned processes correctly  
✅ **Minimal overhead**: No shell process consuming resources  
✅ **Clean process tree**: Only your application runs  

#### Cons
❌ **No variable expansion**: `$JAVA_OPTS` would be treated as literal string  
❌ **Fixed arguments**: Cannot change JVM options without rebuilding image  
❌ **No shell features**: Cannot use pipes, redirection, or command substitution  
❌ **Inflexible**: Hard to customize at runtime  

#### Process Tree
```
PID 1: java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 org.springframework.boot.loader.launch.WarLauncher
```

#### When to Use
- Production deployments with fixed JVM configuration
- When you don't need runtime flexibility
- Maximum performance and clean signal handling required

---

### Option 3: Exec Form with `sh -c` (Hybrid Approach)

```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher"]
```

#### How It Works
Uses exec form (array) but explicitly invokes shell with `-c` flag to execute the command string.

#### Pros
✅ **Variable expansion**: Environment variables like `$JAVA_OPTS` are expanded  
✅ **Runtime flexibility**: Can override `JAVA_OPTS` via `docker run -e` or docker-compose  
✅ **Explicit control**: You explicitly choose the shell (sh, bash, etc.)  
✅ **Shell features available**: Can use command substitution, pipes if needed  
✅ **Better than shell form**: More explicit and predictable behavior  

#### Cons
⚠️ **Shell is still PID 1**: Shell process is the container's main process  
⚠️ **Signal forwarding needed**: Shell must forward signals to Java (works but not ideal)  
⚠️ **Extra process**: Shell process remains in memory  

#### Process Tree
```
PID 1: sh -c "java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher"
  └─ PID 7: java -XX:+UseContainerSupport ... org.springframework.boot.loader.launch.WarLauncher
```

#### When to Use
- **Production deployments requiring runtime flexibility** (most common)
- When you need to pass different JVM options per environment
- Balance between flexibility and proper containerization
- **Current approach in our Dockerfile** ✅

---

### Option 4: Custom Shell Script with `exec`

```dockerfile
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
```

**docker-entrypoint.sh**:
```bash
#!/bin/sh
set -e

# Custom initialization logic
echo "Starting Spring Boot application..."

# Use exec to replace shell with Java process
exec java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher
```

#### How It Works
1. Shell script runs as PID 1
2. Script performs initialization
3. `exec` **replaces** the shell process with Java process
4. Java becomes PID 1 (shell exits)

#### Pros
✅ **Java becomes PID 1**: The `exec` command replaces the shell  
✅ **Proper signal handling**: Java receives signals directly after `exec`  
✅ **Variable expansion**: Full shell capabilities for setup  
✅ **Initialization logic**: Can run pre-flight checks, config generation, etc.  
✅ **Maximum flexibility**: Custom logic + proper process management  
✅ **Best of all worlds**: Combines flexibility with proper containerization  

#### Cons
⚠️ **Extra file to maintain**: Shell script is a separate artifact  
⚠️ **More complexity**: Need to understand shell scripting  
⚠️ **Must use `exec`**: Forgetting `exec` negates the benefits  

#### Process Tree
```
# Before exec:
PID 1: /bin/sh /usr/local/bin/docker-entrypoint.sh
  └─ PID 7: [initialization commands]

# After exec:
PID 1: java -XX:+UseContainerSupport ... org.springframework.boot.loader.launch.WarLauncher
       (shell replaced, no longer exists)
```

#### When to Use
- Complex initialization requirements (config generation, health checks, migrations)
- Need both runtime flexibility AND proper signal handling
- **Best practice for production-grade containers**
- When you need to run multiple setup commands before starting the app

---

## Comparison Table

| Feature | Shell Form | Exec Form (No Vars) | Exec + `sh -c` | Custom Script + `exec` |
|---------|-----------|---------------------|----------------|------------------------|
| **Variable Expansion** | ✅ Yes | ❌ No | ✅ Yes | ✅ Yes |
| **Java as PID 1** | ❌ No | ✅ Yes | ❌ No | ✅ Yes (after exec) |
| **Proper Signals** | ❌ Poor | ✅ Perfect | ⚠️ OK | ✅ Perfect |
| **Runtime Flexibility** | ✅ High | ❌ None | ✅ High | ✅ Very High |
| **Complexity** | Low | Low | Low | Medium |
| **Production Ready** | ❌ No | ⚠️ Limited | ✅ Yes | ✅✅ Best |
| **Resource Overhead** | Medium | Low | Medium | Low (after exec) |
| **Zombie Prevention** | ❌ No | ✅ Yes | ⚠️ OK | ✅ Yes |

---

## Recommendations

### For Development
Use **Option 3** (Exec + `sh -c`):
```dockerfile
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher"]
```

### For Production (Simple)
Use **Option 3** (Exec + `sh -c`) - Good balance:
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher"]
```

### For Production (Advanced)
Use **Option 4** (Custom Script + `exec`) - Best practice:
```dockerfile
COPY docker-entrypoint.sh /usr/local/bin/
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
```

```bash
#!/bin/sh
set -e

# Pre-flight checks
if [ -z "$DATABASE_URL" ]; then
  echo "ERROR: DATABASE_URL not set"
  exit 1
fi

# Custom initialization
echo "Initializing application..."

# Replace shell with Java (becomes PID 1)
exec java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher
```

---

## Key Takeaways

1. **Spring Boot WAR Structure**: Layer extraction maintains `BOOT-INF/classes/`, `BOOT-INF/lib/`, and loader structure required by `WarLauncher`

2. **Layer Merging**: Multiple `COPY` commands merge directories into `/app`, creating the complete Spring Boot runtime structure

3. **ENTRYPOINT Choice**: 
   - Simple projects: Use `sh -c` for flexibility
   - Production: Use custom script with `exec` for proper PID 1 handling
   - Avoid: Shell form without exec notation

4. **Signal Handling Matters**: In Kubernetes/production, proper SIGTERM handling is critical for graceful shutdowns

5. **The `exec` Keyword**: In shell scripts, `exec` replaces the shell with your application, making it PID 1 and ensuring proper signal handling

---

## 4. How Docker Layers Are Built and Cached

### Understanding Docker Layers

Every instruction in a Dockerfile that modifies the filesystem creates a new **layer**. Layers are stacked on top of each other to form the final image.

### Layer Creation Process

```dockerfile
FROM eclipse-temurin:17-jdk-jammy AS builder    # Layer 1: Base image
WORKDIR /app                                     # Layer 2: Create /app directory
COPY mvnw mvnw.cmd pom.xml ./                   # Layer 3: Copy files
COPY .mvn .mvn                                   # Layer 4: Copy .mvn folder
RUN ./mvnw dependency:go-offline -B             # Layer 5: Download dependencies
COPY src ./src                                   # Layer 6: Copy source code
RUN ./mvnw package -DskipTests -B               # Layer 7: Build application
```

Each layer is:
- **Immutable**: Once created, it never changes
- **Identified by hash**: SHA256 hash of the layer content
- **Cached**: Stored on the host machine's disk
- **Reusable**: Can be shared across multiple images

### How Layer Hashing Works

#### For COPY/ADD Instructions

Docker calculates a hash based on:
1. **Instruction itself**: `COPY pom.xml ./`
2. **File contents**: SHA256 of each file being copied
3. **File metadata**: Permissions (mostly)
4. **Parent layer hash**: Hash of the previous layer

```
Example: COPY pom.xml ./

Hash Calculation:
  parent_layer_hash = "abc123..."
  instruction = "COPY pom.xml ./"
  file_content = SHA256(pom.xml contents) = "def456..."
  
  layer_hash = SHA256(
    parent_layer_hash + 
    instruction + 
    file_content
  ) = "xyz789..."
```

#### For RUN Instructions

Docker calculates a hash based on:
1. **Command string**: `RUN ./mvnw dependency:go-offline -B`
2. **Parent layer hash**: Hash of the previous layer
3. **NOT the output**: The result is not part of the hash!

```
Example: RUN ./mvnw dependency:go-offline -B

Hash Calculation:
  parent_layer_hash = "xyz789..."
  command = "RUN ./mvnw dependency:go-offline -B"
  
  layer_hash = SHA256(
    parent_layer_hash + 
    command
  ) = "uvw123..."
  
Note: The downloaded dependencies are NOT part of the hash!
```

### Layer Cache Lookup Algorithm

When Docker builds an image, for each instruction:

```
┌─────────────────────────────────────────────────────┐
│ Step 1: Calculate Hash                              │
├─────────────────────────────────────────────────────┤
│ - Read current instruction                          │
│ - Get parent layer hash                             │
│ - For COPY: Hash file contents                      │
│ - For RUN: Use command string                       │
│ - Compute SHA256 hash                               │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│ Step 2: Search Cache                                │
├─────────────────────────────────────────────────────┤
│ Query: Do I have a layer with this hash?           │
│                                                      │
│ Search locations:                                    │
│ - Local cache: /var/lib/docker/overlay2/           │
│ - BuildKit cache: /var/lib/docker/buildkit/        │
│ - Remote cache: Container registry (if configured)  │
└─────────────────────────────────────────────────────┘
                      ↓
           ┌──────────┴──────────┐
           │                     │
      CACHE HIT              CACHE MISS
           │                     │
           ↓                     ↓
┌──────────────────┐    ┌──────────────────┐
│ Use Cached Layer │    │ Execute Command  │
├──────────────────┤    ├──────────────────┤
│ - Skip execution │    │ - Create container│
│ - Reuse layer    │    │ - Run command    │
│ - Print:         │    │ - Capture changes│
│   "Using cache"  │    │ - Save as layer  │
│ - Continue to    │    │ - Store hash     │
│   next step      │    │ - BREAK CACHE    │
└──────────────────┘    │   for next steps │
                        └──────────────────┘
```

### Cache Invalidation Chain

Once a layer cache MISS occurs, **all subsequent layers must be rebuilt**:

```
Build 1 (Initial):
Step 3: COPY pom.xml ./           → Hash: abc123 → MISS → Execute → Cache
Step 4: RUN mvnw dependency...    → Hash: def456 → MISS → Execute → Cache
Step 5: COPY src ./src            → Hash: ghi789 → MISS → Execute → Cache
Step 6: RUN mvnw package          → Hash: jkl012 → MISS → Execute → Cache

Build 2 (Only source code changed):
Step 3: COPY pom.xml ./           → Hash: abc123 → HIT ✅ → Use cache
Step 4: RUN mvnw dependency...    → Hash: def456 → HIT ✅ → Use cache
Step 5: COPY src ./src            → Hash: mno345 → MISS ❌ → Execute (NEW hash!)
Step 6: RUN mvnw package          → Hash: pqr678 → MISS ❌ → Execute (parent changed!)

Build 3 (pom.xml changed):
Step 3: COPY pom.xml ./           → Hash: stu901 → MISS ❌ → Execute (file changed!)
Step 4: RUN mvnw dependency...    → Hash: vwx234 → MISS ❌ → Execute (parent changed!)
Step 5: COPY src ./src            → Hash: yza567 → MISS ❌ → Execute (parent changed!)
Step 6: RUN mvnw package          → Hash: bcd890 → MISS ❌ → Execute (parent changed!)
```

### Where Layers Are Stored

#### Local Storage (Default Docker)

```
/var/lib/docker/
├── image/
│   └── overlay2/
│       └── layerdb/
│           ├── sha256/
│           │   ├── abc123.../          ← Layer metadata
│           │   │   ├── cache-id
│           │   │   ├── diff
│           │   │   ├── parent
│           │   │   └── size
│           │   └── def456.../
│           └── mounts/
└── overlay2/
    ├── abc123.../                      ← Actual layer data
    │   ├── diff/                       ← Files in this layer
    │   ├── merged/
    │   └── work/
    └── def456.../
```

#### BuildKit Cache (Modern Docker)

```
/var/lib/docker/
└── buildkit/
    ├── cache/
    │   ├── maven-cache-abc123/         ← Persistent cache mount
    │   │   └── .m2/
    │   │       └── repository/
    │   │           └── [dependencies]
    │   └── npm-cache-def456/
    └── snapshots/
        └── snapshots.db                ← Layer metadata database
```

### Cache Search Example: Step by Step

Let's trace what happens when building our Dockerfile:

```dockerfile
COPY pom.xml ./
```

**Step-by-step process:**

```
1. Docker reads instruction: "COPY pom.xml ./"

2. Calculate hash:
   - Parent layer: "bbb222..." (from WORKDIR /app)
   - Read pom.xml file from host
   - Calculate SHA256(pom.xml content) = "aaa111..."
   - Combine: SHA256("bbb222" + "COPY pom.xml ./" + "aaa111") = "ccc333..."

3. Query cache database:
   SELECT layer_id FROM layers 
   WHERE hash = 'ccc333...' 
   AND parent = 'bbb222...'

4a. If FOUND (cache hit):
    - Print: "---> Using cache"
    - Set current_layer = "ccc333..."
    - Continue to next instruction
    - Time saved: Seconds to minutes!

4b. If NOT FOUND (cache miss):
    - Print: "---> Running in abc123..."
    - Create temporary container from parent layer
    - Copy pom.xml into container
    - Commit container as new layer
    - Calculate hash (verify it matches "ccc333...")
    - Store in cache database:
      INSERT INTO layers (hash, parent, path) 
      VALUES ('ccc333...', 'bbb222...', '/var/lib/docker/overlay2/ccc333...')
    - Set current_layer = "ccc333..."
```

### Why Layer Order Matters

**Bad Order (Cache inefficient):**
```dockerfile
COPY src ./src                    # Changes frequently
COPY pom.xml ./                   # Changes rarely
RUN ./mvnw dependency:go-offline  # Depends on pom.xml
```

Result: Every code change invalidates pom.xml and dependency layers!

**Good Order (Cache optimized):**
```dockerfile
COPY pom.xml ./                   # Changes rarely
RUN ./mvnw dependency:go-offline  # Cached when pom.xml unchanged
COPY src ./src                    # Changes frequently
```

Result: Code changes only invalidate the last layer!

### Multi-Stage Build Cache Behavior

In our Dockerfile with two stages:

```dockerfile
# Stage 1: Builder
FROM eclipse-temurin:17-jdk-jammy AS builder
COPY pom.xml ./                           # Builder layer 3
RUN ./mvnw dependency:go-offline          # Builder layer 4
COPY src ./src                            # Builder layer 5
RUN ./mvnw package                        # Builder layer 6

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy         # Runtime layer 1 (new base)
COPY --from=builder /app/target/deps ./   # Runtime layer 2
COPY --from=builder /app/target/app ./    # Runtime layer 3
```

**Important**: Each stage has its own layer stack. Layers from Stage 1 are NOT in the final image, only files copied via `COPY --from=builder`.

### Cache Optimization Patterns

#### Pattern 1: Dependency Layer Isolation

```dockerfile
# Good: Dependencies cached separately
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package
```

**Why**: `pom.xml` changes rarely → dependency download cached most of the time

#### Pattern 2: Multi-file Copy Ordering

```dockerfile
# Bad: One file change invalidates everything
COPY . /app

# Good: Group by change frequency
COPY package.json package-lock.json ./   # Changes rarely
RUN npm install                           # Cached when lock file unchanged
COPY src ./src                            # Changes frequently
```

#### Pattern 3: BuildKit Cache Mounts

```dockerfile
# Traditional: Cache only works within build
RUN mvn dependency:go-offline

# BuildKit: Cache persists across builds
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline
```

**Difference**: 
- Traditional: Cache in layer (deleted on `docker system prune`)
- BuildKit: Cache in persistent volume (survives cleanup)

### Viewing Layer Information

#### List layers in an image:
```bash
docker history backend:latest
```

Output:
```
IMAGE          CREATED        CREATED BY                                      SIZE
abc123...      2 hours ago    COPY /app/target/application/ ./                5MB
def456...      2 hours ago    COPY /app/target/dependencies/ ./              85MB
ghi789...      2 hours ago    WORKDIR /app                                    0B
```

#### Inspect layer details:
```bash
docker inspect backend:latest | jq '.[0].RootFS.Layers'
```

Output:
```json
[
  "sha256:abc123...",  // Base image layer
  "sha256:def456...",  // Dependencies layer
  "sha256:ghi789...",  // Application layer
]
```

#### Check cache usage:
```bash
docker system df -v
```

### Cache Storage Lifecycle

```
┌──────────────────────────────────────────────────┐
│ Layer Lifecycle                                  │
├──────────────────────────────────────────────────┤
│                                                  │
│ 1. BUILD: Layer created and stored in cache     │
│    └─ /var/lib/docker/overlay2/abc123/          │
│                                                  │
│ 2. USE: Layer reused by image                   │
│    └─ Reference count incremented               │
│                                                  │
│ 3. IMAGE DELETED: Layer reference count reduced │
│    └─ Layer marked as "dangling" if count = 0   │
│                                                  │
│ 4. PRUNE: Dangling layers cleaned up            │
│    └─ docker system prune                       │
│    └─ Frees disk space                          │
│                                                  │
└──────────────────────────────────────────────────┘
```

**Note**: BuildKit cache mounts persist even after `docker system prune` because they're stored separately from image layers.

---

## Further Reading

- [Docker ENTRYPOINT Documentation](https://docs.docker.com/engine/reference/builder/#entrypoint)
- [Spring Boot Layered JARs](https://docs.spring.io/spring-boot/docs/current/reference/html/container-images.html#container-images.efficient-images.layering)
- [PID 1 and Signal Handling in Containers](https://cloud.google.com/architecture/best-practices-for-building-containers#signal-handling)
- [Docker Layer Caching](https://docs.docker.com/build/cache/)
- [BuildKit Cache Mounts](https://docs.docker.com/build/guide/mounts/)

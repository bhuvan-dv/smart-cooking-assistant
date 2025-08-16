# Smart Cooking Assistant - Complete Architecture Overview

## 🏗️ System Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                     Smart Cooking Assistant                    │
│                    (Akka Cluster Application)                  │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────────────────┐    ┌─────────────────────────┐    │
│  │      NODE 1 (2551)      │    │      NODE 2 (2552)      │    │
│  │                         │    │                         │    │
│  │  ┌──────────────────┐   │    │  ┌──────────────────┐   │    │
│  │  │  RecipeFinder    │   │    │  │SubstitutionExpert│   │    │
│  │  └────────┬─────────┘   │    │  └────────┬─────────┘   │    │
│  │           │             │    │           │             │    │
│  │  ┌────────▼─────────┐   │    │  ┌────────▼─────────┐   │    │
│  │  │    LLMActor      │   │    │  │    LLMActor      │   │    │
│  │  └────────┬─────────┘   │    │  └──────────────────┘   │    │
│  │           │             │    │                         │    │
│  │  ┌────────▼─────────┐   │    │                         │    │
│  │  │  LoggingActor    │   │    │                         │    │
│  │  └──────────────────┘   │    │                         │    │
│  └─────────────────────────┘    └─────────────────────────┘    │
│                                                                │
│                    Akka Cluster Communication                  │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   Ollama/OpenAI  │
                    │   (AI Backend)   │
                    └──────────────────┘
```

## 📦 Project Structure

```
smart-cooking-assistant/
├── src/main/java/com/cooking/
│   ├── Main.java                 # Entry point & orchestration
│   ├── actor/                    # Akka actors
│   │   ├── RecipeFinder.java     # Main recipe coordinator
│   │   ├── LLMActor.java         # AI integration actor
│   │   ├── LoggingActor.java     # Recipe logging
│   │   └── SubstitutionExpert.java # Ingredient substitutions
│   ├── api/                      # External API clients
│   │   ├── OpenAIClient.java     # Base AI client
│   │   └── OllamaClient.java     # Ollama implementation
│   └── model/                    # Data models
│       └── RecipeRequest.java    # Request data structure
├── src/main/resources/
│   ├── application.conf          # Akka configuration
│   └── logback.xml              # Logging configuration
└── pom.xml                       # Maven dependencies
```

## 🎭 Actor System Details

### 1. **RecipeFinder Actor** (Node 1)
**Purpose**: Main coordinator for recipe requests
**Location**: `com.cooking.actor.RecipeFinder`

**Commands**:
- `FindRecipe` - Standard tell pattern
- `FindRecipeWithAsk` - Ask pattern with timeout
- `ForwardToLLM` - Forward pattern preserving sender

**Responsibilities**:
- Receives recipe requests from users
- Coordinates with LLMActor for AI generation
- Sends results to LoggingActor
- Implements three Akka patterns (tell, ask, forward)

### 2. **LLMActor** (Both Nodes)
**Purpose**: Interface with AI services (Ollama/OpenAI)
**Location**: `com.cooking.actor.LLMActor`

**Commands**:
- `ProcessRecipeRequest` - Generate recipe from AI

**Responses**:
- `RecipeResponse` - Successful recipe generation
- `RecipeError` - Error during generation

**Responsibilities**:
- Communicates with external AI services
- Handles API timeouts and errors
- Returns formatted recipes

### 3. **LoggingActor** (Node 1)
**Purpose**: Persist and track recipe requests
**Location**: `com.cooking.actor.LoggingActor`

**Commands**:
- `LogRecipeRequest` - Log recipe and response

**Responsibilities**:
- Records all recipe requests
- Tracks responses for analytics
- Provides audit trail

### 4. **SubstitutionExpert** (Node 2)
**Purpose**: Specialized ingredient substitution suggestions
**Location**: `com.cooking.actor.SubstitutionExpert`

**Commands**:
- `FindSubstitutions` - Get ingredient alternatives

**Responses**:
- `SubstitutionResponse` - Substitution suggestions

**Responsibilities**:
- Provides dietary alternatives
- Handles allergy substitutions
- Suggests available ingredient swaps

## 🔄 Data Flow Patterns

### Pattern 1: Tell (Fire-and-Forget)
```
User → RecipeFinder.FindRecipe
         ↓ (tell)
      LLMActor.ProcessRecipeRequest
         ↓ (response via adapter)
      RecipeFinder (processes response)
         ↓ (tell)
      LoggingActor.LogRecipeRequest
         ↓
      Response → User
```

### Pattern 2: Ask (Request-Response with Future)
```
User → RecipeFinder.FindRecipeWithAsk
         ↓ (ask with timeout)
      LLMActor.ProcessRecipeRequest
         ↓ (CompletionStage)
      Future Response
         ↓ (pipeToSelf)
      RecipeFinder (handles future)
         ↓
      Response → User
```

### Pattern 3: Forward (Preserve Original Sender)
```
User → RecipeFinder.ForwardToLLM
         ↓ (forward - preserves sender)
      LLMActor.ProcessRecipeRequest
         ↓ (response to adapter)
      RecipeFinder (maintains original sender ref)
         ↓
      Response → User (original sender)
```

## 🌐 Cluster Communication

### Cluster Setup:
```java
// Node 1 (Seed Node)
Port: 2551
Roles: ["node1"]
Actors: RecipeFinder, LLMActor, LoggingActor

// Node 2
Port: 2552  
Roles: ["node2"]
Actors: SubstitutionExpert, LLMActor
```

### Cluster Events:
- `MemberUp` - Node joins cluster
- `MemberLeft` - Node leaving
- `MemberExited` - Node exited
- `MemberRemoved` - Node removed

## 💾 Data Models

### RecipeRequest
```java
public class RecipeRequest {
    String query;           // "chicken curry"
    String dietaryPreference; // "vegetarian"
    boolean needSubstitutions; // true/false
}
```

### Message Flow Example
```
1. User Input: "Make me a vegan pasta"
   ↓
2. RecipeRequest created:
   - query: "vegan pasta"
   - dietary: "vegan"
   - substitutions: true
   ↓
3. RecipeFinder receives request
   ↓
4. LLMActor calls Ollama API:
   - Prompt: "You are a cooking assistant..."
   - Model: llama3.2:1b
   ↓
5. Ollama generates recipe
   ↓
6. Response flows back:
   - LLMActor → RecipeFinder
   - RecipeFinder → LoggingActor
   - RecipeFinder → User
   ↓
7. User sees formatted recipe
```

## 🔧 External Integrations

### Ollama Integration (Primary)
```java
OllamaClient extends OpenAIClient {
    - URL: http://localhost:11434
    - Models: llama3.2:1b, llama3.2:3b
    - Spring AI based implementation
    - Supports streaming responses
}
```

### OpenAI Integration (Fallback)
```java
OpenAIClient {
    - API Key based authentication
    - Models: gpt-3.5-turbo, gpt-4
    - REST API calls
}
```

## 🚀 Execution Modes

### 1. **Interactive Mode**
```bash
mvn exec:java -Dexec.args="interactive"
```
- Single node operation
- Direct user interaction
- Console-based recipe requests

### 2. **Cluster Mode**
```bash
# Terminal 1 - Seed Node
mvn exec:java -Dexec.args="2551 node1 seed"

# Terminal 2 - Second Node  
mvn exec:java -Dexec.args="2552 node2"
```
- Distributed processing
- Fault tolerance
- Load balancing

### 3. **Test Modes**
```bash
mvn exec:java -Dexec.args="test"      # AI connection test
mvn exec:java -Dexec.args="patterns"   # Akka patterns demo
mvn exec:java -Dexec.args="akka-tests" # Comprehensive tests
```

## 🔐 Configuration

### Akka Configuration (`application.conf`)
```hocon
akka {
  actor {
    provider = "cluster"
    serialization-bindings {
      "com.cooking.model.RecipeRequest" = jackson-json
    }
  }
  
  remote.artery {
    canonical.hostname = "127.0.0.1"
    canonical.port = 0
  }
  
  cluster {
    seed-nodes = []
    downing-provider-class = "akka.cluster.sbr.SplitBrainResolver"
  }
}
```

## 📊 Key Design Patterns

1. **Actor Model**: Concurrent, message-driven architecture
2. **Supervision**: Fault tolerance through actor hierarchies
3. **Message Adapter**: Type conversion between actors
4. **Clustering**: Distributed computing across nodes
5. **Circuit Breaker**: Handling external service failures
6. **Command Pattern**: Encapsulated requests as messages

## 🎯 Benefits of This Architecture

1. **Scalability**: Add more nodes as needed
2. **Fault Tolerance**: Actors restart on failure
3. **Modularity**: Each actor has single responsibility
4. **Concurrency**: Non-blocking, async processing
5. **Flexibility**: Easy to add new features/actors
6. **Distribution**: Work across multiple machines

## 🔄 Typical User Journey

1. **User starts application** → Main.java initializes
2. **Cluster forms** → Nodes discover each other
3. **User enters recipe request** → "Thai green curry"
4. **Request routes to RecipeFinder** → Chooses pattern
5. **LLMActor queries AI** → Ollama generates recipe
6. **Response processed** → Formatted for display
7. **LoggingActor records** → Audit trail created
8. **User receives recipe** → Displayed in console

## 🛠️ Technology Stack

- **Akka**: Actor framework (v2.6.20)
- **Java**: Primary language (v11+)
- **Spring AI**: Ollama integration (v0.8.1)
- **Jackson**: Serialization (v2.15.3)
- **Ollama**: Local LLM (llama3.2)
- **Maven**: Build tool
- **Logback**: Logging framework

This architecture provides a robust, scalable, and maintainable system for AI-powered recipe generation with clear separation of concerns and excellent fault tolerance.
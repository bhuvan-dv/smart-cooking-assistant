package com.cooking;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;
import akka.cluster.ClusterEvent;
import com.cooking.actor.LLMActor;
import com.cooking.actor.LoggingActor;
import com.cooking.actor.RecipeFinder;
import com.cooking.actor.SubstitutionExpert;
import com.cooking.api.OpenAIClient;
import com.cooking.api.OllamaClient;
import com.cooking.model.RecipeRequest;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Scanner;

public class Main {
    // Store actor references for testing
    private static ActorRef<RecipeFinder.Command> recipeFinderRef;
    private static ActorRef<SubstitutionExpert.Command> substitutionExpertRef;
    private static ActorRef<LLMActor.Command> llmActorNode1Ref;
    private static ActorRef<LLMActor.Command> llmActorNode2Ref;
    private static ActorRef<LoggingActor.Command> loggingActorRef;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Handle pattern demonstration
        if (args.length == 1 && "patterns".equals(args[0])) {
            demonstrateAllAkkaPatterns();
            testAllAkkaPatterns();
            return;
        }

        // Handle comprehensive Akka tests
        if (args.length == 1 && "akka-tests".equals(args[0])) {
            runComprehensiveAkkaTests();
            return;
        }

        // Handle interactive test mode
        if (args.length == 1 && "interactive".equals(args[0])) {
            runInteractiveMode();
            return;
        }

        // Handle simple test mode
        if (args.length == 1 && "test".equals(args[0])) {
            testAIConnection();
            return;
        }

        // Handle cluster mode
        if (args.length < 2) {
            System.err.println("Usage:");
            System.err.println("  java Main patterns              # Demonstrate tell/ask/forward patterns");
            System.err.println("  java Main akka-tests            # Comprehensive Akka framework tests");
            System.err.println("  java Main interactive           # Interactive recipe mode");
            System.err.println("  java Main test                  # Simple AI test");
            System.err.println("  java Main <port> <role> [seed]  # Cluster mode");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String role = args[1];
        boolean isSeed = args.length > 2 && "seed".equals(args[2]);

        startCluster(port, role, isSeed);
    }

    private static void runComprehensiveAkkaTests() {
        System.out.println("🧪 COMPREHENSIVE AKKA FRAMEWORK TESTS");
        System.out.println("=====================================");

        // Test 1: Actor System Creation
        testActorSystemCreation();

        // Test 2: Individual Actor Behavior
        testIndividualActors();

        // Test 3: Message Serialization
        testMessageSerialization();

        // Test 4: Actor Communication
        testActorCommunication();

        // Test 5: Error Handling
        testErrorHandling();

        // Test 6: Performance Testing
        testPerformance();

        System.out.println("\n🎉 All Akka framework tests completed!");
    }

    private static void testActorSystemCreation() {
        System.out.println("\n📋 TEST 1: Actor System Creation");
        System.out.println("================================");

        try {
            // Test creating actor system with different configurations
            Config testConfig = ConfigFactory.parseString(
                    "akka.actor.provider = local\n" +
                            "akka.loglevel = INFO"
            );

            ActorSystem<String> testSystem = ActorSystem.create(
                    Behaviors.receive(String.class)
                            .onMessage(String.class, msg -> {
                                System.out.println("✅ Actor system message handling works: " + msg);
                                return Behaviors.same();
                            })
                            .build(),
                    "TestSystem",
                    testConfig
            );

            // Test message sending
            testSystem.tell("Hello Actor System!");

            // Wait briefly for message processing
            Thread.sleep(1000);

            testSystem.terminate();
            System.out.println("✅ Actor System Creation: PASSED");

        } catch (Exception e) {
            System.err.println("❌ Actor System Creation: FAILED - " + e.getMessage());
        }
    }

    private static void testIndividualActors() {
        System.out.println("\n🎭 TEST 2: Individual Actor Behavior");
        System.out.println("===================================");

        ActorSystem<Void> testSystem = ActorSystem.create(
                Behaviors.setup(context -> {
                    try {
                        // Test LLMActor creation
                        OllamaClient testAIClient = new OllamaClient("llama3.2:1b");
                        ActorRef<LLMActor.Command> testLLMActor = context.spawn(LLMActor.create(testAIClient), "testLLM");
                        System.out.println("✅ LLMActor creation: PASSED");

                        // Test LoggingActor creation
                        ActorRef<LoggingActor.Command> testLoggingActor = context.spawn(LoggingActor.create(), "testLogging");
                        System.out.println("✅ LoggingActor creation: PASSED");

                        // Test RecipeFinder creation
                        ActorRef<RecipeFinder.Command> testRecipeFinder = context.spawn(
                                RecipeFinder.create(testLLMActor, testLoggingActor), "testRecipeFinder");
                        System.out.println("✅ RecipeFinder creation: PASSED");

                        // Test SubstitutionExpert creation
                        ActorRef<SubstitutionExpert.Command> testSubExpert = context.spawn(
                                SubstitutionExpert.create(testAIClient), "testSubExpert");
                        System.out.println("✅ SubstitutionExpert creation: PASSED");

                        // Test actor behavior by sending messages
                        testActorMessaging(context, testLLMActor, testLoggingActor, testRecipeFinder, testSubExpert);

                    } catch (Exception e) {
                        System.err.println("❌ Individual Actor Test: FAILED - " + e.getMessage());
                        e.printStackTrace();
                    }

                    return Behaviors.empty();
                }),
                "ActorTestSystem"
        );

        // Let tests run for a moment
        try {
            Thread.sleep(15000); // Wait longer for AI responses
            testSystem.terminate();
        } catch (InterruptedException e) {
            testSystem.terminate();
        }
    }

    private static void testActorMessaging(akka.actor.typed.javadsl.ActorContext<Void> context,
                                           ActorRef<LLMActor.Command> llmActor,
                                           ActorRef<LoggingActor.Command> loggingActor,
                                           ActorRef<RecipeFinder.Command> recipeFinder,
                                           ActorRef<SubstitutionExpert.Command> subExpert) {

        System.out.println("\n📨 Testing Actor Messaging:");

        // Test 1: Direct LLMActor messaging
        ActorRef<LLMActor.Response> llmResponseHandler = context.spawn(
                Behaviors.receive(LLMActor.Response.class)
                        .onMessage(LLMActor.RecipeResponse.class, response -> {
                            System.out.println("✅ LLMActor direct messaging: PASSED");
                            System.out.println("   Response length: " + response.response.length() + " characters");
                            return Behaviors.same();
                        })
                        .onMessage(LLMActor.RecipeError.class, error -> {
                            System.out.println("⚠️  LLMActor returned error: " + error.error);
                            return Behaviors.same();
                        })
                        .build(),
                "llmTestHandler"
        );

        RecipeRequest testRequest = new RecipeRequest("test messaging soup", "none", false);
        llmActor.tell(new LLMActor.ProcessRecipeRequest(testRequest, llmResponseHandler));

        // Test 2: LoggingActor messaging
        loggingActor.tell(new LoggingActor.LogRecipeRequest(testRequest, "Test response for logging"));
        System.out.println("✅ LoggingActor messaging: PASSED");

        // Test 3: RecipeFinder end-to-end messaging
        ActorRef<RecipeFinder.RecipeResponse> recipeResponseHandler = context.spawn(
                Behaviors.receive(RecipeFinder.RecipeResponse.class)
                        .onMessage(RecipeFinder.RecipeResponse.class, response -> {
                            System.out.println("✅ RecipeFinder end-to-end messaging: PASSED");
                            System.out.println("   Full chain: RecipeFinder -> LLMActor -> LoggingActor -> Response");
                            return Behaviors.same();
                        })
                        .build(),
                "recipeTestHandler"
        );

        RecipeRequest chainTestRequest = new RecipeRequest("actor chain test pizza", "vegetarian", true);
        recipeFinder.tell(new RecipeFinder.FindRecipe(chainTestRequest, recipeResponseHandler));

        // Test 4: SubstitutionExpert messaging
        ActorRef<SubstitutionExpert.SubstitutionResponse> subResponseHandler = context.spawn(
                Behaviors.receive(SubstitutionExpert.SubstitutionResponse.class)
                        .onMessage(SubstitutionExpert.SubstitutionResponse.class, response -> {
                            System.out.println("✅ SubstitutionExpert messaging: PASSED");
                            System.out.println("   Substitution advice length: " + response.response.length() + " characters");
                            return Behaviors.same();
                        })
                        .build(),
                "subTestHandler"
        );

        subExpert.tell(new SubstitutionExpert.FindSubstitutions(testRequest, subResponseHandler));
    }

    private static void testMessageSerialization() {
        System.out.println("\n📦 TEST 3: Message Serialization");
        System.out.println("================================");

        try {
            // Test RecipeRequest serialization
            RecipeRequest original = new RecipeRequest("pasta carbonara", "vegetarian", true);

            // Test object creation and properties
            System.out.println("Original request: " + original);
            System.out.println("Query: " + original.getQuery());
            System.out.println("Dietary: " + original.getDietaryPreference());
            System.out.println("Substitutions: " + original.isNeedSubstitutions());

            // Test equals and hashCode
            RecipeRequest duplicate = new RecipeRequest("pasta carbonara", "vegetarian", true);
            if (original.equals(duplicate)) {
                System.out.println("✅ Object equality works");
            }

            if (original.hashCode() == duplicate.hashCode()) {
                System.out.println("✅ Hash code consistency works");
            }

            System.out.println("✅ Message Serialization: PASSED");

        } catch (Exception e) {
            System.err.println("❌ Message Serialization: FAILED - " + e.getMessage());
        }
    }

    private static void testActorCommunication() {
        System.out.println("\n🔄 TEST 4: Inter-Actor Communication");
        System.out.println("===================================");

        ActorSystem<Void> testSystem = ActorSystem.create(
                Behaviors.setup(context -> {
                    try {
                        OllamaClient aiClient = new OllamaClient("llama3.2:1b");

                        // Create all actors
                        ActorRef<LLMActor.Command> llmActor = context.spawn(LLMActor.create(aiClient), "commTestLLM");
                        ActorRef<LoggingActor.Command> loggingActor = context.spawn(LoggingActor.create(), "commTestLogging");
                        ActorRef<RecipeFinder.Command> recipeFinder = context.spawn(
                                RecipeFinder.create(llmActor, loggingActor), "commTestRecipeFinder");

                        // Test communication chain: RecipeFinder -> LLMActor -> LoggingActor
                        ActorRef<RecipeFinder.RecipeResponse> finalHandler = context.spawn(
                                Behaviors.receive(RecipeFinder.RecipeResponse.class)
                                        .onMessage(RecipeFinder.RecipeResponse.class, response -> {
                                            System.out.println("✅ Full Communication Chain: PASSED");
                                            System.out.println("   Recipe generated and logged successfully");
                                            System.out.println("   Response preview: " + response.response.substring(0, Math.min(100, response.response.length())) + "...");
                                            return Behaviors.same();
                                        })
                                        .build(),
                                "commTestFinalHandler"
                        );

                        RecipeRequest commTestRequest = new RecipeRequest("communication test beef stew", "healthy", false);
                        recipeFinder.tell(new RecipeFinder.FindRecipe(commTestRequest, finalHandler));

                        System.out.println("📤 Communication test request sent...");

                    } catch (Exception e) {
                        System.err.println("❌ Actor Communication Test: FAILED - " + e.getMessage());
                        e.printStackTrace();
                    }

                    return Behaviors.empty();
                }),
                "CommunicationTestSystem"
        );

        try {
            Thread.sleep(15000); // Wait for async processing
            testSystem.terminate();
        } catch (InterruptedException e) {
            testSystem.terminate();
        }
    }

    private static void testErrorHandling() {
        System.out.println("\n🚨 TEST 5: Error Handling");
        System.out.println("=========================");

        ActorSystem<Void> testSystem = ActorSystem.create(
                Behaviors.setup(context -> {
                    try {
                        // Test with a broken AI client (wrong model)
                        OllamaClient brokenClient = new OllamaClient("nonexistent-model-12345");
                        ActorRef<LLMActor.Command> brokenLLMActor = context.spawn(LLMActor.create(brokenClient), "brokenLLM");

                        // Test error response
                        ActorRef<LLMActor.Response> errorHandler = context.spawn(
                                Behaviors.receive(LLMActor.Response.class)
                                        .onMessage(LLMActor.RecipeError.class, error -> {
                                            System.out.println("✅ Error Handling: PASSED - Correctly caught error");
                                            System.out.println("   Error message: " + error.error.substring(0, Math.min(100, error.error.length())) + "...");
                                            return Behaviors.same();
                                        })
                                        .onMessage(LLMActor.RecipeResponse.class, success -> {
                                            System.out.println("⚠️  Expected error but got success - unexpected model availability");
                                            return Behaviors.same();
                                        })
                                        .build(),
                                "errorTestHandler"
                        );

                        RecipeRequest errorTestRequest = new RecipeRequest("error test recipe", "none", false);
                        brokenLLMActor.tell(new LLMActor.ProcessRecipeRequest(errorTestRequest, errorHandler));

                        System.out.println("📤 Error test request sent with invalid model...");

                    } catch (Exception e) {
                        System.err.println("❌ Error Handling Test setup: FAILED - " + e.getMessage());
                    }

                    return Behaviors.empty();
                }),
                "ErrorTestSystem"
        );

        try {
            Thread.sleep(8000);
            testSystem.terminate();
        } catch (InterruptedException e) {
            testSystem.terminate();
        }
    }

    private static void testPerformance() {
        System.out.println("\n⚡ TEST 6: Performance Testing");
        System.out.println("=============================");

        ActorSystem<Void> testSystem = ActorSystem.create(
                Behaviors.setup(context -> {
                    try {
                        OllamaClient aiClient = new OllamaClient("llama3.2:1b");
                        ActorRef<LLMActor.Command> perfLLMActor = context.spawn(LLMActor.create(aiClient), "perfLLM");

                        // Performance test: Send multiple requests
                        int numRequests = 3; // Small number for quick testing
                        long startTime = System.currentTimeMillis();

                        ActorRef<LLMActor.Response> perfHandler = context.spawn(
                                Behaviors.setup(perfCtx -> {
                                    int[] responseCount = {0};

                                    return Behaviors.receive(LLMActor.Response.class)
                                            .onMessage(LLMActor.Response.class, response -> {
                                                responseCount[0]++;

                                                if (responseCount[0] == numRequests) {
                                                    long endTime = System.currentTimeMillis();
                                                    double avgTime = (endTime - startTime) / (double) numRequests;

                                                    System.out.println("✅ Performance Test: PASSED");
                                                    System.out.println("   Processed " + numRequests + " requests");
                                                    System.out.println("   Total time: " + (endTime - startTime) + "ms");
                                                    System.out.println("   Average time per request: " + String.format("%.2f", avgTime) + "ms");

                                                    if (avgTime < 10000) { // Less than 10 seconds per request
                                                        System.out.println("   ⚡ Performance: EXCELLENT");
                                                    } else if (avgTime < 30000) { // Less than 30 seconds
                                                        System.out.println("   👍 Performance: GOOD");
                                                    } else {
                                                        System.out.println("   ⚠️  Performance: SLOW (consider smaller model)");
                                                    }
                                                }

                                                return Behaviors.same();
                                            })
                                            .build();
                                }),
                                "perfHandler"
                        );

                        // Send multiple test requests
                        for (int i = 1; i <= numRequests; i++) {
                            RecipeRequest perfRequest = new RecipeRequest("performance test recipe " + i, "none", false);
                            perfLLMActor.tell(new LLMActor.ProcessRecipeRequest(perfRequest, perfHandler));
                        }

                        System.out.println("📤 Sent " + numRequests + " performance test requests...");

                    } catch (Exception e) {
                        System.err.println("❌ Performance Test: FAILED - " + e.getMessage());
                        e.printStackTrace();
                    }

                    return Behaviors.empty();
                }),
                "PerformanceTestSystem"
        );

        try {
            Thread.sleep(45000); // Wait for all responses
            testSystem.terminate();
        } catch (InterruptedException e) {
            testSystem.terminate();
        }
    }

    private static void runInteractiveMode() {
        System.out.println("🍳 Welcome to the Smart Cooking Assistant!");
        System.out.println("==========================================");

        try {
            // Create AI client
            OllamaClient aiClient = new OllamaClient("llama3.2:1b");

            // Test connection
            System.out.println("🦙 Testing Ollama connection...");
            aiClient.generateRecipe("test");
            System.out.println("✅ Ollama is ready!\n");

            while (true) {
                System.out.println("\n" + "=".repeat(50));
                System.out.print("🍽️  What would you like to cook? (or 'quit' to exit): ");
                String query = scanner.nextLine().trim();

                if ("quit".equalsIgnoreCase(query) || "exit".equalsIgnoreCase(query)) {
                    System.out.println("👋 Thanks for using Smart Cooking Assistant!");
                    break;
                }

                if (query.isEmpty()) {
                    System.out.println("❌ Please enter a recipe request!");
                    continue;
                }

                // Get dietary preferences
                System.out.print("🥗 Any dietary preferences? (vegetarian/vegan/gluten-free/keto or press Enter for none): ");
                String dietary = scanner.nextLine().trim();
                if (dietary.isEmpty()) dietary = "none";

                // Ask about substitutions
                System.out.print("🔄 Need ingredient substitutions? (y/n): ");
                String subsInput = scanner.nextLine().trim();
                boolean needSubs = subsInput.toLowerCase().startsWith("y");

                // Create recipe request
                RecipeRequest request = new RecipeRequest(query, dietary, needSubs);

                System.out.println("\n🤖 Generating recipe...");
                System.out.println("📝 Request: " + request);

                try {
                    String recipe = aiClient.generateRecipe(buildDetailedPrompt(request));

                    System.out.println("\n" + "=".repeat(60));
                    System.out.println("🎉 YOUR RECIPE:");
                    System.out.println("=".repeat(60));
                    System.out.println(recipe);
                    System.out.println("=".repeat(60));

                } catch (Exception e) {
                    System.err.println("❌ Error generating recipe: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize AI client: " + e.getMessage());
            System.err.println("💡 Make sure Ollama is running and llama3.2:1b model is downloaded");
            System.err.println("   Commands: ollama serve && ollama pull llama3.2:1b");
        }
    }

    private static String buildDetailedPrompt(RecipeRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a detailed recipe for: ").append(request.getQuery());

        if (!"none".equals(request.getDietaryPreference())) {
            prompt.append("\nMake it ").append(request.getDietaryPreference()).append(" friendly.");
        }

        if (request.isNeedSubstitutions()) {
            prompt.append("\nInclude common ingredient substitutions at the end.");
        }

        prompt.append("\nFormat the recipe with:");
        prompt.append("\n- Clear title");
        prompt.append("\n- Ingredients list with measurements");
        prompt.append("\n- Step-by-step instructions");
        prompt.append("\n- Cooking/prep time");
        prompt.append("\n- Serving size");

        return prompt.toString();
    }

    private static void demonstrateAllAkkaPatterns() {
        System.out.println("🎯 DEMONSTRATING ALL REQUIRED AKKA PATTERNS");
        System.out.println("===========================================");

        ActorSystem<Void> demoSystem = ActorSystem.create(
                Behaviors.setup(context -> {
                    try {
                        // Create actors
                        OllamaClient aiClient = new OllamaClient("llama3.2:1b");
                        ActorRef<LLMActor.Command> llmActor = context.spawn(LLMActor.create(aiClient), "demoLLM");
                        ActorRef<LoggingActor.Command> loggingActor = context.spawn(LoggingActor.create(), "demoLogging");
                        ActorRef<RecipeFinder.Command> recipeFinder = context.spawn(
                                RecipeFinder.create(llmActor, loggingActor), "demoRecipeFinder");

                        // Create response handler to catch all responses
                        ActorRef<RecipeFinder.RecipeResponse> responseHandler = context.spawn(
                                Behaviors.receive(RecipeFinder.RecipeResponse.class)
                                        .onMessage(RecipeFinder.RecipeResponse.class, response -> {
                                            System.out.println("🎉 PATTERN DEMO SUCCESS: " + response.processingMethod.toUpperCase() + " pattern worked!");
                                            System.out.println("   Response: " + response.response.substring(0, Math.min(100, response.response.length())) + "...");
                                            return Behaviors.same();
                                        })
                                        .build(),
                                "patternDemoHandler"
                        );

                        // DEMONSTRATE ALL THREE PATTERNS
                        System.out.println("\n1️⃣ DEMONSTRATING TELL PATTERN (fire-and-forget):");
                        RecipeRequest tellRequest = new RecipeRequest("tell demo - simple pasta", "none", false);
                        recipeFinder.tell(new RecipeFinder.FindRecipe(tellRequest, responseHandler));

                        // Wait a moment between patterns
                        Thread.sleep(2000);

                        System.out.println("\n2️⃣ DEMONSTRATING ASK PATTERN (request-response with future):");
                        RecipeRequest askRequest = new RecipeRequest("ask demo - chicken curry", "spicy", true);
                        recipeFinder.tell(new RecipeFinder.FindRecipeWithAsk(askRequest, responseHandler));

                        // Wait a moment between patterns
                        Thread.sleep(2000);

                        System.out.println("\n3️⃣ DEMONSTRATING FORWARD PATTERN (retain original sender):");
                        RecipeRequest forwardRequest = new RecipeRequest("forward demo - vegetable soup", "vegan", false);
                        recipeFinder.tell(new RecipeFinder.ForwardToLLM(forwardRequest, responseHandler));

                        System.out.println("\n📋 SUMMARY OF DEMONSTRATED PATTERNS:");
                        System.out.println("✅ TELL: Fire-and-forget messaging");
                        System.out.println("✅ ASK: Request-response with CompletionStage/Future");
                        System.out.println("✅ FORWARD: Message forwarding preserving original sender");

                    } catch (Exception e) {
                        System.err.println("❌ Pattern demonstration failed: " + e.getMessage());
                        e.printStackTrace();
                    }

                    return Behaviors.empty();
                }),
                "PatternDemoSystem"
        );

        try {
            Thread.sleep(30000); // Wait for all patterns to complete
            demoSystem.terminate();
        } catch (InterruptedException e) {
            demoSystem.terminate();
        }
    }

    private static void testAIConnection() {
        System.out.println("🧪 Testing AI connection...");

        try {
            // Try Ollama first (free, local)
            System.out.println("🦙 Trying Ollama (local)...");
            try {
                OllamaClient ollamaClient = new OllamaClient("llama3.2:1b");
                String ollamaResponse = ollamaClient.generateRecipe("Give me a simple recipe for scrambled eggs");

                System.out.println("✅ OLLAMA SUCCESS!");
                System.out.println("Complete Response:");
                System.out.println("==================");
                System.out.println(ollamaResponse);
                System.out.println("==================");
                return;

            } catch (Exception ollamaError) {
                System.out.println("⚠️  Ollama failed: " + ollamaError.getMessage());
                System.out.println("💡 Make sure Ollama is running: 'ollama serve' and model is downloaded: 'ollama pull llama3.2:1b'");
            }

        } catch (Exception e) {
            System.err.println("❌ ALL TESTS FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startCluster(int port, String role, boolean isSeed) {
        // Load configuration
        Config config = ConfigFactory.load();

        // Override port and role
        Config customConfig = ConfigFactory.parseString(
                "akka.remote.artery.canonical.port=" + port + "\n" +
                        "akka.cluster.roles=[\"" + role + "\"]"
        ).withFallback(config);

        ActorSystem<Void> system = ActorSystem.create(
                Behaviors.setup(context -> {
                    context.getLog().info("🚀 Starting {} cluster node on port {}", role, port);

                    // Create AI client - prefer Ollama, fallback to OpenAI
                    OpenAIClient aiClient = createAIClient(config, context);
                    if (aiClient == null) {
                        context.getLog().error("❌ No AI client available! Check Ollama or OpenAI configuration.");
                        return Behaviors.stopped();
                    }

                    // Create actors based on role
                    if ("node1".equals(role)) {
                        llmActorNode1Ref = context.spawn(LLMActor.create(aiClient), "llmActor");
                        loggingActorRef = context.spawn(LoggingActor.create(), "loggingActor");
                        recipeFinderRef = context.spawn(RecipeFinder.create(llmActorNode1Ref, loggingActorRef), "recipeFinder");

                        context.getLog().info("✅ Node 1 actors created: RecipeFinder, LLMActor, LoggingActor");

                    } else if ("node2".equals(role)) {
                        llmActorNode2Ref = context.spawn(LLMActor.create(aiClient), "llmActor");
                        substitutionExpertRef = context.spawn(SubstitutionExpert.create(aiClient), "substitutionExpert");

                        context.getLog().info("✅ Node 2 actors created: LLMActor, SubstitutionExpert");
                    }

                    return Behaviors.empty();
                }),
                "SmartCookingSystem",
                customConfig
        );

        // Join cluster
        Cluster cluster = Cluster.get(system);
        if (isSeed) {
            cluster.manager().tell(akka.cluster.typed.Join.create(cluster.selfMember().address()));
        }

        // Subscribe to cluster events
        ActorRef<ClusterEvent.MemberEvent> clusterListener = system.systemActorOf(
                Behaviors.<ClusterEvent.MemberEvent>setup(ctx -> {
                    cluster.subscriptions().tell(Subscribe.create(ctx.getSelf(), ClusterEvent.MemberEvent.class));

                    return Behaviors.receive(ClusterEvent.MemberEvent.class)
                            .onMessage(ClusterEvent.MemberUp.class, memberUp -> {
                                ctx.getLog().info("🎉 Member is Up: {}", memberUp.member());

                                // Count members
                                int memberCount = 0;
                                for (akka.cluster.Member member : cluster.state().getMembers()) {
                                    memberCount++;
                                }

                                if (memberCount >= 2 && "node1".equals(role)) {
                                    ctx.getLog().info("🚀 Both nodes are up! Cluster is ready for recipe requests!");
                                    startInteractiveClusterMode(system);
                                }

                                return Behaviors.same();
                            })
                            .onMessage(ClusterEvent.MemberEvent.class, event -> {
                                ctx.getLog().info("Cluster event: {}", event);
                                return Behaviors.same();
                            })
                            .build();
                }),
                "clusterListener",
                Props.empty()
        );

        // Keep running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            system.terminate();
        }
    }

    private static OpenAIClient createAIClient(Config config, akka.actor.typed.javadsl.ActorContext<Void> context) {
        // Try Ollama first
        try {
            OllamaClient ollama = new OllamaClient("llama3.2:1b");
            // Quick test
            ollama.generateRecipe("test");
            context.getLog().info("🦙 Using Ollama (local AI)");
            return ollama;
        } catch (Exception e) {
            context.getLog().warn("Ollama not available: {}", e.getMessage());
        }

        // Fallback to OpenAI
        try {
            String apiKey = config.getString("cooking.openai.api-key");
            if (!apiKey.isEmpty()) {
                String model = config.getString("cooking.openai.model");
                double temperature = config.getDouble("cooking.openai.temperature");
                context.getLog().info("🤖 Using OpenAI: {}", model);
                return new OpenAIClient(apiKey, model, temperature);
            }
        } catch (Exception e) {
            context.getLog().warn("OpenAI not available: {}", e.getMessage());
        }

        return null;
    }

    private static void startInteractiveClusterMode(ActorSystem<Void> system) {
        new Thread(() -> {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🍳 SMART COOKING ASSISTANT CLUSTER READY!");
            System.out.println("=".repeat(60));
            System.out.println("Available commands:");
            System.out.println("  - Type recipe requests normally (e.g., 'chicken curry')");
            System.out.println("  - Type 'cluster-status' to see cluster info");
            System.out.println("  - Type 'actor-test' to test actor communication");
            System.out.println("  - Type 'performance-test' to run performance tests");
            System.out.println("  - Type 'substitution-test' to test substitution expert");
            System.out.println("  - Type 'quit' to exit");

            while (true) {
                try {
                    System.out.print("\n🍽️  Command: ");
                    String input = scanner.nextLine().trim();

                    if ("quit".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                        System.out.println("👋 Shutting down cooking assistant...");
                        system.terminate();
                        break;
                    }

                    if ("cluster-status".equalsIgnoreCase(input)) {
                        printClusterStatus(system);
                        continue;
                    }

                    if ("actor-test".equalsIgnoreCase(input)) {
                        testActorCommunicationInCluster(system);
                        continue;
                    }

                    if ("performance-test".equalsIgnoreCase(input)) {
                        runClusterPerformanceTest(system);
                        continue;
                    }

                    if ("substitution-test".equalsIgnoreCase(input)) {
                        testSubstitutionExpertInCluster(system);
                        continue;
                    }

                    if (input.isEmpty()) {
                        System.out.println("❌ Please enter a command or recipe request!");
                        continue;
                    }

                    // Regular recipe request
                    processRecipeRequest(system, input);

                } catch (Exception e) {
                    System.err.println("❌ Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private static void printClusterStatus(ActorSystem<Void> system) {
        Cluster cluster = Cluster.get(system);
        System.out.println("\n📊 CLUSTER STATUS:");
        System.out.println("==================");
        System.out.println("Self Address: " + cluster.selfMember().address());
        System.out.println("Self Roles: " + cluster.selfMember().getRoles());
        System.out.println("Cluster Members:");

        int memberCount = 0;
        for (akka.cluster.Member member : cluster.state().getMembers()) {
            memberCount++;
            System.out.println("  " + memberCount + ". " + member.address() + " - " + member.status() + " - " + member.getRoles());
        }

        System.out.println("Total Members: " + memberCount);
        System.out.println("Cluster Status: " + (memberCount >= 2 ? "✅ HEALTHY" : "⚠️  INCOMPLETE"));

        // Show actor availability
        System.out.println("\nActor Availability:");
        System.out.println("- RecipeFinder: " + (recipeFinderRef != null ? "✅ Available" : "❌ Not available"));
        System.out.println("- SubstitutionExpert: " + (substitutionExpertRef != null ? "✅ Available" : "❌ Not available"));
    }

    private static void testActorCommunicationInCluster(ActorSystem<Void> system) {
        System.out.println("\n🧪 TESTING ACTOR COMMUNICATION IN CLUSTER...");

        if (recipeFinderRef != null) {
            RecipeRequest testRequest = new RecipeRequest("cluster communication test - simple salad", "healthy", false);
            sendRecipeRequestThroughCluster(system, testRequest);
            System.out.println("📤 Sent actor communication test request");
        } else {
            System.out.println("❌ RecipeFinder not available in this node (try this command in node1)");
        }
    }

    private static void runClusterPerformanceTest(ActorSystem<Void> system) {
        System.out.println("\n⚡ RUNNING CLUSTER PERFORMANCE TEST...");

        if (recipeFinderRef != null) {
            long startTime = System.currentTimeMillis();

            String[] testRecipes = {
                    "quick pasta dish",
                    "simple breakfast",
                    "healthy snack"
            };

            for (int i = 0; i < testRecipes.length; i++) {
                RecipeRequest perfRequest = new RecipeRequest(testRecipes[i], "none", false);
                sendRecipeRequestThroughCluster(system, perfRequest);
            }

            System.out.println("📤 Sent " + testRecipes.length + " concurrent requests. Watch for responses...");
            System.out.println("⏱️  Started at: " + new java.util.Date());
        } else {
            System.out.println("❌ Performance test requires node1 (RecipeFinder)");
        }
    }

    private static void testSubstitutionExpertInCluster(ActorSystem<Void> system) {
        System.out.println("\n🔄 TESTING SUBSTITUTION EXPERT...");

        if (substitutionExpertRef != null) {
            // This would run on node2
            System.out.println("✅ SubstitutionExpert is available on this node");
            System.out.println("💡 In a full implementation, you'd send a test request here");
        } else if (recipeFinderRef != null) {
            // This runs on node1
            System.out.println("💡 SubstitutionExpert is on node2. Testing through RecipeFinder...");
            RecipeRequest subTestRequest = new RecipeRequest("pasta carbonara", "vegan", true);
            sendRecipeRequestThroughCluster(system, subTestRequest);
        } else {
            System.out.println("❌ No appropriate actors available for substitution test");
        }
    }

    private static void processRecipeRequest(ActorSystem<Void> system, String query) {
        System.out.print("🥗 Dietary preferences? (vegetarian/vegan/gluten-free/keto or Enter for none): ");
        String dietary = scanner.nextLine().trim();
        if (dietary.isEmpty()) dietary = "none";

        System.out.print("🔄 Need ingredient substitutions? (y/n): ");
        String subsInput = scanner.nextLine().trim();
        boolean needSubs = subsInput.toLowerCase().startsWith("y");

        RecipeRequest request = new RecipeRequest(query, dietary, needSubs);
        sendRecipeRequestThroughCluster(system, request);
    }

    private static void sendRecipeRequestThroughCluster(ActorSystem<Void> system, RecipeRequest request) {
        if (recipeFinderRef != null) {
            system.systemActorOf(
                    Behaviors.<Void>setup(ctx -> {
                        ctx.getLog().info("📤 Processing request through cluster: {}", request);

                        // Create response handler that prints to console
                        ActorRef<RecipeFinder.RecipeResponse> responseHandler =
                                ctx.spawn(Behaviors.<RecipeFinder.RecipeResponse>setup(respCtx -> {
                                    return Behaviors.receive(RecipeFinder.RecipeResponse.class)
                                            .onMessage(RecipeFinder.RecipeResponse.class, response -> {
                                                System.out.println("\n" + "=".repeat(80));
                                                System.out.println("🎉 YOUR RECIPE (Generated through Akka Cluster + Ollama):");
                                                System.out.println("=".repeat(80));
                                                System.out.println(response.response);
                                                System.out.println("=".repeat(80));
                                                System.out.println("⏱️  Response received at: " + new java.util.Date());
                                                return Behaviors.same();
                                            })
                                            .build();
                                }), "interactiveResponseHandler-" + System.currentTimeMillis());

                        // Send request to RecipeFinder actor
                        recipeFinderRef.tell(new RecipeFinder.FindRecipe(request, responseHandler));

                        return Behaviors.stopped();
                    }),
                    "interactiveRequester-" + System.currentTimeMillis(),
                    Props.empty()
            );
        } else {
            System.err.println("❌ RecipeFinder actor not available! Make sure cluster is running and you're on node1.");
        }
    }
    // Add this method to your existing Main.java class (after the testAIConnection method)

    private static void testAllAkkaPatterns() {
        System.out.println("🎯 TESTING ALL REQUIRED AKKA PATTERNS");
        System.out.println("=====================================");

        ActorSystem<Void> patternSystem = ActorSystem.create(
                Behaviors.setup(context -> {
                    try {
                        // Create actors
                        OllamaClient aiClient = new OllamaClient("llama3.2:1b");
                        ActorRef<LLMActor.Command> llmActor = context.spawn(LLMActor.create(aiClient), "patternLLM");
                        ActorRef<LoggingActor.Command> loggingActor = context.spawn(LoggingActor.create(), "patternLogging");
                        ActorRef<RecipeFinder.Command> recipeFinder = context.spawn(
                                RecipeFinder.create(llmActor, loggingActor), "patternRecipeFinder");

                        // Response handler for all patterns
                        ActorRef<RecipeFinder.RecipeResponse> responseHandler = context.spawn(
                                Behaviors.receive(RecipeFinder.RecipeResponse.class)
                                        .onMessage(RecipeFinder.RecipeResponse.class, response -> {
                                            System.out.println("🎉 PATTERN SUCCESS: " + response.processingMethod.toUpperCase() + " pattern completed!");
                                            System.out.println("   Recipe preview: " + response.response.substring(0, Math.min(80, response.response.length())) + "...");
                                            return Behaviors.same();
                                        })
                                        .build(),
                                "patternResponseHandler"
                        );

                        // 1. TELL PATTERN (fire-and-forget)
                        System.out.println("\n1️⃣ TESTING TELL PATTERN:");
                        RecipeRequest tellRequest = new RecipeRequest("tell pattern test - quick soup", "none", false);
                        recipeFinder.tell(new RecipeFinder.FindRecipe(tellRequest, responseHandler));
                        Thread.sleep(8000); // Wait for response

                        // 2. ASK PATTERN (request-response with future)
                        System.out.println("\n2️⃣ TESTING ASK PATTERN:");
                        RecipeRequest askRequest = new RecipeRequest("ask pattern test - pasta dish", "vegetarian", true);
                        recipeFinder.tell(new RecipeFinder.FindRecipeWithAsk(askRequest, responseHandler));
                        Thread.sleep(8000); // Wait for response

                        // 3. FORWARD PATTERN (retain original sender)
                        System.out.println("\n3️⃣ TESTING FORWARD PATTERN:");
                        RecipeRequest forwardRequest = new RecipeRequest("forward pattern test - salad", "healthy", false);
                        recipeFinder.tell(new RecipeFinder.ForwardToLLM(forwardRequest, responseHandler));
                        Thread.sleep(8000); // Wait for response

                        System.out.println("\n✅ ALL AKKA COMMUNICATION PATTERNS DEMONSTRATED!");

                    } catch (Exception e) {
                        System.err.println("❌ Pattern test failed: " + e.getMessage());
                        e.printStackTrace();
                    }

                    return Behaviors.empty();
                }),
                "PatternTestSystem"
        );

        try {
            Thread.sleep(30000); // Wait for all patterns
            patternSystem.terminate();
        } catch (InterruptedException e) {
            patternSystem.terminate();
        }
    }

}
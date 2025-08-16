package com.cooking.api;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;

import java.io.IOException;
import java.util.List;

/**
 * Ollama Client - Uses locally running Ollama server with Spring AI
 * Install: https://ollama.ai/download
 * Usage: ollama serve && ollama pull llama3.2:1b
 *
 * Spring AI provides a cleaner integration with Ollama
 */
public class OllamaClient extends OpenAIClient {
    private final ChatClient chatClient;
    private final String model;
    private final OllamaOptions defaultOptions;
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    /**
     * Creates an OllamaClient with the specified model
     * @param model The Ollama model to use (e.g., "llama3.2:1b", "llama3.2:3b", "codellama")
     */
    public OllamaClient(String model) {
        super("dummy", model, 0.7); // Dummy values for parent constructor
        this.model = model;

        // Create default options
        this.defaultOptions = OllamaOptions.create()
                .withModel(model)
                .withTemperature(0.7f)
                .withNumPredict(2048);

        // Initialize Spring AI Ollama client
        this.chatClient = createChatClient(DEFAULT_OLLAMA_URL);
    }

    /**
     * Creates an OllamaClient with custom URL and model
     * @param ollamaUrl The URL where Ollama is running
     * @param model The Ollama model to use
     */
    public OllamaClient(String ollamaUrl, String model) {
        super("dummy", model, 0.7);
        this.model = model;

        // Create default options
        this.defaultOptions = OllamaOptions.create()
                .withModel(model)
                .withTemperature(0.7f)
                .withNumPredict(2048);

        // Initialize with custom URL
        this.chatClient = createChatClient(ollamaUrl);
    }

    private ChatClient createChatClient(String ollamaUrl) {
        // Create OllamaApi with configuration
        OllamaApi ollamaApi = new OllamaApi(ollamaUrl);

        // Create and return the chat client
        return new OllamaChatClient(ollamaApi);
    }

    @Override
    public String generateRecipe(String prompt) throws IOException {
        // Skip if it's just a test call
        if ("test".equals(prompt)) {
            return testConnection();
        }

        try {
            // Create the full prompt with cooking context
            String fullPrompt = createCookingPrompt(prompt);

            // Create prompt with UserMessage and options
            Prompt springPrompt = new Prompt(
                    new UserMessage(fullPrompt),
                    defaultOptions
            );

            // Call the chat client
            ChatResponse response = chatClient.call(springPrompt);

            // Extract and return the response
            if (response != null && response.getResult() != null) {
                Generation generation = response.getResult();
                if (generation.getOutput() != null) {
                    return generation.getOutput().getContent();
                }
            }

            throw new IOException("Empty response from Ollama");

        } catch (Exception e) {
            // Handle Spring AI specific exceptions
            String errorMessage = "Error calling Ollama API: " + e.getMessage();

            // Add helpful debugging information
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                errorMessage += "\nMake sure Ollama is running: 'ollama serve'";
            } else if (e.getMessage() != null && e.getMessage().contains("model")) {
                errorMessage += "\nMake sure model is downloaded: 'ollama pull " + model + "'";
            }

            throw new IOException(errorMessage, e);
        }
    }

    /**
     * Creates a cooking-specific prompt
     */
    private String createCookingPrompt(String userRequest) {
        return String.format(
                "You are a helpful cooking assistant. %s\n" +
                        "Please provide a detailed recipe with:\n" +
                        "- Complete list of ingredients with measurements\n" +
                        "- Step-by-step instructions\n" +
                        "- Preparation and cooking time\n" +
                        "- Serving size\n" +
                        "- Any helpful tips or variations",
                userRequest
        );
    }

    /**
     * Tests the connection to Ollama
     */
    private String testConnection() throws IOException {
        try {
            // Try a simple prompt to test connectivity
            Prompt testPrompt = new Prompt(
                    new UserMessage("Say 'Test successful' in 3 words"),
                    defaultOptions
            );

            ChatResponse response = chatClient.call(testPrompt);

            if (response != null && response.getResult() != null) {
                return "Test successful - Ollama is connected";
            } else {
                throw new IOException("Ollama connection test failed");
            }
        } catch (Exception e) {
            throw new IOException("Cannot connect to Ollama at " + DEFAULT_OLLAMA_URL +
                    ": " + e.getMessage(), e);
        }
    }

    /**
     * Alternative synchronous method with timeout handling
     */
    public String generateRecipeWithTimeout(String prompt, int timeoutSeconds) throws IOException {
        // Create a thread to handle timeout
        final String[] result = new String[1];
        final IOException[] error = new IOException[1];

        Thread generationThread = new Thread(() -> {
            try {
                result[0] = generateRecipe(prompt);
            } catch (IOException e) {
                error[0] = e;
            }
        });

        generationThread.start();

        try {
            generationThread.join(timeoutSeconds * 1000L);
            if (generationThread.isAlive()) {
                generationThread.interrupt();
                throw new IOException("Recipe generation timed out after " + timeoutSeconds + " seconds");
            }

            if (error[0] != null) {
                throw error[0];
            }

            return result[0];

        } catch (InterruptedException e) {
            throw new IOException("Recipe generation interrupted", e);
        }
    }

    /**
     * Get available models from Ollama (hardcoded for now)
     */
    public List<String> getAvailableModels() {
        // Return common models - in a real implementation,
        // you would call Ollama's API to get the actual list
        return List.of(
                "llama3.2:1b",
                "llama3.2:3b",
                "llama3.1:8b",
                "codellama:7b",
                "mistral:7b",
                "phi3:mini",
                "gemma:2b",
                "neural-chat:7b"
        );
    }

    /**
     * Simple callback interface for async operations
     */
    public interface RecipeCallback {
        void onSuccess(String recipe);
        void onError(IOException error);
    }

    /**
     * Asynchronous recipe generation
     */
    public void generateRecipeAsync(String prompt, RecipeCallback callback) {
        new Thread(() -> {
            try {
                String recipe = generateRecipe(prompt);
                callback.onSuccess(recipe);
            } catch (IOException e) {
                callback.onError(e);
            }
        }).start();
    }
}
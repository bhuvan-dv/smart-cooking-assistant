package com.cooking.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Mock OpenAI Client for testing without API costs
 * Generates realistic recipe responses based on the prompt
 */
public class MockOpenAIClient extends OpenAIClient {
    private static final Random random = new Random();

    // Pre-defined recipe templates for different types
    private static final Map<String, String[]> recipeTemplates = new HashMap<>();

    static {
        recipeTemplates.put("eggs", new String[]{
                "# Simple Scrambled Eggs\n\n**Ingredients:**\n- 3 large eggs\n- 2 tbsp butter\n- Salt and pepper to taste\n- Optional: 2 tbsp milk\n\n**Instructions:**\n1. Crack eggs into a bowl and whisk\n2. Heat butter in non-stick pan over medium-low heat\n3. Pour in eggs and gently stir\n4. Cook for 2-3 minutes until creamy\n5. Season with salt and pepper\n\n**Cooking time:** 5 minutes",
                "# Fluffy Scrambled Eggs\n\n**Ingredients:**\n- 4 eggs\n- 3 tbsp heavy cream\n- 1 tbsp butter\n- Chives for garnish\n\n**Instructions:**\n1. Whisk eggs with cream\n2. Melt butter over low heat\n3. Add eggs, stir constantly\n4. Remove from heat while slightly wet\n5. Garnish with chives\n\n**Cooking time:** 4 minutes"
        });

        recipeTemplates.put("pasta", new String[]{
                "# Simple Pasta with Tomatoes\n\n**Ingredients:**\n- 12 oz pasta\n- 4 fresh tomatoes, diced\n- 3 cloves garlic, minced\n- 1/4 cup olive oil\n- Fresh basil\n- Parmesan cheese\n\n**Instructions:**\n1. Cook pasta according to package directions\n2. Heat olive oil, sauté garlic for 1 minute\n3. Add tomatoes, cook 5 minutes\n4. Toss with pasta and basil\n5. Serve with Parmesan\n\n**Cooking time:** 15 minutes",
                "# Creamy Tomato Pasta\n\n**Ingredients:**\n- 1 lb penne pasta\n- 1 can crushed tomatoes\n- 1/2 cup heavy cream\n- 1 onion, diced\n- 2 tbsp olive oil\n\n**Instructions:**\n1. Cook pasta until al dente\n2. Sauté onion until soft\n3. Add tomatoes, simmer 10 minutes\n4. Stir in cream\n5. Combine with pasta\n\n**Cooking time:** 20 minutes"
        });

        recipeTemplates.put("cookies", new String[]{
                "# Classic Chocolate Chip Cookies\n\n**Ingredients:**\n- 2 1/4 cups flour\n- 1 cup butter, softened\n- 3/4 cup brown sugar\n- 1/2 cup white sugar\n- 2 eggs\n- 2 cups chocolate chips\n\n**Instructions:**\n1. Preheat oven to 375°F\n2. Cream butter and sugars\n3. Beat in eggs\n4. Mix in flour gradually\n5. Fold in chocolate chips\n6. Drop on baking sheet\n7. Bake 9-11 minutes\n\n**Cooking time:** 25 minutes (including baking)"
        });
    }

    public MockOpenAIClient(String apiKey, String model, double temperature) {
        super(apiKey, model, temperature);
    }

    @Override
    public String generateRecipe(String prompt) throws IOException {
        // Simulate API delay
        try {
            Thread.sleep(500 + random.nextInt(1000)); // 0.5-1.5 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Analyze prompt to determine recipe type
        String lowerPrompt = prompt.toLowerCase();
        String[] templates = null;

        if (lowerPrompt.contains("egg")) {
            templates = recipeTemplates.get("eggs");
        } else if (lowerPrompt.contains("pasta") || lowerPrompt.contains("tomato")) {
            templates = recipeTemplates.get("pasta");
        } else if (lowerPrompt.contains("cookie") || lowerPrompt.contains("chocolate")) {
            templates = recipeTemplates.get("cookies");
        }

        String baseRecipe;
        if (templates != null) {
            baseRecipe = templates[random.nextInt(templates.length)];
        } else {
            baseRecipe = generateGenericRecipe(prompt);
        }

        // Apply dietary modifications
        if (lowerPrompt.contains("vegetarian") || lowerPrompt.contains("vegan")) {
            baseRecipe = applyVegetarianModifications(baseRecipe);
        }

        if (lowerPrompt.contains("gluten-free")) {
            baseRecipe = applyGlutenFreeModifications(baseRecipe);
        }

        // Add substitutions if requested
        if (lowerPrompt.contains("substitution")) {
            baseRecipe += "\n\n**Common Substitutions:**\n" +
                    "- Butter → Coconut oil or vegan butter\n" +
                    "- Eggs → Flax eggs (1 tbsp ground flax + 3 tbsp water per egg)\n" +
                    "- Heavy cream → Coconut cream\n" +
                    "- Regular flour → Almond flour or gluten-free flour blend";
        }

        return baseRecipe;
    }

    private String generateGenericRecipe(String prompt) {
        return "# Custom Recipe\n\n**Based on your request:** " + prompt +
                "\n\n**Ingredients:**\n- [Main ingredient]\n- Seasonings to taste\n- Cooking oil\n\n" +
                "**Instructions:**\n1. Prepare ingredients\n2. Cook according to standard methods\n3. Season and serve\n\n" +
                "**Cooking time:** 15-30 minutes\n\n" +
                "*Note: This is a mock response for testing purposes.*";
    }

    private String applyVegetarianModifications(String recipe) {
        return recipe.replace("butter", "vegan butter")
                .replace("heavy cream", "coconut cream")
                .replace("Parmesan cheese", "nutritional yeast or vegan parmesan");
    }

    private String applyGlutenFreeModifications(String recipe) {
        return recipe.replace("flour", "gluten-free flour blend")
                .replace("pasta", "gluten-free pasta");
    }
}
package com.cooking.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.cooking.api.OpenAIClient;
import com.cooking.model.RecipeRequest;

public class LLMActor extends AbstractBehavior<LLMActor.Command> {
    public interface Command {}

    public static final class ProcessRecipeRequest implements Command {
        public final RecipeRequest request;
        public final ActorRef<Response> replyTo;

        public ProcessRecipeRequest(RecipeRequest request, ActorRef<Response> replyTo) {
            this.request = request;
            this.replyTo = replyTo;
        }
    }

    // Define all possible response types
    public interface Response {}

    public static final class RecipeResponse implements Response {
        public final String response;
        public RecipeResponse(String response) {
            this.response = response;
        }
    }

    public static final class RecipeError implements Response {
        public final String error;
        public RecipeError(String error) {
            this.error = error;
        }
    }

    private final OpenAIClient aiClient;

    private LLMActor(ActorContext<Command> context, OpenAIClient aiClient) {
        super(context);
        this.aiClient = aiClient;
        context.getLog().info("🤖 LLMActor created and ready");
    }

    public static Behavior<Command> create(OpenAIClient aiClient) {
        return Behaviors.setup(context -> new LLMActor(context, aiClient));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessRecipeRequest.class, this::onProcessRecipeRequest)
                .build();
    }

    private Behavior<Command> onProcessRecipeRequest(ProcessRecipeRequest msg) {
        getContext().getLog().info("🧠 LLMActor processing: {}", msg.request);

        try {
            String prompt = buildPrompt(msg.request);
            getContext().getLog().info("🔄 Calling AI with prompt...");

            String response = aiClient.generateRecipe(prompt);

            getContext().getLog().info("✅ AI response received (length: {} chars)", response.length());
            msg.replyTo.tell(new RecipeResponse(response));

        } catch (Exception e) {
            getContext().getLog().error("❌ Error processing recipe request: {}", e.getMessage());
            msg.replyTo.tell(new RecipeError("Error generating recipe: " + e.getMessage()));
        }
        return this;
    }

    private String buildPrompt(RecipeRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a detailed recipe for: ").append(request.getQuery());

        if (request.getDietaryPreference() != null &&
                !request.getDietaryPreference().isEmpty() &&
                !"none".equals(request.getDietaryPreference())) {
            prompt.append("\nMake it ").append(request.getDietaryPreference()).append(" friendly.");
        }

        if (request.isNeedSubstitutions()) {
            prompt.append("\nInclude common ingredient substitutions at the end.");
        }

        prompt.append("\nProvide the recipe with:");
        prompt.append("\n- A clear title");
        prompt.append("\n- Complete ingredients list with measurements");
        prompt.append("\n- Detailed step-by-step instructions");
        prompt.append("\n- Cooking time and preparation time");
        prompt.append("\n- Number of servings");

        return prompt.toString();
    }
}
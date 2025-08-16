package com.cooking.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.cooking.api.OpenAIClient;
import com.cooking.model.RecipeRequest;

public class SubstitutionExpert extends AbstractBehavior<SubstitutionExpert.Command> {
    public interface Command {}

    public static final class FindSubstitutions implements Command {
        public final RecipeRequest request;
        public final ActorRef<SubstitutionResponse> replyTo;

        public FindSubstitutions(RecipeRequest request, ActorRef<SubstitutionResponse> replyTo) {
            this.request = request;
            this.replyTo = replyTo;
        }
    }

    public static final class SubstitutionResponse {
        public final String response;

        public SubstitutionResponse(String response) {
            this.response = response;
        }
    }

    private final OpenAIClient aiClient;

    private SubstitutionExpert(ActorContext<Command> context, OpenAIClient aiClient) {
        super(context);
        this.aiClient = aiClient;
        context.getLog().info("🔄 SubstitutionExpert created and ready");
    }

    public static Behavior<Command> create(OpenAIClient aiClient) {
        return Behaviors.setup(context -> new SubstitutionExpert(context, aiClient));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(FindSubstitutions.class, this::onFindSubstitutions)
                .build();
    }

    private Behavior<Command> onFindSubstitutions(FindSubstitutions msg) {
        getContext().getLog().info("🔍 Finding substitutions for: {}", msg.request);

        try {
            String prompt = buildSubstitutionPrompt(msg.request);
            String response = aiClient.generateRecipe(prompt);

            getContext().getLog().info("✅ Substitutions found");
            msg.replyTo.tell(new SubstitutionResponse(response));

        } catch (Exception e) {
            getContext().getLog().error("❌ Error finding substitutions: {}", e.getMessage());
            msg.replyTo.tell(new SubstitutionResponse("Error finding substitutions: " + e.getMessage()));
        }
        return this;
    }

    private String buildSubstitutionPrompt(RecipeRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Provide ingredient substitutions for this recipe: ").append(request.getQuery());

        if (request.getDietaryPreference() != null &&
                !request.getDietaryPreference().isEmpty() &&
                !"none".equals(request.getDietaryPreference())) {
            prompt.append("\nFocus on making it ").append(request.getDietaryPreference()).append(" friendly.");
        }

        prompt.append("\nProvide:");
        prompt.append("\n- Common ingredient substitutions");
        prompt.append("\n- Dietary restriction alternatives");
        prompt.append("\n- Healthier alternatives");
        prompt.append("\n- Budget-friendly substitutions");

        return prompt.toString();
    }
}
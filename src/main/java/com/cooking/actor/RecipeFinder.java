package com.cooking.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.cooking.model.RecipeRequest;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class RecipeFinder extends AbstractBehavior<RecipeFinder.Command> {

    // Commands
    public interface Command {}

    // Regular tell pattern
    public static final class FindRecipe implements Command {
        public final RecipeRequest request;
        public final ActorRef<RecipeResponse> replyTo;

        public FindRecipe(RecipeRequest request, ActorRef<RecipeResponse> replyTo) {
            this.request = request;
            this.replyTo = replyTo;
        }
    }

    // Ask pattern command
    public static final class FindRecipeWithAsk implements Command {
        public final RecipeRequest request;
        public final ActorRef<RecipeResponse> replyTo;

        public FindRecipeWithAsk(RecipeRequest request, ActorRef<RecipeResponse> replyTo) {
            this.request = request;
            this.replyTo = replyTo;
        }
    }

    // Forward pattern command
    public static final class ForwardToLLM implements Command {
        public final RecipeRequest request;
        public final ActorRef<RecipeResponse> originalSender;

        public ForwardToLLM(RecipeRequest request, ActorRef<RecipeResponse> originalSender) {
            this.request = request;
            this.originalSender = originalSender;
        }
    }

    // Response
    public static final class RecipeResponse {
        public final String response;
        public final String processingMethod;

        public RecipeResponse(String response, String processingMethod) {
            this.response = response;
            this.processingMethod = processingMethod;
        }
    }

    // Internal adapter for LLM responses
    private static final class WrappedLLMResponse implements Command {
        public final LLMActor.Response response;
        public final ActorRef<RecipeResponse> originalSender;
        public final String processingMethod;

        public WrappedLLMResponse(LLMActor.Response response,
                                  ActorRef<RecipeResponse> originalSender,
                                  String processingMethod) {
            this.response = response;
            this.originalSender = originalSender;
            this.processingMethod = processingMethod;
        }
    }

    private final ActorRef<LLMActor.Command> llmActor;
    private final ActorRef<LoggingActor.Command> loggingActor;

    public static Behavior<Command> create(ActorRef<LLMActor.Command> llmActor,
                                           ActorRef<LoggingActor.Command> loggingActor) {
        return Behaviors.setup(context -> new RecipeFinder(context, llmActor, loggingActor));
    }

    private RecipeFinder(ActorContext<Command> context,
                         ActorRef<LLMActor.Command> llmActor,
                         ActorRef<LoggingActor.Command> loggingActor) {
        super(context);
        this.llmActor = llmActor;
        this.loggingActor = loggingActor;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(FindRecipe.class, this::onFindRecipe)
                .onMessage(FindRecipeWithAsk.class, this::onFindRecipeWithAsk)
                .onMessage(ForwardToLLM.class, this::onForwardToLLM)
                .onMessage(WrappedLLMResponse.class, this::onWrappedLLMResponse)
                .build();
    }

    // PATTERN 1: TELL (Fire-and-forget)
    private Behavior<Command> onFindRecipe(FindRecipe command) {
        getContext().getLog().info("📤 TELL PATTERN: Processing recipe request");

        // Create adapter to convert LLMActor.Response to our internal command
        ActorRef<LLMActor.Response> responseAdapter =
                getContext().messageAdapter(LLMActor.Response.class,
                        response -> new WrappedLLMResponse(response, command.replyTo, "tell"));

        // Send to LLM actor using tell
        llmActor.tell(new LLMActor.ProcessRecipeRequest(command.request, responseAdapter));

        return this;
    }

    // PATTERN 2: ASK (Request-Response with Future)
    private Behavior<Command> onFindRecipeWithAsk(FindRecipeWithAsk command) {
        getContext().getLog().info("❓ ASK PATTERN: Processing recipe request with ask");

        // Use ask pattern with timeout
        Duration timeout = Duration.ofSeconds(30);

        CompletionStage<LLMActor.Response> future =
                AskPattern.ask(
                        llmActor,
                        (ActorRef<LLMActor.Response> replyTo) ->
                                new LLMActor.ProcessRecipeRequest(command.request, replyTo),
                        timeout,
                        getContext().getSystem().scheduler()
                );

        // Handle the future response
        getContext().pipeToSelf(future, (response, failure) -> {
            if (failure != null) {
                getContext().getLog().error("Ask pattern failed", failure);
                return new WrappedLLMResponse(
                        new LLMActor.RecipeError("Ask pattern failed: " + failure.getMessage()),
                        command.replyTo,
                        "ask"
                );
            } else {
                return new WrappedLLMResponse(response, command.replyTo, "ask");
            }
        });

        return this;
    }

    // PATTERN 3: FORWARD (Preserve original sender)
    private Behavior<Command> onForwardToLLM(ForwardToLLM command) {
        getContext().getLog().info("↪️ FORWARD PATTERN: Forwarding to LLM with original sender preserved");

        // Create a special adapter that preserves the original sender
        ActorRef<LLMActor.Response> forwardAdapter =
                getContext().messageAdapter(LLMActor.Response.class,
                        response -> new WrappedLLMResponse(response, command.originalSender, "forward"));

        // Send to LLM actor
        llmActor.tell(new LLMActor.ProcessRecipeRequest(command.request, forwardAdapter));

        // Log that we're forwarding
        getContext().getLog().info("Message forwarded to LLM, original sender will receive response");

        return this;
    }

    // Handle wrapped LLM responses
    private Behavior<Command> onWrappedLLMResponse(WrappedLLMResponse wrapped) {
        if (wrapped.response instanceof LLMActor.RecipeResponse) {
            LLMActor.RecipeResponse recipeResponse = (LLMActor.RecipeResponse) wrapped.response;

            // Log the recipe
            loggingActor.tell(new LoggingActor.LogRecipeRequest(
                    new RecipeRequest("logged", "none", false),
                    recipeResponse.response
            ));

            // Send response back to original sender
            wrapped.originalSender.tell(new RecipeResponse(
                    recipeResponse.response,
                    wrapped.processingMethod
            ));

            getContext().getLog().info("✅ {} pattern completed successfully",
                    wrapped.processingMethod.toUpperCase());

        } else if (wrapped.response instanceof LLMActor.RecipeError) {
            LLMActor.RecipeError error = (LLMActor.RecipeError) wrapped.response;

            wrapped.originalSender.tell(new RecipeResponse(
                    "Error: " + error.error,
                    wrapped.processingMethod + " (failed)"
            ));

            getContext().getLog().error("❌ {} pattern failed: {}",
                    wrapped.processingMethod.toUpperCase(), error.error);
        }

        return this;
    }
}
package com.cooking.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.cooking.model.RecipeRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggingActor extends AbstractBehavior<LoggingActor.Command> {
    public interface Command {}

    public static final class LogRecipeRequest implements Command {
        public final RecipeRequest request;
        public final String response;

        public LogRecipeRequest(RecipeRequest request, String response) {
            this.request = request;
            this.response = response;
        }
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LoggingActor(ActorContext<Command> context) {
        super(context);
        context.getLog().info("📝 LoggingActor created and ready");
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(LoggingActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(LogRecipeRequest.class, this::onLogRecipeRequest)
                .build();
    }

    private Behavior<Command> onLogRecipeRequest(LogRecipeRequest msg) {
        String timestamp = LocalDateTime.now().format(formatter);

        getContext().getLog().info("📊 RECIPE LOG [{}]: Query='{}', Dietary='{}', Substitutions={}, ResponseLength={}",
                timestamp,
                msg.request.getQuery(),
                msg.request.getDietaryPreference(),
                msg.request.isNeedSubstitutions(),
                msg.response.length()
        );

        // You could also write to file here for persistent logging
        // writeToLogFile(timestamp, msg.request, msg.response);

        return this;
    }

    // Optional: Write to file for persistent logging
    private void writeToLogFile(String timestamp, RecipeRequest request, String response) {
        // Implementation for file logging if needed
        // Could use Files.write() to append to a log file
    }
}
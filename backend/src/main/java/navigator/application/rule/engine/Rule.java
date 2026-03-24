package navigator.application.rule.engine;

public interface Rule<TContext, TResult> {

    String getId();

    int getPriority();

    boolean match(TContext context);

    TResult apply(TContext context);

    String reason(TContext context);
}

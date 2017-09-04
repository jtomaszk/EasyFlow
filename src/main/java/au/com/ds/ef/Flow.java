package au.com.ds.ef;

import au.com.ds.ef.call.ContextHandler;
import au.com.ds.ef.call.EventHandler;
import au.com.ds.ef.call.ExecutionErrorHandler;
import au.com.ds.ef.call.StateHandler;
import au.com.ds.ef.err.LogicViolationError;

public abstract class Flow<C extends StatefulContext> implements TransitonManager {

    public void waitForCompletion(C context) {
        context.awaitTermination();
    }

    public void start(final C context) {
        start(false, context);
    }

    abstract public void start(boolean enterInitialState, final C context);


    abstract public Flow<C> whenEnter(StateEnum state, ContextHandler<? extends C> onEnter);

    abstract public Flow<C> whenEnter(StateHandler<C> onEnter);

    abstract public Flow<C> whenError(ExecutionErrorHandler<C> onError);

    abstract public Flow<C> whenFinalState(StateHandler<C> onFinalState);

    public Flow<C> whenLeave(StateEnum state, ContextHandler<C> onEnter) {
        throw new UnsupportedOperationException();
    }

    public Flow<C> whenLeave(StateHandler<C> onEnter) {
        throw new UnsupportedOperationException();
    }

    public Flow<C> whenEvent(EventHandler<C> onEvent) {
        throw new UnsupportedOperationException();
    }

    public Flow<C> whenEvent(EventEnum event, ContextHandler<C> onEvent) {
        throw new UnsupportedOperationException();
    }


    abstract public boolean trigger(final EventEnum event, final C context) throws LogicViolationError;

    abstract public boolean safeTrigger(final EventEnum event, final C context);

    abstract public boolean conditionTrigger(final EventEnum event, final C context, final StateEnum condition) throws LogicViolationError;
}

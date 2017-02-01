package au.com.ds.ef;

import au.com.ds.ef.call.ContextHandler;
import au.com.ds.ef.call.EventHandler;
import au.com.ds.ef.call.ExecutionErrorHandler;
import au.com.ds.ef.call.StateHandler;
import au.com.ds.ef.err.LogicViolationError;

public interface Flow<C extends StatefulContext> extends TransitonManager{

    default void waitForCompletion(C context) {
        context.awaitTermination();
    }

    default void start(final C context){
        start(false, context);
    }

    void start(boolean enterInitialState, final C context);


    Flow<C> whenEnter(StateEnum state, ContextHandler<? extends C> onEnter);

    Flow<C> whenEnter(StateHandler<C> onEnter);

    Flow<C> whenError(ExecutionErrorHandler<C> onError);

    Flow<C> whenFinalState(StateHandler<C> onFinalState);

    default Flow<C> whenLeave(StateEnum state, ContextHandler<C> onEnter){
        throw new UnsupportedOperationException();
    }

    default Flow<C> whenLeave(StateHandler<C> onEnter) {
        throw new UnsupportedOperationException();
    }

    default Flow<C> whenEvent(EventHandler<C> onEvent) {
        throw new UnsupportedOperationException();
    }

    default Flow<C> whenEvent(EventEnum event, ContextHandler<C> onEvent){
        throw new UnsupportedOperationException();
    }


    void trigger(final EventEnum event, final C context) throws LogicViolationError;

    boolean safeTrigger(final EventEnum event, final C context);

    boolean conditionTrigger(final EventEnum event, final C context, final StateEnum condition) throws LogicViolationError;
}

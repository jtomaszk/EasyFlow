package au.com.ds.ef;

import au.com.ds.ef.call.ContextHandler;
import au.com.ds.ef.call.ExecutionErrorHandler;
import au.com.ds.ef.call.StateHandler;
import au.com.ds.ef.err.ExecutionError;
import au.com.ds.ef.err.LogicViolationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static au.com.ds.ef.HandlerCollection.EventType;

/**
 *
 * Flow which only invokes actions registered when entering particular state.
 * Apart from that you can register generic action invoked upon error and termination.
 *
 * Flow allows to have conditional steps.
 *
 */
public class EnterFlow<C extends StatefulContext> implements Flow<C> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public class DefaultErrorHandler implements ExecutionErrorHandler<StatefulContext> {
        @Override
        public void call(ExecutionError error, StatefulContext context) {
            String msg = "Execution Error in StateHolder [" + error.getState() + "] ";
            if (error.getEvent() != null) {
                msg += "on EventHolder [" + error.getEvent() + "] ";
            }
            msg += "with Context [" + error.getContext() + "] ";

            Exception e = new Exception(msg, error);
            logger.error("Error", e);
        }
    }

    private StateEnum startState;
    private TransitionCollection transitions;

    private Executor executor;

    private HandlerCollection handlers = new HandlerCollection();
    private boolean trace = false;

    protected EnterFlow(StateEnum startState) {
        this.startState = startState;
        this.handlers.setHandler(HandlerCollection.EventType.ERROR, null, null, new DefaultErrorHandler());
    }

    public StateEnum getStartState() {
        return startState;
    }

    public void processAllTransitions(boolean skipValidation) {
        List<Transition> cTransitions = Transition.consumeTransitions();
        if (cTransitions != null) {
            cTransitions = cTransitions.stream().filter(t -> t.getStateFrom() != null).collect(Collectors.toList());
        }
        transitions = new TransitionCollection(cTransitions, !skipValidation);
    }

    public void setTransitions(Collection<Transition> collection, boolean skipValidation) {
        transitions = new TransitionCollection(collection, !skipValidation);
    }

    public List<Transition> getAvailableTransitions(StateEnum stateFrom) {
        return transitions.getTransitions(stateFrom);
    }

    public Flow<C> whenEnter(StateEnum state, ContextHandler<? extends C> onEnter) {
        handlers.setHandler(EventType.STATE_ENTER, state, null, onEnter);
        return this;
    }

    public Flow<C> whenEnter(StateHandler<C> onEnter) {
        handlers.setHandler(EventType.ANY_STATE_ENTER, null, null, onEnter);
        return this;
    }

    public Flow<C> whenError(ExecutionErrorHandler<C> onError) {
        handlers.setHandler(EventType.ERROR, null, null, onError);
        return this;
    }

    public Flow<C> whenFinalState(StateHandler<C> onFinalState) {
        handlers.setHandler(EventType.FINAL_STATE, null, null, onFinalState);
        return this;
    }

    public Flow<C> executor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public Flow<C> trace() {
        trace = true;
        return this;
    }

    public void start(boolean enterInitialState, final C context) {
        if (executor == null) {
            throw new IllegalStateException("You need to provide executor.");
        }
        context.setFlow(this);

        if (context.getStateValue() == null) {
            context.getStateRef().set(startState);
        }

        transit(context.getStateValue(), context);
    }

    public boolean safeTrigger(final EventEnum event, final C context) {
        throw new UnsupportedOperationException();
    }

    public boolean trigger(final EventEnum event, final C context) throws LogicViolationError {
        return trigger(event, context, null, 1);
    }

    public boolean conditionTrigger(final EventEnum event,
                                    final C context,
                                    final StateEnum condition) throws LogicViolationError {
        return trigger(event, context, condition, 0);
    }

    /**
     * If condition state do not match current, no handlers will be invoked.
     * @param repetition - describe how many times we can try to change state
     * @return false - when context terminated
     * @return false - when current state is different than expected in condition
     * @return false - when couldn't set a new condition - race condition
     * @return true - state changed, transtion scheduled
     * @throws LogicViolationError
     */
    boolean trigger(final EventEnum event, final C context, StateEnum condition, int repetition) throws LogicViolationError {

        if (context.isTerminated()){
            return false;
        }

        final StateEnum stateFrom = context.getStateValue();
        if (condition != null && stateFrom != condition) {
            logger.trace("Current state is different than expected in condition.");
            return false;
        }

        final Transition transition = transitions.getTransition(stateFrom, event);

        if (transition == null) {
            throw new LogicViolationError(String.format("Invalid Event: %s triggered while in State: %s for %s",
                    event, stateFrom, context));
        }

        try {
            if (context.getStateRef().compareAndSet(stateFrom, transition.getStateTo())) {
                transit(transition.getStateTo(), context);
            }else{

                if(repetition>0){
                    logger.info("Fail to change state due to parallel context change.");
                    return trigger(event, context, null, --repetition);
                }
                return false;
            }
        } catch (Exception e) {
            doOnError(new ExecutionError(stateFrom, event, e, "Execution Error in [trigger]", context));
        }
        return true;
    }

    void transit(final StateEnum targetState, final C context) {
        RunnableWrapper wrapper = context.getRunnableWrapper();
        wrapper.setRunnableMethod(() -> enter(targetState, context));
        if (!context.isTerminated()) {
            executor.execute(wrapper);
        }
    }

    protected void enter(final StateEnum state, final C context) {

        if (context.isTerminated()) {
            return;
        }

        try {
            logger.trace("When enter {} for {} <<<", state, context);

            handlers.callOnStateEntered(state, context);

            logger.trace("When enter {} for {} >>>", state, context);

            if (transitions.isFinal(state)) {
                doOnTerminate(state, context);
            }
        } catch (Exception e) {
            doOnError(new ExecutionError(state, null, e, "Execution Error in [whenEnter] handler", context));
        }
    }

    protected void doOnError(final ExecutionError error) {
        handlers.callOnError(error);
        doOnTerminate(error.getState(), (C) error.getContext());
    }

    protected void doOnTerminate(StateEnum state, final C context) {
        if (!context.isTerminated()) {
            try {
                logger.trace("Terminating context {}", context);

                context.setTerminated();
                handlers.callOnFinalState(state, context);
            } catch (Exception e) {
                logger.error("Execution Error in [whenTerminate] handler", e);
            }
        }
    }
}

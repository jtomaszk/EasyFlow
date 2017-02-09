package au.com.ds.ef;

import au.com.ds.ef.call.ContextHandler;
import au.com.ds.ef.call.EventHandler;
import au.com.ds.ef.call.ExecutionErrorHandler;
import au.com.ds.ef.call.StateHandler;
import au.com.ds.ef.err.ExecutionError;
import au.com.ds.ef.err.LogicViolationError;
import au.com.ds.ef.log.FlowLogger;
import au.com.ds.ef.log.FlowLoggerImpl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static au.com.ds.ef.HandlerCollection.EventType;

public class EasyFlow<C extends StatefulContext> implements Flow<C> {
    public class DefaultErrorHandler implements ExecutionErrorHandler<StatefulContext> {
        @Override
        public void call(ExecutionError error, StatefulContext context) {
            String msg = "Execution Error in StateHolder [" + error.getState() + "] ";
            if (error.getEvent() != null) {
                msg += "on EventHolder [" + error.getEvent() + "] ";
            }
            msg += "with Context [" + error.getContext() + "] ";

            Exception e = new Exception(msg, error);
            log.error("Error", e);
        }
    }

    private StateEnum startState;
    private TransitionCollection transitions;

    private Executor executor;

    private HandlerCollection handlers = new HandlerCollection();
    private boolean trace = false;
    private FlowLogger log = new FlowLoggerImpl();

    protected EasyFlow(StateEnum startState) {
        this.startState = startState;
        this.handlers.setHandler(HandlerCollection.EventType.ERROR, null, null, new DefaultErrorHandler());
    }

    public void processAllTransitions(boolean skipValidation) {
        List<Transition> cTransitions = RegularTransition.Repository.consume();
        if (cTransitions != null) {
            cTransitions = cTransitions.stream().filter(t -> t.getStateFrom() != null).collect(Collectors.toList());
        }
        transitions = new TransitionCollection(cTransitions, !skipValidation);
    }

    public void setTransitions(Collection<Transition> collection, boolean skipValidation) {
        transitions = new TransitionCollection(collection, !skipValidation);
    }

    private void prepare() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
    }

    public void start(boolean enterInitialState, final C context) {
        prepare();
        context.setFlow(this);

        if (context.getStateValue() == null) {
            setCurrentState(startState, false, context);
        } else if (enterInitialState) {
            setCurrentState(context.getStateValue(), true, context);
        }
    }

    protected void setCurrentState(final StateEnum state, final boolean enterInitialState, final C context) {
        transit(null, state, enterInitialState, context);
    }

    protected void transit(final StateEnum condition, final StateEnum targetState, final boolean enterInitialState, final C context) {
        RunnableWrapper wrapper = context.getRunnableWrapper();
        wrapper.setRunnableMethod(() -> {
            if (!enterInitialState) {
                StateEnum prevState = context.getStateValue();
                if (prevState != null) {
                    leave(prevState, context);
                }
            }

            if (casState(context, condition, targetState)) {
                enter(targetState, context);
            }

        });

        execute(wrapper, context);
    }

    protected void execute(Runnable task, final C context) {
        if (!context.isTerminated()) {
            executor.execute(task);
        }
    }

    protected boolean casState(final C context, StateEnum expectedState, StateEnum targetState) {
        if (expectedState != null) {
            return context.getStateRef().compareAndSet(expectedState, targetState);
        }else{
            context.getStateRef().set(targetState);
            return true;
        }
    }

    public Flow<C> whenEvent(EventEnum event, ContextHandler<C> onEvent) {
        handlers.setHandler(EventType.EVENT_TRIGGER, null, event, onEvent);
        return this;
    }

    public Flow<C> whenEvent(EventHandler<C> onEvent) {
        handlers.setHandler(EventType.ANY_EVENT_TRIGGER, null, null, onEvent);
        return this;
    }

    public Flow<C> whenEnter(StateEnum state, ContextHandler<? extends C> onEnter) {
        handlers.setHandler(EventType.STATE_ENTER, state, null, onEnter);
        return this;
    }

    public Flow<C> whenEnter(StateHandler<C> onEnter) {
        handlers.setHandler(EventType.ANY_STATE_ENTER, null, null, onEnter);
        return this;
    }

    public Flow<C> whenLeave(StateEnum state, ContextHandler<C> onEnter) {
        handlers.setHandler(EventType.STATE_LEAVE, state, null, onEnter);
        return this;
    }

    public Flow<C> whenLeave(StateHandler<C> onEnter) {
        handlers.setHandler(EventType.ANY_STATE_LEAVE, null, null, onEnter);
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

    public <C1 extends StatefulContext> EasyFlow<C1> executor(Executor executor) {
        this.executor = executor;
        return (EasyFlow<C1>) this;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> trace() {
        trace = true;
        return (EasyFlow<C1>) this;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> logger(FlowLogger log) {
        this.log = log;
        return (EasyFlow<C1>) this;
    }

    public List<Transition> getAvailableTransitions(StateEnum stateFrom) {
        return transitions.getTransitions(stateFrom);
    }

    public boolean isEventHandledByState(final StateEnum state, final EventEnum event) {
        for (Transition transition : transitions.getTransitions(state)) {
            if (transition.getEvent() == event) return true;
        }
        return false;
    }
    public boolean trigger(final EventEnum event, final C context) throws LogicViolationError {
        return trigger(event, context, null, false);
    }

    public boolean safeTrigger(final EventEnum event, final C context) {
        try {
            return trigger(event, context, null, true);
        } catch (LogicViolationError logicViolationError) {
            return false;
        }
    }
    public boolean conditionTrigger(final EventEnum event, final C context, final StateEnum condition) throws LogicViolationError {
        return trigger(event, context, condition, false);
    }

    /**
     * Concurrent modification of state can cause situation when event and leave handlers are invoked but not enter handlers.
     * If conditional state do not match current, no handlers will be invoked
     */
    private boolean trigger(final EventEnum event, final C context, final StateEnum condition, final boolean safe)
            throws LogicViolationError {

        if (context.isTerminated()) {
            return false;
        }

        final StateEnum stateFrom = context.getStateValue();
        final Transition transition = transitions.getTransition(stateFrom, event);

        if (condition!=null && stateFrom != condition) {
            return false;
        }

        if (transition != null) {

            RunnableWrapper wrapper = context.getRunnableWrapper();
            wrapper.setRunnableMethod(() -> {
                try {
                    StateEnum stateTo = transition.getStateTo();
                    if (isTrace())
                        log.info("when triggered %s in %s for %s <<<", event, stateFrom, context);

                    handlers.callOnEventTriggered(event, stateFrom, stateTo, context);

                    if (isTrace())
                        log.info("when triggered %s in %s for %s >>>", event, stateFrom, context);

                    transit(condition, stateTo, false, context);
                } catch (Exception e) {
                    doOnError(new ExecutionError(stateFrom, event, e,
                            "Execution Error in [trigger]", context));
                }
            });

            execute(wrapper, context);
        } else if (!safe) {
            throw new LogicViolationError("Invalid Event: " + event +
                    " triggered while in State: " + context.getStateValue() + " for " + context);
        }

        return transition != null;
    }

    private void enter(final StateEnum state, final C context) {
        if (context.isTerminated()) {
            return;
        }

        try {
            // first enter state
            if (isTrace())
                log.info("when enter %s for %s <<<", state, context);

            handlers.callOnStateEntered(state, context);

            if (isTrace())
                log.info("when enter %s for %s >>>", state, context);

            if (transitions.isFinal(state)) {
                doOnTerminate(state, context);
            }
        } catch (Exception e) {
            doOnError(new ExecutionError(state, null, e,
                    "Execution Error in [whenEnter] handler", context));
        }
    }

    private void leave(StateEnum state, final C context) {
        if (context.isTerminated()) {
            return;
        }

        try {
            if (isTrace())
                log.info("when leave %s for %s <<<", state, context);

            handlers.callOnStateLeaved(state, context);

            if (isTrace())
                log.info("when leave %s for %s >>>", state, context);
        } catch (Exception e) {
            doOnError(new ExecutionError(state, null, e,
                    "Execution Error in [whenLeave] handler", context));
        }
    }

    protected boolean isTrace() {
        return trace;
    }

    protected void doOnError(final ExecutionError error) {
        handlers.callOnError(error);
        doOnTerminate(error.getState(), (C) error.getContext());
    }

    public StateEnum getStartState() {
        return startState;
    }

    protected void doOnTerminate(StateEnum state, final C context) {
        if (!context.isTerminated()) {
            try {
                if (isTrace())
                    log.info("terminating context %s", context);

                context.setTerminated();
                handlers.callOnFinalState(state, context);
            } catch (Exception e) {
                log.error("Execution Error in [whenTerminate] handler", e);
            }
        }
    }
}

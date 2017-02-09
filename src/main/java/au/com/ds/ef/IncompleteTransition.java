package au.com.ds.ef;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Can't be final transition.
 */
public class IncompleteTransition extends RegularTransition {

    private static ThreadLocal<List<Transition>> defaultTransitions = new InheritableThreadLocal<List<Transition>>(){{
        set(new ArrayList<>());
    }};

    /**
     * Updates all transitions on(x).to(X) with stateFrom that comes from emit(x)
     */
    public static class Stub implements Transition {

        private final IncompleteTransition target;

        public Stub(IncompleteTransition incompleteTransition) {
            target = incompleteTransition;
        }

        @Override
        public EventEnum getEvent() {
            return target.getEvent();
        }

        @Override
        public StateEnum getStateFrom() {
            return target.getStateFrom();
        }

        @Override
        public StateEnum getStateTo() {
            return target.getStateTo();
        }

        @Override
        public boolean isFinal() {
            return target.isFinal();
        }

        @Override
        public void setStateFrom(StateEnum stateFrom) {
            target.setStateFrom(stateFrom);
        }

        @Override
        public Transition transit(Transition... transitions) {

            List<Transition> emitTransitions = Repository.get().stream().filter(t -> t.getStateTo() == null)
                    .peek(innerT ->
                            Stream.of(transitions)
                                    .filter(t -> t.getStateFrom() == null && t.getEvent() == innerT.getEvent())
                                    .findFirst()
                                    .ifPresent(t -> t.setStateFrom(innerT.getStateFrom()))
                    ).collect(Collectors.toList());

            Repository.get().removeAll(emitTransitions);

            target.getDefaultTransitions().stream().forEach(t -> t.setStateFrom(target.getStateTo()));

            return target;
        }
    }

    public IncompleteTransition(EventEnum event, StateEnum stateTo, List<Transition> defaultTransitions) {
        super(event, stateTo, false, defaultTransitions);
    }

    public static IncompleteTransition from(StateEnum startState) {
        return from(startState, null);
    }

    public static IncompleteTransition from(StateEnum startState, List<Transition> dT) {
        defaultTransitions.set(dT);
        return new IncompleteTransition(null, startState, dT);
    }

    public static FlowBuilder.ToHolder on(EventEnum event) {
        return new FlowBuilder.ToHolder(event, defaultTransitions.get());
    }

    public Stub accept(EventEnum event){
        this.event = event;
        return new Stub(this);
    }

    @Override
    public IncompleteTransition transit(Transition... transitions) {
        return (IncompleteTransition)super.transit(transitions);
    }

    protected List<Transition> getDefaultTransitions(){
        return defaultTransitions.get();
    }
}

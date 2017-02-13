package au.com.ds.ef;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Constructed sub-flow, can't be reused - uniqueness of state has to be preserved.
 * Events has to be unique across all sub flows when provided as reference to ToHolder.subflow method
 *
 */
public class IncompleteTransition extends RegularTransition {

    public static class LateExecution implements Transition{

        final Supplier<IncompleteTransition> factory;

        final EventEnum event;

        public LateExecution(Supplier<IncompleteTransition> factory, EventEnum event) {
            this.factory = factory;
            this.event = event;
        }

        @Override
        public Transition transit(Transition... transitions) {
            return factory.get().accept(event).transit(transitions);
        }
    }

    /**
     * Updates all transitions on(x).to(X) with stateFrom that comes from emit(x)
     */
    private static class Proxy implements Transition {

        private final IncompleteTransition target;

        public Proxy(IncompleteTransition incompleteTransition) {
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

        /**
         * From all transactions created by 'emit' function (TE) in thread local repository,
         * match with sub transitions (ST) by event type.
         * Update ST state-from field.
         * Remove matched TE.
         */
        @Override
        public Transition transit(Transition... transitions) {

            Repository.get().removeIf( t->t.getStateTo() == null && updateOuterTransaction(t, transitions));

            if(target.defaultTransitions!=null) {
                target.defaultTransitions.stream().forEach(t -> t.setStateFrom(target.getStateTo()));
            }

            return target;
        }

        private boolean updateOuterTransaction(Transition innerT, Transition[] transitions) {
            return Stream.of(transitions)
                    .filter(t -> t.getStateFrom() == null && t.getEvent() == innerT.getEvent())
                    .findFirst()
                    .map( tr->{
                        tr.setStateFrom(innerT.getStateFrom());
                        return true;
                    }).orElse(Boolean.FALSE);
        }
    }

    public IncompleteTransition(EventEnum event, StateEnum stateTo, List<Transition> defaultTransitions) {
        super(event, stateTo, false, defaultTransitions);
    }

    public static IncompleteTransition from(StateEnum startState) {
        return from(startState, null);
    }

    public static IncompleteTransition from(StateEnum startState, List<Transition> dt) {
        ToHolder.resetDefaultTransitions(dt);
        return new IncompleteTransition(null, startState, dt);
    }

    public Proxy accept(EventEnum event){
        this.event = event;
        return new Proxy(this);
    }

    @Override
    public IncompleteTransition transit(Transition... transitions) {
        return (IncompleteTransition)super.transit(transitions);
    }
}

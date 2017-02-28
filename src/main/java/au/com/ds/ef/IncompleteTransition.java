package au.com.ds.ef;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Constructed sub-flow, can't be reused - uniqueness of state has to be preserved.
 * Events has to be unique across all sub flows when provided as reference to ToHolder.subflow method
 *
 */
public class IncompleteTransition extends RegularTransition {

    /**
     * Use when cascading subflows with the same events could result in overriding transitions.
     * Because creating transitions is postponed until transit method,
     * supplier function with subflows can be used as final step.
     */
    public static class LateExecution implements Transition{

        final Supplier<IncompleteTransition> factory;

        final EventEnum event;

        private StateEnum stateFrom;

        public LateExecution(Supplier<IncompleteTransition> factory, EventEnum event) {
            this.factory = factory;
            this.event = event;
        }

        @Override
        public Transition transit(Transition... transitions) {
            Proxy proxy = factory.get().accept(event);
            proxy.setStateFrom(stateFrom);
            return proxy.transit(transitions);
        }

        @Override
        public void setStateFrom(StateEnum stateFrom) {
            this.stateFrom = stateFrom;
        }

        @Override
        public EventEnum getEvent() {
            return event;
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
         *
         * Update with From value only not overridden default transitions
         */
        @Override
        public Transition transit(Transition... transitions) {

            Repository.get().removeIf( t->t.getStateTo() == null && updateOuterTransaction(t, transitions));

            Repository.get().stream()
                    .filter( t->t.getStateTo() == null)
                    .forEach( t-> updateInnerTransactions(t, transitions));

            if(target.defaultTransitions!=null) {

                Set<EventEnum> targetsEvents = Repository.get().stream()
                        .filter(t -> t.getStateFrom() == target.getStateTo())
                        .map(t->t.getEvent())
                        .collect(Collectors.toSet());

                target.defaultTransitions.stream()
                        .filter(dt->targetsEvents.contains(dt.getEvent())==false)
                        .forEach(t -> t.setStateFrom(target.getStateTo()));
            }

            return target;
        }

        //update inner transition, it happens when sub-flows emits same event more than once
        private void updateInnerTransactions(Transition innerT, Transition[] transitions) {

            Optional<Transition> outerTransitionOpt = Stream.of(transitions)
                    .filter(t -> t.getEvent() == innerT.getEvent())
                    .findFirst();

            if(outerTransitionOpt.isPresent()){
                innerT.setStateTo(outerTransitionOpt.get().getStateTo());
            }
        }

        //updates outer transitions with stateFrom from sub-flow emits
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

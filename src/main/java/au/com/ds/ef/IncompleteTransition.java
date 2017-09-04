package au.com.ds.ef;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

/**
 * Constructed sub-flow, can't be reused - uniqueness of state has to be preserved.
 * Events has to be unique across all sub flows when provided as reference to ToHolder.subflow method
 */
public class IncompleteTransition extends RegularTransition {

    /**
     * Use when cascading subflows with the same events could result in overriding transitions.
     * Because creating transitions is postponed until transit method,
     * supplier function with subflows can be used as final step.
     */
    public static class LateExecution extends Transition {

        final Supplier<IncompleteTransition> factory;

        final EventEnum event;

        private StateEnum stateFrom;

        public LateExecution(Supplier<IncompleteTransition> factory, EventEnum event) {
            this.factory = factory;
            this.event = event;
        }

        @Override
        public Transition transit(Transition... transitions) {
            Proxy proxy = factory.get().accept(Lists.newArrayList(event));
            proxy.propagateStateFrom(stateFrom);
            return proxy.transit(transitions);
        }

        @Override
        public void propagateStateFrom(StateEnum stateFrom) {
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
    static class Proxy extends Transition {

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
        public List<Transition> getDerivedTransitions() {
            return target.getDerivedTransitions();
        }

        @Override
        public void propagateStateFrom(StateEnum stateFrom) {
            target.propagateStateFrom(stateFrom);
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
         * <p>
         * Update with From value only not overridden default transitions
         */
        @Override
        public Transition transit(final Transition... transitions) {

            Repository.get().removeAll(
                    FluentIterable.from(Repository.get())
                            .filter(new Predicate<Transition>() {
                                @Override
                                public boolean apply(Transition t) {
                                    return t.getStateTo() == null && updateOuterTransaction(t, transitions);
                                }
                            }).toList()
            );

            if (target.defaultTransitions != null) {

                final Set<EventEnum> targetsEvents = FluentIterable.from(Repository.get())
                        .filter(new Predicate<Transition>() {
                            @Override
                            public boolean apply(Transition t) {
                                return t.getStateFrom() == target.getStateTo();
                            }
                        }).transform(new Function<Transition, EventEnum>() {
                            @Override
                            public EventEnum apply(Transition t) {
                                return t.getEvent();
                            }
                        }).toSet();

                FluentIterable<Transition> filtered = FluentIterable.from(target.defaultTransitions)
                        .filter(new Predicate<Transition>() {
                            @Override
                            public boolean apply(Transition dt) {
                                return !targetsEvents.contains(dt.getEvent());
                            }
                        });
                for (Transition t : filtered) {
                    t.propagateStateFrom(target.getStateTo());
                }
            }

            return target;
        }

        private boolean updateOuterTransaction(final Transition innerT, Transition[] transitions) {
            return !FluentIterable.of(transitions)
                    .transformAndConcat(new Function<Transition, Iterable<Transition>>() {
                        @Override
                        public Iterable<Transition> apply(Transition t) {
                            List<Transition> list = Lists.newArrayList(t);
                            list.addAll(t.getDerivedTransitions());
                            return list;
                        }
                    }).filter(new Predicate<Transition>() {
                        @Override
                        public boolean apply(Transition t) {
                            return t.getStateFrom() == null && t.getEvent() == innerT.getEvent();
                        }
                    }).transform(new Function<Transition, Boolean>() {
                        @Override
                        public Boolean apply(Transition t) {
                            t.setStateFrom(innerT.getStateFrom());
                            return true;
                        }
                    }).toList().isEmpty();
        }
    }

    public IncompleteTransition(List<EventEnum> event, StateEnum stateTo, List<Transition> defaultTransitions) {
        super(event, stateTo, false, defaultTransitions);
    }

    public static IncompleteTransition from(StateEnum startState) {
        return from(startState, null);
    }

    public static IncompleteTransition from(StateEnum startState, List<Transition> dt) {
        ToHolder.resetDefaultTransitions(dt);
        return new IncompleteTransition(null, startState, dt);
    }

    public Proxy accept(List<EventEnum> events) {
        this.event = events.remove(0);

        if (events.size() > 1) {
            this.derivedTransitions = FluentIterable.from(events)
                    .transform(new Function<EventEnum, Transition>() {
                        @Override
                        public Transition apply(EventEnum e) {
                            return new RegularTransition(e, getStateTo(), isFinal());
                        }
                    }).toList();
        }

        return new Proxy(this);
    }

    @Override
    public IncompleteTransition transit(Transition... transitions) {
        return (IncompleteTransition) super.transit(transitions);
    }
}

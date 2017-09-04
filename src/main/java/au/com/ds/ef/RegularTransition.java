package au.com.ds.ef;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import java.util.LinkedList;
import java.util.List;

public class RegularTransition extends Transition {

    protected EventEnum event;
    private StateEnum stateFrom;
    private StateEnum stateTo;
    private boolean isFinal;
    protected List<Transition> defaultTransitions = null;
    protected List<Transition> derivedTransitions = new LinkedList<Transition>();

    static RegularTransition spanTransitionTree(
            List<EventEnum> events, StateEnum stateTo, boolean isFinal, List<Transition> dt) {
        return new RegularTransition(events, stateTo, isFinal, dt);
    }

    static RegularTransition createSingleTransition(EventEnum event, StateEnum stateTo, boolean isFinal) {
        return new RegularTransition(event, stateTo, isFinal);
    }

    protected RegularTransition(List<EventEnum> events, final StateEnum stateTo, final boolean isFinal, List<Transition> dt) {
        this.event = events != null ? events.remove(0) : null;
        this.stateTo = stateTo;
        this.isFinal = isFinal;

        if (dt != null && !dt.isEmpty()) {
            this.defaultTransitions = FluentIterable.from(dt)
                    .transform(new Function<Transition, Transition>() {
                        @Override
                        public Transition apply(Transition t) {
                            return new RegularTransition(t.getEvent(), t.getStateTo(), t.isFinal());
                        }
                    }).toList();
        }

        if (events != null && !events.isEmpty()) {
            this.derivedTransitions = FluentIterable.from(events)
                    .transform(new Function<EventEnum, Transition>() {
                        @Override
                        public Transition apply(EventEnum e) {
                            return new RegularTransition(e, stateTo, isFinal);
                        }
                    })
                    .toList();
        }

        Repository.get().add(this);
    }

    protected RegularTransition(EventEnum event, StateEnum stateTo, boolean isFinal) {
        this.event = event;
        this.stateTo = stateTo;
        this.isFinal = isFinal;
        Repository.get().add(this);
    }

    public Transition transit(Transition... transitions) {
        List<Transition> list = TransitionUtil.composeTransitions(defaultTransitions, transitions);
        for (Transition t : list) {
            t.propagateStateFrom(stateTo);
        }
        return this;
    }

    public EventEnum getEvent() {
        return event;
    }

    public void propagateStateFrom(final StateEnum stateFrom) {
        this.stateFrom = stateFrom;
        for (Transition dt : derivedTransitions) {
            dt.propagateStateFrom(stateFrom);
        }
    }

    public void setStateFrom(StateEnum stateFrom) {
        this.stateFrom = stateFrom;
    }

    public StateEnum getStateFrom() {
        return stateFrom;
    }

    public StateEnum getStateTo() {
        return stateTo;
    }

    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public List<Transition> getDerivedTransitions() {
        return derivedTransitions;
    }

    @Override
    public String toString() {
        return "Transition{" +
                "event=" + event +
                ", stateFrom=" + stateFrom +
                ", stateTo=" + stateTo +
                '}';
    }
}
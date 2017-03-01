package au.com.ds.ef;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class RegularTransition implements Transition{

    protected EventEnum event;
    private StateEnum stateFrom;
    private StateEnum stateTo;
    private boolean isFinal;
    protected List<Transition> defaultTransitions = null;
    protected List<Transition> derivedTransitions = new LinkedList<>();

    static RegularTransition spanTransitionTree(
            List<EventEnum> events, StateEnum stateTo, boolean isFinal, List<Transition> dt){
        return new RegularTransition(events, stateTo, isFinal, dt);
    }

    static RegularTransition createSingleTransition(EventEnum event, StateEnum stateTo, boolean isFinal){
        return new RegularTransition(event, stateTo, isFinal);
    }

    protected RegularTransition(List<EventEnum> events, StateEnum stateTo, boolean isFinal, List<Transition> dt) {
        this.event = events!=null ? events.remove(0) : null;
        this.stateTo = stateTo;
        this.isFinal = isFinal;

        if(dt!=null && !dt.isEmpty()){
            this.defaultTransitions = dt.stream()
                    .map( t-> new RegularTransition(t.getEvent(), t.getStateTo(), t.isFinal()))
                    .collect(Collectors.toList());
        }

        if(events!=null && !events.isEmpty()) {
            this.derivedTransitions = events.stream()
                    .map(e -> new RegularTransition(e, stateTo, isFinal))
                    .collect(Collectors.toList());
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
        TransitionUtil.composeTransitions(defaultTransitions, transitions).forEach(t -> t.propagateStateFrom(stateTo));
        return this;
    }

    //

    public EventEnum getEvent() {
        return event;
    }

    public void propagateStateFrom(StateEnum stateFrom) {
        this.stateFrom = stateFrom;
        this.derivedTransitions.forEach( dt->dt.propagateStateFrom(stateFrom));
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
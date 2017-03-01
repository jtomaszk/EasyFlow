package au.com.ds.ef;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User: andrey
 * Date: 6/12/2013
 * Time: 2:21 PM
 */
public class RegularTransition implements Transition{

    protected EventEnum event;
    private StateEnum stateFrom;
    private StateEnum stateTo;
    private boolean isFinal;
    protected List<Transition> defaultTransitions = null;

    public RegularTransition(EventEnum event, StateEnum stateFrom, StateEnum stateTo, boolean isFinal) {
        this.event = event;
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        this.isFinal = isFinal;
    }

    protected RegularTransition(EventEnum event, StateEnum stateTo, boolean isFinal, List<Transition> dt) {
        this.event = event;
        this.stateTo = stateTo;
        this.isFinal = isFinal;

        if(dt!=null && !dt.isEmpty()){
            this.defaultTransitions = dt.stream()
                    .map( t-> new RegularTransition(t.getEvent(), t.getStateTo(), t.isFinal(), null))
                    .collect(Collectors.toList());
        }

        Repository.get().add(this);
    }

    public Transition transit(Transition... transitions) {
        TransitionUtil.composeTransitions(defaultTransitions, transitions).forEach(t -> t.setStateFrom(stateTo));
        return this;
    }

    //

    public EventEnum getEvent() {
        return event;
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
    public String toString() {
        return "Transition{" +
                "event=" + event +
                ", stateFrom=" + stateFrom +
                ", stateTo=" + stateTo +
                '}';
    }
}
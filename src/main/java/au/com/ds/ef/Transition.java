package au.com.ds.ef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * User: andrey
 * Date: 6/12/2013
 * Time: 2:21 PM
 */
public final class Transition {
    private static ThreadLocal<List<Transition>> transitions = new ThreadLocal<List<Transition>>();
    private EventEnum event;
    private StateEnum stateFrom;
    private StateEnum stateTo;
    private boolean isFinal;
    private List<Transition> defaultTransitions = null;

    public Transition(EventEnum event, StateEnum stateFrom, StateEnum stateTo) {
        this.event = event;
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        this.isFinal = false;
    }

    public Transition(EventEnum event, StateEnum stateFrom, StateEnum stateTo, boolean isFinal) {
        this.event = event;
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        this.isFinal = isFinal;
    }

    protected Transition(EventEnum event, StateEnum stateTo, boolean isFinal, List<Transition> dt) {
        this.event = event;
        this.stateTo = stateTo;
        this.isFinal = isFinal;

        if(dt!=null){
            this.defaultTransitions = dt.stream()
                    .map( t-> new Transition(t.getEvent(), t.getStateTo(), t.isFinal, null))
                    .collect(Collectors.toList());
        }

        register(this);
    }

    private static void register(Transition transition) {
        List<Transition> list = transitions.get();
        if (list == null) {
            list = new ArrayList<>();
            transitions.set(list);
        }
        list.add(transition);
    }

    public EventEnum getEvent() {
        return event;
    }

    protected void setStateFrom(StateEnum stateFrom) {
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

    public Transition transit(Transition... transitions) {

        if (this.defaultTransitions!=null){

            composeTransitions(defaultTransitions, transitions).forEach(t -> t.setStateFrom(stateTo));
        } else {
            for (Transition transition : transitions) {
                transition.setStateFrom(stateTo);
            }
        }

        return this;
    }

    private Stream<Transition> composeTransitions(List<Transition> dtList, Transition[] transitions) {

        return Stream.concat(
                Arrays.stream(transitions),
                dtList.stream().filter(dt -> !isOverridePresent(dt, transitions)));
    }

    private boolean isOverridePresent(Transition dt, Transition[] transitions) {
        return Arrays.stream(transitions).filter(t -> t.getEvent()==dt.getEvent()).findFirst().isPresent();
    }

    @Override
    public String toString() {
        return "Transition{" +
                "event=" + event +
                ", stateFrom=" + stateFrom +
                ", stateTo=" + stateTo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transition that = (Transition) o;

        if (!event.equals(that.event)) return false;
        if (!stateFrom.equals(that.stateFrom)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = event.hashCode();
        result = 31 * result + stateFrom.hashCode();
        return result;
    }

    protected static List<Transition> consumeTransitions() {
        List<Transition> ts = transitions.get();
        transitions.remove();
        return ts;
    }
}
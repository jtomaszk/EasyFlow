package au.com.ds.ef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Transition {

    public EventEnum getEvent() {
        throw new UnsupportedOperationException();
    }

    public StateEnum getStateFrom() {
        throw new UnsupportedOperationException();
    }

    public StateEnum getStateTo() {
        throw new UnsupportedOperationException();
    }

    public boolean isFinal() {
        throw new UnsupportedOperationException();
    }

    public void setStateFrom(StateEnum stateFrom) {
        throw new UnsupportedOperationException();
    }

    public void propagateStateFrom(StateEnum stateFrom) {
        throw new UnsupportedOperationException();
    }

    public List<Transition> getDerivedTransitions() {
        return Collections.emptyList();
    }

    public abstract Transition transit(Transition... transitions);

    static class Repository {
        private final static ThreadLocal<List<Transition>> transitions = new ThreadLocal<List<Transition>>() {{
            set(new ArrayList<Transition>());
        }};

        static List<Transition> get() {
            return transitions.get();
        }

        static List<Transition> consume() {
            List<Transition> ts = transitions.get();
            transitions.set(new ArrayList<Transition>());
            return ts;
        }
    }
}


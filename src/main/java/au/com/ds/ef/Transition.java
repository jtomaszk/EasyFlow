package au.com.ds.ef;

import java.util.ArrayList;
import java.util.List;

public interface Transition {

    class Repository{
        private final static ThreadLocal<List<Transition>> transitions = new ThreadLocal<List<Transition>>(){{
            set(new ArrayList<>());
        }};

        static List<Transition> get(){
            return transitions.get();
        }

        static List<Transition> consume() {
            List<Transition> ts = transitions.get();
            transitions.set(new ArrayList<>());
            return ts;
        }
    }

    EventEnum getEvent();

    StateEnum getStateFrom();

    StateEnum getStateTo();

    boolean isFinal();

    void setStateFrom(StateEnum stateFrom);

    Transition transit(Transition... transitions);
}

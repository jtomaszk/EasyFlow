package au.com.ds.ef;

import java.util.ArrayList;
import java.util.List;

public class ToHolder {

    private static ThreadLocal<List<Transition>> defaultTransitions = new InheritableThreadLocal<List<Transition>>(){{
        set(new ArrayList<>());
    }};

    public static void resetDefaultTransitions(){
        defaultTransitions.set(new ArrayList<>());
    }

    public static void resetDefaultTransitions(List<Transition> dt){
        if(dt!=null){
            defaultTransitions.set(dt);
        }else{
            resetDefaultTransitions();
        }
    }

    public static List<Transition> getDefaultTransitions(){
        List<Transition> clone = new ArrayList<>();
        clone.addAll(defaultTransitions.get());
        return clone;
    }

    private EventEnum event;

    public ToHolder(EventEnum event) {
        this.event = event;
    }


    public static ToHolder on(EventEnum event) {
        return new ToHolder(event);
    }

    public static Transition emit(EventEnum event) {
        return new RegularTransition(event, null, false, null);
    }

    public Transition subFlow(IncompleteTransition incompleteTransition){
        return incompleteTransition.accept(event);
    }

    public Transition to(StateEnum state) {
        return new RegularTransition(event, state, false, defaultTransitions.get());
    }

    public Transition finish(StateEnum state) {
        return new RegularTransition(event, state, true, null);
    }

    public Transition backTo(StateEnum state) {
        return new RegularTransition(event, state, false, null);
    }
}


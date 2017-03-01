package au.com.ds.ef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

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

    private EventEnum event[];

    public ToHolder(EventEnum... event) {
        if(event==null && event.length>0) throw new IllegalArgumentException("Non empty array of events required.");
        this.event = event;
    }

    public static ToHolder on(EventEnum... event) {
        return new ToHolder(event);
    }

    public static Transition emit(EventEnum event) {
        return RegularTransition.createSingleTransition(event, null, false);
    }

    public Transition subFlow(IncompleteTransition incompleteTransition){
        return incompleteTransition.accept(new ArrayList<>(Arrays.asList(event)));
    }

    public Transition subFlow(Supplier<IncompleteTransition> itSupplier){

        if(event.length>1) throw new IllegalStateException("Can't transit group of events to sub-flow supplier.");

        return new IncompleteTransition.LateExecution(itSupplier, event[0]);
    }

    public Transition to(StateEnum state) {

        return RegularTransition.spanTransitionTree(new ArrayList<>(Arrays.asList(event)), state, false, defaultTransitions.get());
    }

    public Transition finish(StateEnum state) {
        return RegularTransition.spanTransitionTree(new ArrayList<>(Arrays.asList(event)), state, true, null);
    }

    public Transition backTo(StateEnum state) {
        return RegularTransition.spanTransitionTree(new ArrayList<>(Arrays.asList(event)), state, false, null);
    }
}


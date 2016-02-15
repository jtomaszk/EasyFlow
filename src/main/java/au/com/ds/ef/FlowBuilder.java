package au.com.ds.ef;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class FlowBuilder<C extends StatefulContext> {
    private final EasyFlow<C> flow;

    private static ThreadLocal<List<Transition>> defaultTransitions = new InheritableThreadLocal<>();

    public static class ToHolder {
        private EventEnum event;
        private final List<Transition> defaultTransitions;

        private ToHolder(EventEnum event, List<Transition> defaultTransitions) {
            this.event = event;
            this.defaultTransitions = defaultTransitions;
        }

        public Transition to(StateEnum state) {
            return new Transition(event, state, false, defaultTransitions);
        }

        public Transition finish(StateEnum state) {
            return new Transition(event, state, true, null);
        }


        public Transition backTo(StateEnum state) {
            return new Transition(event, state, false, null);
        }
    }

    private FlowBuilder(StateEnum startState) {
        this(startState, null);
    }

    private FlowBuilder(StateEnum startState, List<Transition> defaultTransitions) {
        this.flow = new EasyFlow<C>(startState);
    }

    public static <C extends StatefulContext> FlowBuilder<C> from(StateEnum startState) {

        return new FlowBuilder<C>(startState);
    }

    public static <C extends StatefulContext> FlowBuilder<C> from(StateEnum startState, List<Transition> dt) {

        defaultTransitions.set(dt);

        return new FlowBuilder<C>(startState, dt);
    }

    public static <C extends StatefulContext> EasyFlow<C> fromTransitions(
            StateEnum startState, Collection<Transition> transitions, boolean skipValidation) {
        EasyFlow<C> flow = new EasyFlow<C>(startState);
        flow.setTransitions(transitions, skipValidation);
        return flow;
    }

    public static ToHolder on(EventEnum event) {
        return new ToHolder(event, defaultTransitions.get());
    }

    public <C1 extends StatefulContext> EasyFlow<C1> transit(Transition... transitions) {

        if(defaultTransitions.get()!=null){
            return transit(false, this.defaultTransitions.get(), transitions);
        }else{
            return transit(false, transitions);
        }
    }

    public <C1 extends StatefulContext> EasyFlow<C1> transit(
            boolean skipValidation,List<Transition> dt, Transition... transitions) {

        Stream.concat(Arrays.stream(transitions), dt.stream())
                .forEach( t -> t.setStateFrom(flow.getStartState()));

        flow.processAllTransitions(skipValidation);

        defaultTransitions.remove();

        return (EasyFlow<C1>) flow;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> transit(boolean skipValidation, Transition... transitions) {
        for (Transition transition : transitions) {
            transition.setStateFrom(flow.getStartState());
        }
        flow.processAllTransitions(skipValidation);

        return (EasyFlow<C1>) flow;
    }
}

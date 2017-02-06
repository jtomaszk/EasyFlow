package au.com.ds.ef;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class FlowBuilder<F extends Flow> {

    protected final F flow;

    private static ThreadLocal<List<Transition>> defaultTransitions = new InheritableThreadLocal<>();

    protected FlowBuilder(F flow) {
        this.flow = flow;
    }

    public static class EasyFlowBuilder extends FlowBuilder<EasyFlow>{

        private EasyFlowBuilder(StateEnum startState) {
            super(new EasyFlow(startState));
        }

        public static EasyFlowBuilder from(StateEnum startState) {
            return new EasyFlowBuilder(startState);
        }

        public static EasyFlowBuilder from(StateEnum startState, List<Transition> dt) {
            defaultTransitions.set(dt);
            return new EasyFlowBuilder(startState);
        }

        public static EasyFlow fromTransitions(
                StateEnum startState, Collection<Transition> transitions, boolean skipValidation) {
            EasyFlow flow = new EasyFlow(startState);
            flow.setTransitions(transitions, skipValidation);
            return flow;
        }
    }

    public static class EnterFlowBuilder extends FlowBuilder<EnterFlow> {

        private EnterFlowBuilder(StateEnum startState) {
            super(new EnterFlow(startState));
        }

        public static EnterFlowBuilder from(StateEnum startState) {
            return new EnterFlowBuilder(startState);
        }

        public static EnterFlowBuilder from(StateEnum startState, List<Transition> dt) {
            defaultTransitions.set(dt);
            return new EnterFlowBuilder(startState);
        }
    }


    public static ToHolder on(EventEnum event) {
        return new ToHolder(event, defaultTransitions.get());
    }

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

    public F transit(Transition... transitions) {

        if(defaultTransitions.get()!=null){
            return transit(false, defaultTransitions.get(), transitions);
        }else{
            return transit(false, transitions);
        }
    }

    public F transit(boolean skipValidation, Transition... transitions) {
        for (Transition transition : transitions) {
            transition.setStateFrom(flow.getStartState());
        }
        flow.processAllTransitions(skipValidation);

        return flow;
    }

    public F transit(
            boolean skipValidation,List<Transition> dt, Transition... transitions) {

        composeTransitions(dt, transitions).forEach( t -> t.setStateFrom(flow.getStartState()));

        flow.processAllTransitions(skipValidation);

        defaultTransitions.remove();

        return flow;
    }

    private Stream<Transition> composeTransitions(List<Transition> dtList, Transition[] transitions) {

        return Stream.concat(
                Arrays.stream(transitions),
                dtList.stream().filter(dt -> !isOverridePresent(dt, transitions)));
    }

    private boolean isOverridePresent(Transition dt, Transition[] transitions) {
        return Arrays.stream(transitions).filter(t -> t.getEvent()==dt.getEvent()).findFirst().isPresent();
    }
}

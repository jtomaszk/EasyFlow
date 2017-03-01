package au.com.ds.ef;

import java.util.Collection;
import java.util.List;

public class FlowBuilder<F extends Flow> {

    protected final F flow;

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
            ToHolder.resetDefaultTransitions(dt);
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
            ToHolder.resetDefaultTransitions(dt);
            return new EnterFlowBuilder(startState);
        }
    }

    public F transit(Transition... transitions) {
        return transit(false, ToHolder.getDefaultTransitions(), transitions);
    }

    public F transit(boolean skipValidation, List<Transition> dt, Transition... transitions) {

        TransitionUtil.composeTransitions(dt, transitions).forEach( t -> t.propagateStateFrom(flow.getStartState()));

        flow.processAllTransitions(skipValidation);

        return flow;
    }
}

package au.com.ds.ef;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FlowBuilder<C extends StatefulContext> {
    private final EasyFlow<C> flow;

    private final Optional<Stream<Transition>> defaultTransitions;

    public static class ToHolderBulk {
        Stream<EventEnum> events;

        public ToHolderBulk(EventEnum... events) {
            this.events = Arrays.stream(events);
        }

        public Stream<Transition> to(StateEnum state) {
            return doTransition(state, false);
        }

        public Stream<Transition> finish(StateEnum state) {
            return doTransition(state, true);
        }

        private Stream<Transition> doTransition(StateEnum state, boolean isFinal) {
           return events.map( event -> new Transition(event, state, isFinal));
        }
    }

    public static class ToHolder {
        private EventEnum event;

        private ToHolder(EventEnum event) {
            this.event = event;
        }

        public Transition to(StateEnum state) {
            return new Transition(event, state, false);
        }

        public Transition finish(StateEnum state) {
            return new Transition(event, state, true);
        }
    }

    private FlowBuilder(StateEnum startState) {
        this(startState, null);
    }

    private FlowBuilder(StateEnum startState, Stream<Transition> defaultTransitions) {
        this.flow = new EasyFlow<C>(startState);
        this.defaultTransitions = Optional.ofNullable(defaultTransitions);
    }

    public static <C extends StatefulContext> FlowBuilder<C> from(StateEnum startState) {
        return new FlowBuilder<C>(startState);
    }

    public static <C extends StatefulContext> FlowBuilder<C> from(StateEnum startState, Stream<Transition> dt) {

        return new FlowBuilder<C>(startState, dt);
    }

    public static <C extends StatefulContext> EasyFlow<C> fromTransitions(StateEnum startState,
                                                                          Collection<Transition> transitions, boolean skipValidation) {
        EasyFlow<C> flow = new EasyFlow<C>(startState);
        flow.setTransitions(transitions, skipValidation);
        return flow;
    }

    public static ToHolder on(EventEnum event) {
        return new ToHolder(event);
    }

    public static ToHolderBulk on(EventEnum... events) {
        return new ToHolderBulk(events);
    }

    public <C1 extends StatefulContext> EasyFlow<C1> transit(Transition... transitions) {

        if(this.defaultTransitions.isPresent()){
            return transit(false, this.defaultTransitions.get(), transitions);
        }else{
            return transit(false, transitions);
        }
    }

    public <C1 extends StatefulContext> EasyFlow<C1> transit(
            boolean skipValidation,Stream<Transition> defaultTransitions, Transition... transitions) {

        Stream.concat(defaultTransitions, Arrays.stream(transitions))
                .forEach( t -> t.setStateFrom(flow.getStartState()));

        flow.processAllTransitions(skipValidation);

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

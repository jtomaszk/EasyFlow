package au.com.ds.ef;

import au.com.ds.ef.call.ContextHandler;
import au.com.ds.ef.call.EventHandler;
import au.com.ds.ef.call.ExecutionErrorHandler;
import au.com.ds.ef.call.StateHandler;
import au.com.ds.ef.err.ExecutionError;
import au.com.ds.ef.err.LogicViolationError;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static au.com.ds.ef.FlowBuilder.EasyFlowBuilder.from;
import static au.com.ds.ef.FlowBuilder.EasyFlowBuilder.fromTransitions;
import static au.com.ds.ef.RunSingleTest.Events.event_1;
import static au.com.ds.ef.RunSingleTest.Events.event_2;
import static au.com.ds.ef.RunSingleTest.Events.event_3;
import static au.com.ds.ef.RunSingleTest.Events.event_4;
import static au.com.ds.ef.RunSingleTest.Events.event_5;
import static au.com.ds.ef.RunSingleTest.States.START;
import static au.com.ds.ef.RunSingleTest.States.STATE_1;
import static au.com.ds.ef.RunSingleTest.States.STATE_2;
import static au.com.ds.ef.RunSingleTest.States.STATE_3;
import static au.com.ds.ef.RunSingleTest.States.STATE_4;
import static au.com.ds.ef.ToHolder.on;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class RunSingleTest {
    public enum States implements StateEnum {
        START, STATE_1, STATE_2, STATE_3, STATE_4
    }

    public enum Events implements EventEnum {
        event_1, event_2, event_3, event_4, event_5
    }

    @Test
    public void testInvalidEvent() throws LogicViolationError {
        final Exception[] exception = new Exception[]{null};

        // state machine definition
        final EasyFlow<StatefulContext> flow =

                from(START).transit(
                        on(event_1).to(STATE_1).transit(
                                on(event_2).finish(STATE_2)
                        )
                );

        // handlers definition
        flow
                .whenError(new ExecutionErrorHandler() {
                    @Override
                    public void call(ExecutionError error, StatefulContext context) {
                        exception[0] = error;
                    }
                })

                .whenEnter(START, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        context.trigger(event_2);
                    }
                });

        StatefulContext ctx = new StatefulContext();
        flow.trace().start(ctx);
        ctx.awaitTermination();

        assertNotNull("Exception must be thrown during flow execution", exception[0]);
        assertTrue("Exception type should be ExecutionError", exception[0] instanceof ExecutionError);
        assertTrue("Exception cause should be LogicViolationError", exception[0].getCause() instanceof LogicViolationError);
    }

    @Test
    public void testEventsOrder() throws LogicViolationError {
        EasyFlow<StatefulContext> flow =

                from(START).transit(
                        on(event_1).to(STATE_1).transit(
                                on(event_2).finish(STATE_2),
                                on(event_3).to(STATE_3).transit(
                                        on(event_4).to(STATE_1),
                                        on(event_5).finish(STATE_4)
                                )
                        )
                );

        final Collection<Integer> actualOrder = Collections.synchronizedCollection(new ArrayList<Integer>());

        flow
                .whenEnter(START, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        actualOrder.add(1);
                        context.trigger(event_1);
                    }
                })
                .whenLeave(START, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) {
                        actualOrder.add(2);
                    }
                })

                .whenEnter(STATE_1, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        actualOrder.add(3);
                        context.trigger(event_2);
                    }
                }).whenLeave(STATE_1, new ContextHandler<StatefulContext>() {
            @Override
            public void call(StatefulContext context) {
                actualOrder.add(4);
            }
        })

                .whenEnter(STATE_2, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) {
                        actualOrder.add(5);
                    }
                }).whenLeave(STATE_2, new ContextHandler<StatefulContext>() {
            @Override
            public void call(StatefulContext context) {
                throw new RuntimeException("It never leaves the final state");
            }
        })

                .whenFinalState(new StateHandler<StatefulContext>() {
                    @Override
                    public void call(StateEnum state, StatefulContext context) {
                        actualOrder.add(6);
                    }
                });

        StatefulContext ctx = new StatefulContext();
        flow.trace().start(ctx);
        ctx.awaitTermination();

        int i = 0;
        synchronized (actualOrder) {
            for (Integer order : actualOrder) {
                i++;
                if (order != i) {
                    throw new RuntimeException("Events called not in order expected: " + i + " actual: " + order);
                }
            }
        }
    }


    @Test
    public void testEventsOrderTransitionCollection() throws LogicViolationError {

        RegularTransition t1 = RegularTransition.createSingleTransition(event_1, STATE_1, false);
        t1.setStateFrom(START);

        RegularTransition t2 = RegularTransition.createSingleTransition(event_2, STATE_2, true);
        t2.setStateFrom(STATE_1);

        RegularTransition t3 = RegularTransition.createSingleTransition(event_3, STATE_3, false);
        t3.setStateFrom(STATE_1);

        RegularTransition t4 = RegularTransition.createSingleTransition(event_4, STATE_1, false);
        t4.setStateFrom(STATE_3);

        RegularTransition t5 = RegularTransition.createSingleTransition(event_5, STATE_4, true);
        t5.setStateFrom(STATE_3);

        List<Transition> transitions = Lists.<Transition>newArrayList(
                t1, t2, t3, t4, t5
        );

        EasyFlow<StatefulContext> flow = fromTransitions(START, transitions, false);

        final List<Integer> actualOrder = new ArrayList<Integer>();

        flow
                .whenEnter(START, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        actualOrder.add(1);
                        context.trigger(event_1);
                    }
                })

                .whenLeave(START, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) {
                        actualOrder.add(2);
                    }
                })

                .whenEnter(STATE_1, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        actualOrder.add(3);
                        context.trigger(event_2);
                    }
                })

                .whenLeave(STATE_1, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) {
                        actualOrder.add(4);
                    }
                })

                .whenEnter(STATE_2, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) {
                        actualOrder.add(5);
                    }
                })

                .whenLeave(STATE_2, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) {
                        throw new RuntimeException("It never leaves the final state");
                    }
                })

                .whenFinalState(new StateHandler<StatefulContext>() {
                    @Override
                    public void call(StateEnum state, StatefulContext context) {
                        actualOrder.add(6);
                    }
                });

        StatefulContext ctx = new StatefulContext();
        flow.trace().start(ctx);
        ctx.awaitTermination();

        int i = 0;
        for (Integer order : actualOrder) {
            i++;
            if (order != i) {
                throw new RuntimeException("Events called not in order expected: " + i + " actual: " + order);
            }
        }

        Transition.Repository.consume();
    }

    @Test
    public void testEventReuse() {
        EasyFlow<StatefulContext> flow =

                from(START).transit(
                        on(event_1).to(STATE_1).transit(
                                on(event_3).to(START)
                        ),
                        on(event_2).to(STATE_2).transit(
                                on(event_2).finish(STATE_3),
                                on(event_1).finish(STATE_4)
                        )
                );

        flow
                .whenEnter(START, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        context.trigger(event_2);
                    }
                })
                .whenEnter(STATE_2, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        context.trigger(event_2);
                    }
                });

        StatefulContext ctx = new StatefulContext();
        flow.trace().start(ctx);
        ctx.awaitTermination();

        assertEquals("Final state", STATE_3, ctx.getStateValue());
    }

    @Test
    public void testSyncExecutor() {
        EasyFlow<StatefulContext> flow =

                from(START).transit(
                        on(event_1).to(STATE_1).transit(
                                on(event_3).to(START)
                        ),
                        on(event_2).to(STATE_2).transit(
                                on(event_2).finish(STATE_3),
                                on(event_1).finish(STATE_4)
                        )
                );

        flow
                .whenEnter(START, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        context.trigger(event_2);
                    }
                })
                .whenEnter(STATE_2, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        context.trigger(event_2);
                    }
                });


        StatefulContext ctx = new StatefulContext();
        flow
                .executor(new SyncExecutor())
                .trace()
                .start(ctx);

        assertEquals("Final state", STATE_3, ctx.getStateValue());
    }

    @Test
    public void testGlobalHandlers() {
        EasyFlow<StatefulContext> flow =

                from(START).transit(
                        on(event_1).to(STATE_1).transit(
                                on(event_3).to(START)
                        ),
                        on(event_2).to(STATE_2).transit(
                                on(event_2).finish(STATE_3),
                                on(event_1).finish(STATE_4)
                        )
                );

        final boolean[] whenEnterCalled = {false};

        flow.whenEnter(new StateHandler<StatefulContext>() {
            @Override
            public void call(StateEnum state, StatefulContext context) throws Exception {
                whenEnterCalled[0] = true;
            }
        });

        final boolean[] whenLeaveCalled = {false};

        flow.whenLeave(new StateHandler<StatefulContext>() {
            @Override
            public void call(StateEnum state, StatefulContext context) throws Exception {
                whenLeaveCalled[0] = true;
            }
        });

        final boolean[] whenEventCalled = {false};

        flow.whenEvent(new EventHandler<StatefulContext>() {
            @Override
            public void call(EventEnum event, StateEnum from, StateEnum to, StatefulContext context) throws Exception {
                whenEventCalled[0] = true;
            }
        });

        StatefulContext ctx = new StatefulContext();
        flow
                .executor(new SyncExecutor())
                .trace()
                .start(ctx);

        try {
            flow.trigger(event_1, ctx);
        } catch (LogicViolationError logicViolationError) {
            logicViolationError.printStackTrace();
        }

        assertTrue(whenEnterCalled[0]);
        assertTrue(whenLeaveCalled[0]);
        assertTrue(whenEventCalled[0]);

    }
}

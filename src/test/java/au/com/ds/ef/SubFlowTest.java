package au.com.ds.ef;

import au.com.ds.ef.err.DefinitionError;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static au.com.ds.ef.SubFlowTest.Events.a;
import static au.com.ds.ef.SubFlowTest.Events.b;
import static au.com.ds.ef.SubFlowTest.Events.beReported;
import static au.com.ds.ef.SubFlowTest.Events.bpInternalError;
import static au.com.ds.ef.SubFlowTest.Events.c;
import static au.com.ds.ef.SubFlowTest.Events.e;
import static au.com.ds.ef.SubFlowTest.Events.err;
import static au.com.ds.ef.SubFlowTest.Events.err2;
import static au.com.ds.ef.SubFlowTest.Events.f;
import static au.com.ds.ef.SubFlowTest.Events.s1;
import static au.com.ds.ef.SubFlowTest.Events.s11;
import static au.com.ds.ef.SubFlowTest.Events.s2;
import static au.com.ds.ef.SubFlowTest.Events.s22;
import static au.com.ds.ef.SubFlowTest.Events.sf;
import static au.com.ds.ef.SubFlowTest.States.A;
import static au.com.ds.ef.SubFlowTest.States.B;
import static au.com.ds.ef.SubFlowTest.States.B_ERR;
import static au.com.ds.ef.SubFlowTest.States.C;
import static au.com.ds.ef.SubFlowTest.States.C2;
import static au.com.ds.ef.SubFlowTest.States.C3;
import static au.com.ds.ef.SubFlowTest.States.E;
import static au.com.ds.ef.SubFlowTest.States.E2;
import static au.com.ds.ef.SubFlowTest.States.E3;
import static au.com.ds.ef.SubFlowTest.States.END_1;
import static au.com.ds.ef.SubFlowTest.States.END_2;
import static au.com.ds.ef.SubFlowTest.States.END_ERR1;
import static au.com.ds.ef.SubFlowTest.States.END_ERR2;
import static au.com.ds.ef.SubFlowTest.States.F;
import static au.com.ds.ef.SubFlowTest.States.I;
import static au.com.ds.ef.SubFlowTest.States.J;
import static au.com.ds.ef.SubFlowTest.States.RBE;
import static au.com.ds.ef.SubFlowTest.States.SF;
import static au.com.ds.ef.SubFlowTest.States.SF2;
import static au.com.ds.ef.SubFlowTest.States.SF3;
import static au.com.ds.ef.SubFlowTest.States.SFN;
import static au.com.ds.ef.SubFlowTest.States.START;
import static au.com.ds.ef.ToHolder.emit;
import static au.com.ds.ef.ToHolder.on;

public class SubFlowTest {

    public enum States implements StateEnum {
        START, A, B, C, C2, C3, SF, SF2, SF3, SFN, D, E, E2, E3, F, I, J, END_1, END_2, END_ERR1, END_ERR2, RBE, B_ERR
    }

    public enum Events implements EventEnum {
        a, s11, s22, sf, sf2, b, c, s1, s2, e, err, err2, bpInternalError, beReported, f
    }

    @After
    public void clean() {
        ToHolder.resetDefaultTransitions();
        Transition.Repository.consume();
    }

    @Test
    public void shouldConnectEmittedWithDefaults() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        IncompleteTransition SUBFLOW = IncompleteTransition.from(SF, withDefaults).transit(
                on(c).to(C).transit(
                        emit(s1),
                        on(e).to(E).transit(
                                emit(s2)
                        )
                )
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW).transit(
                                on(s1).to(I).transit(
                                        on(e).finish(END_1)
                                ),
                                on(s2).to(J).transit(
                                        on(e).finish(END_2)
                                )
                        ),
                        on(b).finish(END_1)
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(A))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == sf && t.getStateTo() == SF;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == I;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(E))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s2 && t.getStateTo() == J;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == err && t.getStateTo() == END_ERR1;
                    }
                }));
    }

    @Test
    public void shouldAcceptMultipleEvents() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        IncompleteTransition SUBFLOW = IncompleteTransition.from(SF, withDefaults).transit(
                on(c).to(C).transit(
                        emit(s1),
                        on(e).to(E).transit(
                                emit(s2)
                        )
                ),
                emit(s11)
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a, bpInternalError).to(A).transit(
                        on(sf).subFlow(SUBFLOW).transit(
                                on(s1, s2).to(I).transit(
                                        on(e).finish(END_1)
                                ),
                                on(s11).finish(J)
                        ),
                        on(b).finish(END_1)
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(START))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == bpInternalError && t.getStateTo() == A;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(START))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == a && t.getStateTo() == A;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(A))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == sf && t.getStateTo() == SF;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == I;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(E))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s2 && t.getStateTo() == I;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == err && t.getStateTo() == END_ERR1;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(SF))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s11 && t.getStateTo() == J;
                    }
                }));

        Assert.assertTrue(flow.getAvailableTransitions(C).size() == 5);
        Assert.assertTrue(flow.getAvailableTransitions(E).size() == 4);
        Assert.assertTrue(flow.getAvailableTransitions(SF).size() == 5);
        Assert.assertTrue(flow.getAvailableTransitions(START).size() == 4);
    }

    @Test
    public void shouldOverrideDefaultsInIncompleteTransition() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1)
        );

        IncompleteTransition SUBFLOW = IncompleteTransition.from(SF, withDefaults).transit(
                on(c).to(C).transit(
                        emit(s1)
                ),
                on(err).finish(END_1)
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW).transit(
                                on(s1).to(I).transit(
                                        on(e).finish(END_1)
                                )
                        ),
                        on(b).finish(END_1),
                        on(err).finish(END_2)
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(A))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == sf && t.getStateTo() == SF;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(A))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == b && t.getStateTo() == END_1;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(A))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == err && t.getStateTo() == END_2;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(SF))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == c && t.getStateTo() == C;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(SF))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == err && t.getStateTo() == END_1;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == I;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == err && t.getStateTo() == END_ERR1;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(I))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == e && t.getStateTo() == END_1;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(I))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == err && t.getStateTo() == END_ERR1;
                    }
                }));


        Assert.assertTrue(flow.getAvailableTransitions(A).size() == 3);
        Assert.assertTrue(flow.getAvailableTransitions(SF).size() == 2);
        Assert.assertTrue(flow.getAvailableTransitions(C).size() == 2);
        Assert.assertTrue(flow.getAvailableTransitions(I).size() == 2);
    }

    @Test
    public void shouldConnectEmittedWithDefaultsButNoDefInSub() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        IncompleteTransition SUBFLOW_NO_DEF = IncompleteTransition.from(SFN).transit(
                on(c).to(C3).transit(
                        emit(s1),
                        on(e).to(E3).transit(
                                emit(s2)
                        )
                )
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW_NO_DEF).transit(
                                on(s1).to(I).transit(
                                        on(e).finish(END_1)
                                ),
                                on(s2).to(J).transit(
                                        on(e).finish(END_2)
                                )
                        ),
                        on(b).finish(END_1)
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(A))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == sf && t.getStateTo() == SFN;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C3))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == I;
                    }
                }));
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(E3))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s2 && t.getStateTo() == J;
                    }
                }));

        Assert.assertFalse(FluentIterable.from(flow.getAvailableTransitions(C3))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == err || t.getEvent() == err2;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(J))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == err || t.getEvent() == err2;
                    }
                }));
    }

    @Test
    public void shouldConnectEmittedWhenInChainInvocation() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        IncompleteTransition SUBFLOW = IncompleteTransition.from(SF, withDefaults).transit(
                on(c).to(C).transit(
                        emit(s1),
                        on(e).to(E).transit(
                                emit(s2)
                        )
                )
        );

        IncompleteTransition SUBFLOW2 = IncompleteTransition.from(SF2, withDefaults).transit(
                on(c).to(C2).transit(
                        emit(s11),
                        on(e).to(E2).transit(
                                emit(s22)
                        )
                )
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW).transit(
                                on(s1).subFlow(SUBFLOW2).transit(
                                        on(s11).finish(END_1),
                                        on(s22).finish(END_2)
                                ),
                                on(s2).finish(END_1)
                        )
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == SF2;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C2))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s11 && t.getStateTo() == END_1;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(E2))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s22 && t.getStateTo() == END_2;
                    }
                }));
    }

    @Test
    public void shouldConnectSubFlowWhenAtTheEnd() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        IncompleteTransition SUBFLOW = IncompleteTransition.from(SF, withDefaults).transit(
                on(c).to(C).transit(
                        emit(s1),
                        on(e).to(E).transit(
                                emit(s2)
                        )
                )
        );

        IncompleteTransition SUBFLOWF = IncompleteTransition.from(SF3, withDefaults).transit(
                on(f).to(F).transit(
                        on(e).finish(END_1)
                )
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW).transit(
                                on(s1).subFlow(SUBFLOWF),
                                on(s2).finish(END_1)
                        )
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(A))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == sf && t.getStateTo() == SF;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == SF3;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(F))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == e && t.getStateTo() == END_1;
                    }
                }));
    }

    @Test(expected = DefinitionError.class)
    public void shouldConnectSubFlowWhenAtTheEndFailWhenReactionOnEventForgotten() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        IncompleteTransition SUBFLOW = IncompleteTransition.from(SF, withDefaults).transit(
                on(c).to(C).transit(
                        emit(s1),
                        on(e).to(E).transit(
                                emit(s2)
                        )
                )
        );

        IncompleteTransition SUBFLOWF = IncompleteTransition.from(SF3, withDefaults).transit(
                on(f).to(F).transit(
                        on(e).finish(END_1)
                )
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW).transit(
                                on(s1).subFlow(SUBFLOWF)
                        )
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert
        Assert.assertFalse(true);
    }

    @Test
    public void shouldWorkWithBackTo() {

        //arrange
        IncompleteTransition SUBFLOWF = IncompleteTransition.from(SF3).transit(
                on(f).to(F).transit(
                        on(e).finish(END_1)
                )
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOWF),
                        on(b).to(B).transit(
                                on(c).backTo(SF3)
                        )
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert
        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(B))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == c && t.getStateTo() == SF3;
                    }
                }));
    }

    @Test
    public void shouldConnectSubFlowWithNonUniqueEventsWhenProvidedAsFunction() {

        //arrange
        final List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        Supplier<IncompleteTransition> SUBFLOW = new Supplier<IncompleteTransition>() {
            @Override
            public IncompleteTransition get() {
                return IncompleteTransition.from(SF, withDefaults).transit(
                        on(c).to(C).transit(
                                emit(s1),
                                on(e).to(E).transit(
                                        emit(s2)
                                )
                        )
                );
            }
        };

        Supplier<IncompleteTransition> SUBFLOW2 = new Supplier<IncompleteTransition>() {
            @Override
            public IncompleteTransition get() {
                return IncompleteTransition.from(SF2, withDefaults).transit(
                        on(c).to(C2).transit(
                                emit(s1),
                                on(e).to(E2).transit(
                                        emit(s2)
                                )
                        )
                );
            }
        };

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW).transit(
                                on(s1).subFlow(SUBFLOW2).transit(
                                        on(s1).finish(END_1),
                                        on(s2).finish(END_2)
                                ),
                                on(s2).finish(END_1)
                        )
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == SF2;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(E))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s2 && t.getStateTo() == END_1;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(C2))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == END_1;
                    }
                }));

        Assert.assertTrue(FluentIterable.from(flow.getAvailableTransitions(E2))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s2 && t.getStateTo() == END_2;
                    }
                }));
    }


    @Test
    public void supplierSubFlowIsNotInvokedAtTheEnd() {

        //arrange
        final List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        Supplier<IncompleteTransition> SUBFLOW = new Supplier<IncompleteTransition>() {
            @Override
            public IncompleteTransition get() {
                return IncompleteTransition.from(SF, withDefaults).transit(
                        on(c).to(C).transit(
                                on(s1).finish(SF)
                        )
                );
            }
        };

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW)
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert

        Assert.assertFalse(FluentIterable.from(flow.getAvailableTransitions(C))
                .anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == s1 && t.getStateTo() == SF;
                    }
                }));
    }
}

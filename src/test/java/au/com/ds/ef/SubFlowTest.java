package au.com.ds.ef;

import au.com.ds.ef.err.DefinitionError;
import org.junit.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static au.com.ds.ef.SubFlowTest.Events.*;
import static au.com.ds.ef.SubFlowTest.States.*;
import static au.com.ds.ef.ToHolder.emit;
import static au.com.ds.ef.ToHolder.on;

public class SubFlowTest {

    public enum States implements StateEnum {
        START, A, B, C, C2, C3, SF, SF2, SF3, SFN, D, E, E2, E3, F, I, J, END_1, END_2, END_ERR1, END_ERR2, RBE, B_ERR
    }

    public enum Events implements EventEnum {
        a, s11,s22,sf, sf2, b, c, s1, s2, e, err, err2,bpInternalError, beReported, f
    }

    @After
    public void clean(){
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
        Assert.assertTrue(flow.getAvailableTransitions(A).stream()
                .filter(t->t.getEvent()==sf && t.getStateTo()==SF).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==I).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(E).stream()
                .filter(t->t.getEvent()==s2 && t.getStateTo()==J).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==err && t.getStateTo()==END_ERR1).findAny().isPresent());
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
                on(a,bpInternalError).to(A).transit(
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
        Assert.assertTrue(flow.getAvailableTransitions(START).stream()
                .filter(t->t.getEvent()==bpInternalError && t.getStateTo()==A).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(START).stream()
                .filter(t->t.getEvent()==a && t.getStateTo()==A).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(A).stream()
                .filter(t->t.getEvent()==sf && t.getStateTo()==SF).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==I).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(E).stream()
                .filter(t->t.getEvent()==s2 && t.getStateTo()==I).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==err && t.getStateTo()==END_ERR1).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(SF).stream()
                .filter(t->t.getEvent()==s11 && t.getStateTo()==J).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(C).stream().count()==5);
        Assert.assertTrue(flow.getAvailableTransitions(E).stream().count()==4);
        Assert.assertTrue(flow.getAvailableTransitions(SF).stream().count()==5);
        Assert.assertTrue(flow.getAvailableTransitions(START).stream().count()==4);
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
        Assert.assertTrue(flow.getAvailableTransitions(A).stream()
                .filter(t->t.getEvent()==sf && t.getStateTo()==SF).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(A).stream()
                .filter(t->t.getEvent()==b && t.getStateTo()==END_1).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(A).stream()
                .filter(t->t.getEvent()==err && t.getStateTo()==END_2).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(SF).stream()
                .filter(t->t.getEvent()==c && t.getStateTo()==C).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(SF).stream()
                .filter(t->t.getEvent()==err && t.getStateTo()==END_1).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==I).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==err && t.getStateTo()==END_ERR1).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(I).stream()
                .filter(t->t.getEvent()==e && t.getStateTo()==END_1).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(I).stream()
                .filter(t->t.getEvent()==err && t.getStateTo()==END_ERR1).findAny().isPresent());


        Assert.assertTrue(flow.getAvailableTransitions(A).size()==3);
        Assert.assertTrue(flow.getAvailableTransitions(SF).size()==2);
        Assert.assertTrue(flow.getAvailableTransitions(C).size()==2);
        Assert.assertTrue(flow.getAvailableTransitions(I).size()==2);
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
        Assert.assertTrue(flow.getAvailableTransitions(A).stream()
                .filter(t->t.getEvent()==sf && t.getStateTo()==SFN).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(C3).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==I).findAny().isPresent());
        Assert.assertTrue(flow.getAvailableTransitions(E3).stream()
                .filter(t->t.getEvent()==s2 && t.getStateTo()==J).findAny().isPresent());

        Assert.assertFalse(flow.getAvailableTransitions(C3).stream()
                .filter(t->t.getEvent()==err || t.getEvent()==err2).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(J).stream()
                .filter(t->t.getEvent()==err || t.getEvent()==err2).findAny().isPresent());
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
        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==SF2).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(C2).stream()
                .filter(t->t.getEvent()==s11 && t.getStateTo()==END_1).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(E2).stream()
                .filter(t->t.getEvent()==s22 && t.getStateTo()==END_2).findAny().isPresent());
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
        Assert.assertTrue(flow.getAvailableTransitions(A).stream()
                .filter(t->t.getEvent()==sf && t.getStateTo()==SF).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==SF3).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(F).stream()
                .filter(t->t.getEvent()==e && t.getStateTo()==END_1).findAny().isPresent());
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
    public void shouldWorkWithBackTo(){

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
        Assert.assertTrue(flow.getAvailableTransitions(B).stream()
                .filter(t->t.getEvent()==c && t.getStateTo()==SF3).findAny().isPresent());
    }

    @Test
    public void shouldConnectSubFlowWithNonUniqueEventsWhenProvidedAsFunction() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        Supplier<IncompleteTransition> SUBFLOW = ()-> IncompleteTransition.from(SF, withDefaults).transit(
                on(c).to(C).transit(
                        emit(s1),
                        on(e).to(E).transit(
                                emit(s2)
                        )
                )
        );

        Supplier<IncompleteTransition> SUBFLOW2 = ()-> IncompleteTransition.from(SF2, withDefaults).transit(
                on(c).to(C2).transit(
                        emit(s1),
                        on(e).to(E2).transit(
                                emit(s2)
                        )
                )
        );

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

        Assert.assertTrue(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==SF2).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(E).stream()
                .filter(t->t.getEvent()==s2 && t.getStateTo()==END_1).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(C2).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==END_1).findAny().isPresent());

        Assert.assertTrue(flow.getAvailableTransitions(E2).stream()
                .filter(t->t.getEvent()==s2 && t.getStateTo()==END_2).findAny().isPresent());
    }


    @Test
    public void supplierSubFlowIsNotInvokedAtTheEnd() {

        //arrange
        List<Transition> withDefaults = Arrays.asList(
                on(err).finish(END_ERR1),
                on(err2).finish(END_ERR2),
                on(bpInternalError).to(RBE).transit(
                        on(beReported).finish(B_ERR)
                )
        );

        Supplier<IncompleteTransition> SUBFLOW = ()-> IncompleteTransition.from(SF, withDefaults).transit(
                on(c).to(C).transit(
                        on(s1).finish(SF)
                )
        );

        Flow<StatefulContext> flow = FlowBuilder.EnterFlowBuilder.from(START, withDefaults).transit(
                on(a).to(A).transit(
                        on(sf).subFlow(SUBFLOW)
                )
        ).executor(new SyncExecutor());

        //act
        flow.start(new StatefulContext());

        //assert

        Assert.assertFalse(flow.getAvailableTransitions(C).stream()
                .filter(t->t.getEvent()==s1 && t.getStateTo()==SF).findAny().isPresent());
    }
}

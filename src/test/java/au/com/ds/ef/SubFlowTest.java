package au.com.ds.ef;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static au.com.ds.ef.FlowBuilder.on;
import static au.com.ds.ef.SubFlowTest.Events.*;
import static au.com.ds.ef.SubFlowTest.States.*;

public class SubFlowTest {

    public enum States implements StateEnum {
        START, A, B, C, SF, D, E, I, J, END_1, END_2, END_ERR1, END_ERR2
    }

    public enum Events implements EventEnum {
        a, sf, b, c, s1, s2, e, err, err2
    }

    public List<Transition> withDefaults = Arrays.asList(
            on(err).finish(END_ERR1),
            on(err2).finish(END_ERR2)
    );

    IncompleteTransition SUBFLOW =
            IncompleteTransition.from(SF, withDefaults).transit(
                    IncompleteTransition.on(c).to(C).transit(
                            FlowBuilder.ToHolder.emit(s1),
                            IncompleteTransition.on(e).to(E).transit(
                                    FlowBuilder.ToHolder.emit(s2)
                            )
                    )
            );

    @Test
    public void shouldConnectEmitedWithDefaults() {

        //arrange
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
        Assert.assertTrue(true);
    }
}

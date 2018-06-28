package au.com.ds.ef;

import au.com.ds.ef.err.DefinitionError;
import org.junit.Test;

import static au.com.ds.ef.FlowBuilder.EasyFlowBuilder.from;
import static au.com.ds.ef.ToHolder.on;
import static au.com.ds.ef.ValidationTest.Events.event_1;
import static au.com.ds.ef.ValidationTest.Events.event_2;
import static au.com.ds.ef.ValidationTest.Events.event_3;
import static au.com.ds.ef.ValidationTest.Events.event_4;
import static au.com.ds.ef.ValidationTest.Events.event_5;
import static au.com.ds.ef.ValidationTest.Events.event_6;
import static au.com.ds.ef.ValidationTest.States.START;
import static au.com.ds.ef.ValidationTest.States.STATE_1;
import static au.com.ds.ef.ValidationTest.States.STATE_2;
import static au.com.ds.ef.ValidationTest.States.STATE_3;
import static au.com.ds.ef.ValidationTest.States.STATE_4;

public class ValidationTest {
    public enum States implements StateEnum {
        START, STATE_1, STATE_2, STATE_3, STATE_4
    }

    public enum Events implements EventEnum {
        event_1, event_2, event_3, event_4, event_5, event_6
    }

    @Test(expected = DefinitionError.class)
    // no transitions defined
    public void testLooseEnd1() {
        from(START).transit();
    }

    @Test(expected = DefinitionError.class)
    // no transitions defined for non-final state
    public void testLooseEnd2() {
        from(START).transit(
                on(event_1).to(STATE_1)
        );
    }

    @Test(expected = DefinitionError.class)
    // no transitions defined for non-final state
    public void testLooseEnd3() {
        from(START).transit(
                on(event_1).to(STATE_1).transit()
        );
    }

    @Test(expected = DefinitionError.class)
    // transition defined for final state
    public void testRedundantEvent() {
        from(START).transit(
                on(event_1).finish(STATE_1).transit(
                        on(event_2).to(STATE_2)
                )
        );
    }

    @Test
    public void testReuseEvent() {
        from(START).transit(
                on(event_1).to(STATE_1).transit(
                        on(event_1).to(STATE_2).transit(
                                on(event_1).finish(STATE_3)
                        )
                )
        );
    }

    @Test(expected = DefinitionError.class)
    public void testAmbiguousEvent() {
        from(START).transit(
                on(event_1).to(STATE_1).transit(
                        on(event_2).finish(STATE_3)
                ),
                on(event_1).to(STATE_2).transit(
                        on(event_3).finish(STATE_3)
                )
        );
    }

    @Test(expected = DefinitionError.class)
    public void testDuplicateEvent() {
        from(START).transit(
                on(event_1).to(STATE_1).transit(
                        on(event_2).finish(STATE_2)
                ),
                on(event_1).to(STATE_1).transit(
                        on(event_3).finish(STATE_3)
                )
        );
    }

    @Test(expected = DefinitionError.class)
    public void testCircularEvent() {
        from(START).transit(
                on(event_1).to(START),
                on(event_2).finish(STATE_2)
        );
    }

    @Test
    public void testValid() {
        from(START).transit(
                on(event_1).to(STATE_1).transit(
                        on(event_3).to(STATE_2),
                        on(event_6).finish(STATE_4)
                ),
                on(event_2).to(STATE_2).transit(
                        on(event_4).to(STATE_3).transit(
                                on(event_5).to(START)
                        )
                )
        );
    }
}

package au.com.ds.ef;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TransitionUtil {

    public static Stream<Transition> composeTransitions(List<Transition> dtList, Transition[] transitions) {

        if (dtList!=null){
            return Stream.concat(
                    Arrays.stream(transitions),
                    dtList.stream().filter(dt -> !isOverridePresent(dt, transitions)));
        }else{
            return Stream.of(transitions);
        }
    }

    private static boolean isOverridePresent(Transition dt, Transition[] transitions) {
        return Arrays.stream(transitions)
                .flatMap(t->Stream.concat(Arrays.asList(t).stream(),t.getDerivedTransitions().stream()))
                .filter(t -> t.getEvent()==dt.getEvent()).findAny().isPresent();
    }
}

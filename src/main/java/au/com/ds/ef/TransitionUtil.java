package au.com.ds.ef;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransitionUtil {

    public static List<Transition> composeTransitions(List<Transition> dtList, final Transition[] transitions) {

        if (dtList != null) {
            ArrayList<Transition> ret = Lists.newArrayList(transitions);
            ret.addAll(FluentIterable.from(dtList)
                    .filter(new Predicate<Transition>() {
                        @Override
                        public boolean apply(Transition dt) {
                            return !isOverridePresent(dt, transitions);
                        }
                    }).toList());
            return ret;
        } else {
            return Lists.newArrayList(transitions);
        }
    }

    private static boolean isOverridePresent(final Transition dt, final Transition[] transitions) {
        return FluentIterable.from(Lists.newArrayList(transitions))
                .transformAndConcat(new Function<Transition, Iterable<Transition>>() {
                    @Override
                    public Iterable<Transition> apply(Transition t) {
                        return FluentIterable.from(Collections.singletonList(t))
                                .append(t.getDerivedTransitions())
                                .toList();
                    }
                }).anyMatch(new Predicate<Transition>() {
                    @Override
                    public boolean apply(Transition t) {
                        return t.getEvent() == dt.getEvent();
                    }
                });
    }
}

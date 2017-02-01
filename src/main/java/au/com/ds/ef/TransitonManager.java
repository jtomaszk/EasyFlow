package au.com.ds.ef;

import java.util.Collection;
import java.util.List;

interface TransitonManager {

    void setTransitions(Collection<Transition> collection, boolean skipValidation);

    void processAllTransitions(boolean skipValidation);

    List<Transition> getAvailableTransitions(StateEnum stateFrom);

    StateEnum getStartState();
}

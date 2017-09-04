package au.com.ds.ef.call;

import au.com.ds.ef.StateEnum;
import au.com.ds.ef.StatefulContext;

public interface StateHandler<C extends StatefulContext> extends Handler {
    void call(StateEnum state, C context) throws Exception;
}

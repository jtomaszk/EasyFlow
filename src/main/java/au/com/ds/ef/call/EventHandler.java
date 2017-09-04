package au.com.ds.ef.call;

import au.com.ds.ef.EventEnum;
import au.com.ds.ef.StateEnum;
import au.com.ds.ef.StatefulContext;

public interface EventHandler<C extends StatefulContext> extends Handler {
    void call(EventEnum event, StateEnum from, StateEnum to, C context) throws Exception;
}

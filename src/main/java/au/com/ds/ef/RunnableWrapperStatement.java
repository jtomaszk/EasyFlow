package au.com.ds.ef;

@FunctionalInterface
public interface RunnableWrapperStatement {
    void wrap(Runnable runnable);
}
package au.com.ds.ef;

public class RunnableWrapper implements Runnable {

    private final RunnableWrapperStatement runnableWrapperStatement;
    private Runnable runnable;

    public RunnableWrapper(RunnableWrapper other) {
        this.runnableWrapperStatement = other.runnableWrapperStatement;
    }

    public RunnableWrapper(RunnableWrapperStatement runnableWrapperStatement) {
        this.runnableWrapperStatement = runnableWrapperStatement;
    }

    @Override
    public void run() {
        runnableWrapperStatement.wrap(runnable);
    }

    public void setRunnableMethod(Runnable runnable) {
        this.runnable = runnable;
    }

    public static class NoWrapping extends RunnableWrapper {
        public NoWrapping() {
            super(Runnable::run);
        }
    }
}
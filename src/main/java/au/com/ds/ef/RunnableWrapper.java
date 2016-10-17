package au.com.ds.ef;

public class RunnableWrapper implements Runnable {

    private final RunnableWrapperStatement runnableWrapperStatement;
    private Runnable runnableMethod;

    public RunnableWrapper(RunnableWrapperStatement runnableWrapperStatement) {
        this.runnableWrapperStatement = runnableWrapperStatement;
    }

    @Override
    public void run() {
        runnableWrapperStatement.wrap(runnableMethod);
    }

    public void setRunnableMethod(Runnable runnableMethod) {
        this.runnableMethod = runnableMethod;
    }

    protected static class NoWrapping extends RunnableWrapper {
        public NoWrapping() {
            super(Runnable::run);
        }
    }
}

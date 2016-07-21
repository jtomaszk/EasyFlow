package au.com.ds.ef.err;

public class LogicViolationError extends RuntimeException {
	private static final long serialVersionUID = 904045792722645067L;

	public LogicViolationError(String message) {
		super(message);
	}
}

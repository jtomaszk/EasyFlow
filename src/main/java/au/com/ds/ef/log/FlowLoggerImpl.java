package au.com.ds.ef.log;

import au.com.ds.ef.Flow;

import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * User: andrey
 * Date: 20/02/2014
 * Time: 10:43 PM
 */
public class FlowLoggerImpl implements FlowLogger {
    private Object logger;
    private Method infoMethod;
    private Method errorMethod;

    public FlowLoggerImpl() {
        try {

            Class<?> factoryClass = Class.forName("org.slf4j.LoggerFactory");
            Class<?> loggerClass = Class.forName("org.slf4j.Logger");
            Method factoryMethod = factoryClass.getDeclaredMethod("getLogger", String.class);
            logger = factoryMethod.invoke(null, Flow.class.getName());
            infoMethod = loggerClass.getDeclaredMethod("info", String.class);
            errorMethod = loggerClass.getDeclaredMethod("error", String.class, Throwable.class);
            info("Using slf4j logging...");
        } catch (Exception e) {
            info("Slf4j is not found on the classpath. Falling back to System.out");
        }
    }

    @Override
    public void info(String message, Object... o) {
        String formattedMessage = format(message, o);
        if (infoMethod != null) {
            try {
                infoMethod.invoke(logger, formattedMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("INFO " + Flow.class.getName() + " " + formattedMessage);
        }
    }

    @Override
    public void error(String message, Throwable e) {
        if (errorMethod != null) {
            try {
                errorMethod.invoke(logger, message, e);
            } catch (Exception e2) {
            }
        } else {
            System.err.println("ERROR " + Flow.class.getName() + " " + message);
            e.printStackTrace();
        }
    }
}

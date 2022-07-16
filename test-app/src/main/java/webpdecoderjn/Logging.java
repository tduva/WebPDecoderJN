
package webpdecoderjn;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class Logging {

    static class TextFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            return String.format("[%1$tF %1$tT/%1$tL %5$s] %2$s%6$s [%3$s/%4$s]\n",
                    new Date(record.getMillis()),
                    formatMessage(record),
                    record.getSourceClassName(),
                    record.getSourceMethodName(),
                    record.getLevel().getName(),
                    getStacktraceForLogging(record.getThrown()));
        }

    }
    
    public static String getStacktraceForLogging(Throwable t) {
        if (t != null) {
            try {
                return "\n:"+getStacktrace(t);
            } catch (Exception ex) {
                return "\n:Error getting stacktrace";
            }
        }
        return "";
    }
    
    public static String getStacktrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static void installSingleLineLog() {
        // Remove default handlers
        LogManager.getLogManager().reset();
        
        ConsoleHandler c = new ConsoleHandler();
        c.setFormatter(new TextFormatter());
        Logger.getLogger("").addHandler(c);
    }
    
}

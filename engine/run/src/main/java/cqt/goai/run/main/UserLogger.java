package cqt.goai.run.main;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * 用户的log
 *
 * @author GOAi
 */
public class UserLogger implements Logger {

    private final Logger log;

    public UserLogger(Logger log) {
        this.log = log;
    }

    @Override
    public String getName() {
        return log.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        log.trace("USER " + msg);
    }

    @Override
    public void trace(String format, Object arg) {
        log.trace("USER " + format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log.trace("USER " + format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        log.trace("USER " + format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log.trace("USER " + msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return log.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        log.trace(marker, "USER " + msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        log.trace(marker, "USER " + format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        log.trace(marker, "USER " + format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        log.trace(marker, "USER " + format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        log.trace(marker, "USER " + msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        log.debug("USER " + msg);
    }

    @Override
    public void debug(String format, Object arg) {
        log.debug("USER " + format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log.debug("USER " + format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        log.debug("USER " + format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log.debug("USER " + msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return log.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        log.debug(marker, "USER " + msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        log.debug(marker, "USER " + format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        log.debug(marker, "USER " + format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        log.debug(marker, "USER " + format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        log.debug(marker, "USER " + msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        log.info("USER " + msg);
    }

    @Override
    public void info(String format, Object arg) {
        log.info("USER " + format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log.info("USER " + format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log.info("USER " + format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log.info("USER " + msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return log.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        log.info(marker, "USER " + msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        log.info(marker, "USER " + format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        log.info(marker, "USER " + format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        log.info(marker, "USER " + format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        log.info(marker, "USER " + msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        log.warn("USER " + msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log.warn("USER " + format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log.warn("USER " + format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log.warn("USER " + format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log.warn("USER " + msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return log.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        log.warn(marker, "USER " + msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        log.warn(marker, "USER " + format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        log.warn(marker, "USER " + format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        log.warn(marker, "USER " + format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        log.warn(marker, "USER " + msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        log.error("USER " + msg);
    }

    @Override
    public void error(String format, Object arg) {
        log.error("USER " + format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log.error("USER " + format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log.error("USER " + format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log.error("USER " + msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return log.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        log.error(marker, "USER " + msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        log.error(marker, "USER " + format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        log.error(marker, "USER " + format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        log.error(marker, "USER " + format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        log.error(marker, "USER " + msg, t);
    }
}

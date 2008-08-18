package com.atlassian.util.concurrent;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The class to which this annotation is applied is immutable. This means that
 * its state cannot be seen to change by callers, which implies that
 * <ul>
 * <li>all public fields are final,
 * <li>all public final reference fields refer to other immutable objects, and
 * <li>constructors and methods do not publish references to any internal state
 * which is potentially mutable by the implementation.
 * </ul>
 * <p>
 * Immutable objects may still have internal mutable state for purposes of
 * performance optimization; some state variables may be lazily computed, so
 * long as they are computed from immutable state and that callers cannot tell
 * the difference.
 * <p>
 * Immutable objects are inherently thread-safe; they may be passed between
 * threads or published without synchronization.
 * 
 * @see http
 * ://www.javaconcurrencyinpractice.com/annotations/doc/net/jcip/annotations
 * /Immutable.html
 */
@Documented
@Target(value = TYPE)
@Retention(value = RUNTIME)
public @interface Immutable {}

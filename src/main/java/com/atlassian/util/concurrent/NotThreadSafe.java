package com.atlassian.util.concurrent;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The class to which this annotation is applied is not thread-safe. This
 * annotation primarily exists for clarifying the non-thread-safety of a class
 * that might otherwise be assumed to be thread-safe, despite the fact that it
 * is a bad idea to assume a class is thread-safe without good reason.
 * 
 * @see http
 * ://www.javaconcurrencyinpractice.com/annotations/doc/net/jcip/annotations
 * /NotThreadSafe.html
 */
@Documented
@Target(value = TYPE)
@Retention(value = RUNTIME)
public @interface NotThreadSafe {

}

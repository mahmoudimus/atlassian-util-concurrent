# atlassian-util-concurrent

## General purpose concurrency utilities

This project contains utility classes that are used by various products and projects inside
Atlassian and may have some utility to the world at large. These are designed to help make
it easier to write concurrent code correctly, and generally encapsulate correct usage inside
the utility classes.

Included are lazy references, copy-on-write maps, latches, promises, some AtomicX extensions
and an asynchronous completion service.

There is more documentation on the [wiki](https://bitbucket.org/atlassian/atlassian-util-concurrent/wiki/Home).

## Issue Tracking

Issues are tracked [here](https://bitbucket.org/atlassian/atlassian-util-concurrent/issues?status=new&status=open)


## Getting atlassian-util-concurrent

Add atlassian-util-concurrent as a dependency to your pom.xml:

    <dependencies>
        ...
        <dependency>
            <groupId>io.atlassian.util.concurrent</groupId>
            <artifactId>atlassian-util-concurrent</artifactId>
            <version>4.0.1</version>
        </dependency>
        ...
    </dependencies>

For Gradle add atlassian-util-concurrent as a dependency to your `dependencies` section:

    compile 'io.atlassian.util.concurrent:atlassian-util-concurrent:4.0.1'

## Contributors

Source code should be formatted according to the local style, which is encoded in the formatter
rules in:

    src/etc/eclipse/formatter.xml

Source code should must be accompanied by a tests covering new functionality. Run tests with:

    mvn verify

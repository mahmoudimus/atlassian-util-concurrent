# Change Log
All notable changes to this project will be documented in this file.

This file is formatted according to [change log](http://keepachangelog.com/) i.e. most recent first.

This project attempts to adhere to [Semantic Versioning](http://semver.org/).

## [4.0.1]
### Added
- io.atlassian.util.concurrent.CompletionStages which contains some utility functions for working with java.util.concurrent.CompletionStage

## [4.0.0]
### Warning
- The contract change for AsyncCompleter [here](https://bitbucket.org/atlassian/atlassian-util-concurrent/commits/43c89994b7b4b3d4dabe5caa84c3b7e574a6de85), requires additional
care during upgrade. The compiler will not warn if exception handling for thrown exceptions from AsyncCompleter will no longer function.

### Added
- Functional interface annotation to Effect interface
- javax.annotation.Nonnull annotation in method arguments that aren't expected to be null.
- TryConsumer interface to be used on Promises (replaces Guava's FutureCallback).
- Callback interface to be used to fulfill Promises.
- SettablePromise: A promise that also extends Callback and so it provides fulfillment methods.
- Documentation on Promise cancellations.

### Changed
- Base package changed from com to io to facilitate compatibility.
- code formatting to comply to java 8 style.
- Methods that were overloaded to accept both a Callable and a Supplier have been changed to receive only one or the other, so that lambdas compile unambiguously.
- Replaced Guava's Supplier, Function, etc with Java 8 equivalents.
- Replace Promise based on Guava's asynchronous classes (ListenableFuture, FutureCallback) with a Promise based on Java 8's CompletableFuture.
- LazyReference no longer extends com.google.common.base.Supplier.
- AsyncCompleter now relies on Lazy#supplier to memoize results internally. This changes any exceptions generated when capturing the initial value to
be wrapped by LazyReference.InitializationException instead of directly thrown
- Promises#forFuture now takes an explicit executor parameter.
- ExecutorSubmitter#submit returns Promises that, if cancelled, signals the executor to not run the computation if it hasn't yet started.
  
### Removed
- Dependency on Guava library.
- Beta annotation from Promise and Promises.
- Promise no longer extends com.google.common.util.concurrent.ListenableFuture
- Promise#toRejectedPromise removed due to deprecation.
- ForwardingPromise as it doesn't make sense to have a Promise decorator. Use promise directly.
- Assertions class. Use Objects#requireNonNull to replace Assertions#notNull. Assertions#isTrue has no replacement.
- CopyOnWriteMaps class removed due to deprecation.
- Function class removed and replaced wih Java 8 java.util.function.Function.
- Functions#toGoogleFunction removed as part of removing Guava dependency.
- Functions#identity removed. Use java.util.function.Function#identity.
- LockManager class and its companion LockManagers removed due to deprecation.
- Supplier class removed and replaced with Java 8 java.util.function.Supplier.
- Suppliers#toGoogleSupplier removed as part of removing Guava dependency.
- Suppliers#fromGoogleSupplier removed as part of removing Guava dependency.
- Effect and Effects removed to be replaced by java.util.function.Consumer.

# cap5lut-ratelimits [![Build Status](https://jitpack.io/v/cap5lut/cap5lut-ratelimits.svg)](https://travis-ci.org/cap5lut/cap5lut-ratelimits)
Rate limit system.

**NOTE**: Library is in an early development state and may undergo heavy changes

## Installation:
* [JitPack](https://jitpack.io/#cap5lut/cap5lut-ratelimits/0.1.0)

## Usage:
### RateLimit
`RateLimit` has the method `acquire()`, this method blocks until it acquired a `RateLimitSlot`
If execution shall be limited to 2 times per second, the `RateLimit` can be created like this:
```java
RateLimit rateLimit = new ConcurrentRateLimit(2, 1, TimeUnit.SECONDS);
```

To limit the execution, simply call `acquire()` before execution.
```java
for(int i = 0; i < 10; i++) {
    rateLimit.acquire();
    System.out.printf("%s: %d%n", Instant.now(), i);
}
```

### RateLimited
`RateLimited` is an rate limited instance wrapper. Its `acquire()` method will acquire a slot from the underyling rate
limits and return the instance.

There are two ways to create a `RateLimited` instance:

#### RateLimit.limit(instance)
An `RateLimited` instance can be created by using the `RateLimit`s helper method `limit(instance)`:
```java
AtomicInteger instance = new AtomicInteger(0);
RateLimit rateLimit = new ConcurrentRateLimit(2, 1, TimeUnit.SECONDS);
RateLimited<AtomicInteger> number = rateLimit.limit(instance);
```

#### new RateLimited(instance, ratelimits)
Another way is to use the constructor directly:
```java
AtomicInteger instance = new AtomicInteger(0);
RateLimit rateLimit = new ConcurrentRateLimit(2, 1, TimeUnit.SECONDS);
RateLimited<AtomicInteger> number = new RateLimited<>(instance, rateLimit);
```


#### Example
```java
for (int i = 0; i < 10; i++) {
    System.out.println(number.acquire().incrementAndGet());
}
```

### Complex rate limits
Sometimes rate limits are part of another rate limit (e.g. Discord's rate limits).
The following example shows, how to use multiple rate limits.

In this example, two tasks should be executed 5 times each. Generally only 5 tasks shall be executed within 1 second,
additionally task `taskA` shall be executed only 2 times per second.

Create the tasks: 
```java
Runnable taskAInstance = () -> System.out.printf("%s A%n", Instant.now());
Runnable taskBInstance = () -> System.out.printf("%s B%n", Instant.now());
```

Create the rate limits:
```java
RateLimit globalLimit = new ConcurrentRateLimit(5, 1, TimeUnit.SECONDS);
RateLimit taskALimit = new ConcurrentRateLimit(2, 1, TimeUnit.SECONDS);
```

Create the rate limited instances:
```java
RateLimited<Runnable> taskA = new RateLimited<Runnable>(taskAInstance, globalLimit, taskALimit);
RateLimited<Runnable> taskB = globalLimit.limit(taskBInstance);
```

Schedule the task execution:
```java
ExecutorService executor = Executors.newCachedThreadPool();

for(int i = 0; i < 5; i++) {
    executor.submit(() -> {
        try {
            taskA.acquire().run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
}

for(int i = 0; i < 5; i++) {
    executor.submit(() -> {
        try {
            taskB.acquire().run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
}
```
        
### Canceling an acquired rate limit slot
Sometimes its needed to cancel an acquired slot. By that is meant, that if you acquired a rate limit slot, but
determined it is not needed anymore and should be returned to the rate limit.

`RateLimit.acquire()` returns a `RateLimitSlot` instance. This instance has a method called `cancel()`, which returns
the acquired slot to the rate limit, if its still in the current rate.
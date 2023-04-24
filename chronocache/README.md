# Chronocache

ChronoCache is a middleware predictive query caching system for remote databases. The paper underlying the techniques is available here: https://cs.uwaterloo.ca/~bjglasbe/papers/chronocache.pdf


## How to use

ChronoCache accepts query submissions via a REST interface. To start up ChronoCache's REST interface, use:

```mvn jetty:run -DskipTests```

You should first set configuration parameters in Parameters.java and chronocache.properties.

A (very basic) prototype of a JDBC driver is available [here](http://github.com/bglasber/chronocache_driver).

A copy of the REST-based TPC-E benchmark we used to evaluate Chronocache is available [here](https://github.com/bglasber/dbt5).

## Citation

Brad Glasbergen, Kyle Langendoen, Michael Abebe, Khuzaima Daudjee. ChronoCache: Predictive and Adaptive Mid-Tier Query Result Caching. SIGMOD 2020.

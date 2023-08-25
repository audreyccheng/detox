# DeToX

This repository contains code implementing DeToX, a transactional caching system. DeToX leverages insights on transactional hit rate to improve caching performance for transactional workloads.

[Usenix OSDI2023 paper pdf](https://www.usenix.org/system/files/osdi23-cheng.pdf)

In addition to DeToX, this repository contains:
1. A modified version of [ChronoCache](https://github.com/bglasber/chronocache), a middleware predictive query caching system, that measures transactional hit rate, integrates with Redis, and supports several benchmarks not available for the original system.
2. A modified version of Redis that supports several eviction algorithms, including DeToX's eviction algorithm and LIFE from the [PACMan](https://www.usenix.org/system/files/conference/nsdi12/pacman.pdf) paper.
3. A caching simulator that takes transaction traces as input and outputs hit rates for the offline Belady and Transactional Belady algorithms.

This repository is structured as follows:
- /chronocache - the codebase for ChronoCache
- /oltpbench-chronocache - the benchmarks for ChronoCache
- /redis - the modified version of Redis supporting transactional caching algorithms
- /simulator - the caching simulator for offline policies
- /sys - the transactional caching system
    - /benchmarks - the benchmarks for running DeToX
    - /src - the implementation of the DeToX shim layer

Prerequisites:
- mvn 3.8.5
- build-essential
- Java 17

Note: if running on EC2 or other cloud providers, make sure security groups / firewalls allow all traffic.

## DeToX
DeToX supports a range of benchmarks and systems. Currently, the default caching system is Redis and the default database is Postgres. There is separate loader and runner for every benchmark.

To run DeToX on a benchmark:

1. `cd sys`

2. `mvn clean install`

3. `mvn assembly:single`

4. Configure benchmark client: `____ExpConfig.json`

    - `threads`: number of benchmark threads

    - `req_threads_per_bm_thread`: thread pool size for each benchmark thread’s requests; a single benchmark thread may run multiple requests in parallel using its own thread pool

    - `exp_length`: experiment length in seconds

    - `postgres_hostname`: where postgres is hosted

    - `redis_hostname`: where redis cache is hosted

    - `redis_enabled`: enables redis cache. otherwise, only uses postgres. Should be false while loading!

    - `redis_prefetch`: enables prefetching to redis; requires redis_enabled to be true.

5. To run loader and benchmark client: `java -jar ___.jar ___ExpConfig.json`

Note: to build a different jar, edit `pom.xml` plugin.

## Postgres
DeToX must be run with a database. [Postgres](https://www.postgresql.org/) is one of the supported systems.

To install and run Postgres:

1. `sudo apt install postgresql`

2. `sudo systemctl start postgresql.service`

3. `sudo -u postgres psql`

4. In `psql: CREATE USER admin WITH PASSWORD 'password';`

5. In `psql: CREATE DATABASE benchmark;`

6. Edit `/etc/postgresql/12/main/pg_hba.conf`; change the line under IPv4 local connections to use 0.0.0.0/0 instead of 127.0.0.1/32 (allows connections from Internet clients)

7. `sudo systemctl restart postgresql.service`

## Redis
For caching, DeToX currently integrates with [Redis](https://redis.io/). We add DeToX, GDSF, and LIFE to Redis. The main changes are in `config.c`, `db.c`, `evict.c`, `object.c`, `server.h`, and `t_string.c` in the `src/` directory. We modify the `mget` function to process parallel reads.

To run Redis:

1. `cd redis, make`

2. Configuring Redis: `redis.conf`

    - `maxmemory: cache size`

    - `maxmemory-policy: gdsf, min-fsl, avg-fsl, etc.`

    - `maxmemory-samples: number of samples to choose eviction candidate from`

3. Run with `redis ./src/redis-server redis.conf`

Note: use `rm dump.rdb` to clear the cache.

## ChronoCache
ChronoCache accepts query submissions via a REST interface. To start up ChronoCache's REST interface, use:

1. Set configuration parameters in `Parameters.java` and `chronocache.properties`.

2. Run `mvn jetty:run -DskipTests`

All benchmarks for ChronoCache should be run via the modified version of OLTPBench available in the `/oltpbench-chronocache` folder.

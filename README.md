Weather4S  [![Chat][gitter-badge]][gitter-link]
=========

![architecture](docs/Weather4s-Architecture.png)

---

[![Cats][cats-badge]][cats-link]

### Objectives

While still aiming to have a project that is of practical use, to some extent, **weather4s**'s usefulness for a hypothetical user is not particularly relevant.
The ultimate goal is rather to implement a fully-fledged, professional-grade, and of course functioning, template/PoC
- for exploring FP concepts in Scala
- as well as a base from which to get ideas and tips for future projects.

As additional aim, the project also tries to be quite opinionated, in some way, in the sense that it should be using only libraries that fully embrace the
FP philosophy - being the only exception so far **Lucene**, used anyhow just in a few unit tests to assess the behaviour of the suggester - as on the other hand to never
make use of any library that could add any sort of "magic" to the codebase (no dependency injection then).

### Requirements

- JDK 8+
- scala 2.13+
- sbt 1.3+
- docker 19+

### Build

```shell
$ sbt compile docker:publishLocal && docker system prune -f
```

### Tests

```shell
$ sbt test it:test
```
Every microservice can still be independently tested:
```
$ sbt "project auth" ";test; it:test"
$ sbt "project geo" ";test; it:test"
$ sbt "project loader" ";test; it:test"
```

## Running Weather4s

```shell
$ ./bin/start-w4s-ssl.sh
```
and
```shell
$ ./bin/stop-w4s-ssl.sh
```
to stop the application.

The Geo service can also just use http (at port 8082), instead of https, by starting Weather4s with:
```shell
$ ./bin/start-w4s.sh
```
In that case, it can be stopped running:
```shell
$ ./bin/stop-w4s.sh
```

#### Health checks (e.g. with HTTPie)
```shell
$ http :8081/auth/health
$ http --verify no https://0.0.0.0:8443/geo/health
$ http :8083/loader/health
```

#### Configuration

All Weather4s' configuration properties can be found in one file, **bin/env-w4s**.

#### Running a single microservice

```shell
$ ./bin/run-postgres.sh; sbt "project auth" run     # Auth requires Postgres
```
or
```shell
$ ./bin/run-elastic.sh;  sbt "project geo" run      # Geo requires ElasticSearch
```
or
```shell
$ ./bin/run-elastic.sh;  sbt "project loader" run   # Loader requires ElasticSearch
```
Any running microservice can then be stopped with *Ctrl-C*, while the Postgres and the ElasticSearch
containers respectively with "`docker stop w4sPostgres`" and "`docker stop w4sElastic`".

## REST Endpoints

**TBD**

[cats-badge]: https://typelevel.org/cats/img/cats-badge-tiny.png
[cats-link]: https://typelevel.org/cats/
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/sherpair/weather4s

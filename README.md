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

The project also pretends to be opinionated, in some way, as it should
- use only libraries that fully embrace the FP philosophy, being the only exception so far **Lucene**, used anyhow just in a few unit tests to assess the behaviour of the
  suggester
- and never make use of any library that could add any sort of "magic" to the codebase (no dependency injection, for instance).

### Microservices

- **Loader**. Operates only upon a user request, sent via **Geo**. An architectural choice was to feed ElasticSearch (the engine) with localities only when the user expressly
  asks to make a specific country available for weather queries. When this happens and as long as the country's localities are not already present in the engine, the service
  downloads the related CSV file from [geonames](http://download.geonames.org/export/dump/), transforms it and adds all localities resulting from the process to the engine in
  a "new country" engine index (e.g. `GB` for United Kingdom, `IT` for Italy, ...).

  Only if the process is successful the `countries` engine index gets updated, with the document of the "new country" set as *available*. **Geo** is notified that one
  country is now available only after **Loader** updates the `meta` engine index, which acts as a trigger (ElasticSearch doesn't provide a transaction-like mechanism),
  and in any case only at the next iteration of the `CacheHandler` in **Geo** which, after noticed the `meta` document was updated, makes the country visible to the user
  as a last step.

- **Geo**. Aside from user authentication, handled by **Auth**, **Geo** is the main backend interface for the frontend to which provides the list of available and
  *non-available-yet* countries, as well as the list of suggestions while the user types the name of the locality she is looking the weather info for.

  It is also responsible for the initialization of the engine, where it persists, at the first launch of Weather4s, the list of all countries in the world, marked as
  *not-available-yet*, as well as the **meta** document (in a specific engine index).

- **Auth**. As expected, **Auth** handles all aspects of user management. From registration via email activation to authentication and authorization for all routes of
  Weather4s, to the management of the member's profile, used by **Geo** to show the weather of the landing locality, chosen by the user during the registration, every
  time she logs in.

### Frontend

**TBD** ... in truth, I already have a working prototype, but it's in Typescript/React. I won't use that, then. Plan is to only use FP Scala, accordingly the last step
will be to write the frontend using Scala.js. Still, it should more or less draw on the same ideas, style and functionalities of the former implementation, as shown in
[this screenshot](docs/screenshot.png), with the addition of user signup/signin/profile pages.

### Requirements

- JDK 8+
- scala 2.13+
- sbt 1.3+
- docker 19+

In addition, the script to generate a self-signed certificate

```shell
$ bin/gen-self-signed-certs.sh
```

requires *openssl* and *bash*. Note that it has to be executed at least once before building the docker images.

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

(Note that all scripts in the bin directory leverage some specific *bash*'s features)

```shell
$ ./bin/start-w4s.sh
```
and
```shell
$ ./bin/stop-w4s.sh
```
to stop the application.

All microservices can also just use http (but it's not recommended), instead of https, by starting Weather4s with:
```shell
$ ./bin/start-w4s-http.sh
```
In that case, it can be stopped running:
```shell
$ ./bin/stop-w4s-http.sh
```

#### Configuration

All Weather4s' configuration properties can be found in **env-w4s**, **env-w4s-ssl** and **env-w4s-secrets**
under the **bin/** directory.

While *env-w4s* and *env-w4s-ssl* are already provided and do not need any modification (but they can be of
course customized by need), *env-w4s-secrets* must be created before launching Weather4s. This is an example
of what it should contain:
```shell
#!/bin/sh

export W4S_DB_NAME=w4s
export W4S_DB_USER=w4s
export W4S_DB_SECRET=w4s123456

export W4S_AUTH_SMTP_ADDRESS=smtp.gmail.com                # your SMTP server
export W4S_AUTH_SMTP_PORT=587                              # your SMTP server's port
export W4S_AUTH_SMTP_USER=<your email account>@gmail.com
export W4S_AUTH_SMTP_SECRET=<your email secret>

export W4S_KEY_STORE_SECRET=w4s123456         # used by bin/gen-self-signed-certs.sh
```

#### Running a single microservice

```shell
# Auth requires Postgres and env vars in bin/env-w4s-secrets
$ ./bin/run-postgres.sh; sbt "project auth" run
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

Note that the following links are only valid with the *project-default* configuration... *https* and *8442*, *8443*, *8444* as network ports.

- [**Auth** API](https://0.0.0.0:8442/auth/api)
- [**Geo** API](https://0.0.0.0:8443/geo/api)
- [**Loader** API](https://0.0.0.0:8444/loader/api)

## A few examples (with HTTPie)

- **Sign-up**
```shell
$ echo '{"accountId":"anAccount","firstName":"FN","lastName":"LN","email":"<actual email>@<email server>","geoId":"123456","country":"GB","secret":[97, 80, 97, 115, 115, 119, 111, 114, 100]}' | \
    http --verify no POST https://0.0.0.0:8442/auth/signup
```
> HTTP/1.1 201 Created  
Content-Length: 206  
Content-Type: application/json  
Date: Wed, 27 Nov 2019 14:42:56 GMT  
{  
&nbsp;&nbsp;"accountId": "anAccount",  
&nbsp;&nbsp;"active": false,  
&nbsp;&nbsp;"country": "GB",  
&nbsp;&nbsp;"createdAt": "2019-11-27T14:42:49.099401Z",  
&nbsp;&nbsp;"email": &lt;actual email&gt;@&lt;email server&gt;",  
&nbsp;&nbsp;"firstName": "FN",  
&nbsp;&nbsp;"geoId": "123456",  
&nbsp;&nbsp;"id": 5,  
&nbsp;&nbsp;"lastName": "LN",  
&nbsp;&nbsp;"role": "Member"  
}

**Note that the new member account has to be confirmed before proceed with the next examples!!**

- **Sign-in**
```shell
$ echo '{"accountId":"anAccount","secret":[97, 80, 97, 115, 115, 119, 111, 114, 100]}' | \
    http --verify no POST https://0.0.0.0:8442/auth/signin
```
> HTTP/1.1 204 No Content  
Authorization: Bearer  eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NoZXJwYWlyLmlvL3dlYXRoZXI0cy9hdXRoIiwic3ViIjoiNSIsImF1ZCI6WyJodHRwczovL3NoZXJwYWlyLmlvL3dlYXRoZXI0cy9hdXRoIiwiaHR0cHM6Ly9zaGVycGFpci5pby93ZWF0aGVyNHMvZ2VvIiwiaHR0cHM6Ly9zaGVycGFpci5pby93ZWF0aGVyNHMvbG9hZGVyIl0sImV4cCI6MTU3NDg3NDU0NiwibmJmIjoxNTc0ODY3MzQ2LCJpYXQiOjE1NzQ4NjczNDYsImlkIjo1LCJhY2NvdW50SWQiOiJ1Y2xpbyIsImZpcnN0TmFtZSI6IlVjbGlvIiwibGFzdE5hbWUiOiJEaW5vYmkiLCJnZW9JZCI6IjEyMzQ1NiIsImNvdW50cnkiOiJJVCIsInJvbGUiOiJNZW1iZXIiLCJjcmVhdGVkQXQiOiIyMDE5LTExLTI3VDE0OjQyOjQ5LjA5OTQwMVoifQ.TgrTEP8Mlj8oF2SgIyvenoZ6itpYzpkk7fnqRnir8qy7-IkD06q5yRLESCT38fFTxXhF8gPgLnJ_smfkgDZ-5cEyNyBf6MNWJhtumxvFLt0Bm7iXdlz4IQStFEOKomuhaNXpNoykl0pufgj3Qow9ECtNoKbA8JPwJyyGJOMiF9whNM1mQDCguY8DyLTODnPmjP8zeXVAHfJXVgNHam5akUWn_VcAw38c5TtV58NZlv7fnr_qxbXLItsimr1j-QGcBeuPxCIgE_lu2qLazfN2cuWwBUbWfEDCuE8fhbm2GxxoGAYS_FZsQ2_KNYkMRj2dNVb1CuKtjjlJQmwCZll-dg  
Date: Wed, 27 Nov 2019 15:09:06 GMT

- **List the available countries**
```shell
$ http --verify no https://0.0.0.0:8443/geo/countries/available 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NoZXJwYWlyLmlvL3dlYXRoZXI0cy9hdXRoIiwic3ViIjoiNSIsImF1ZCI6WyJodHRwczovL3NoZXJwYWlyLmlvL3dlYXRoZXI0cy9hdXRoIiwiaHR0cHM6Ly9zaGVycGFpci5pby93ZWF0aGVyNHMvZ2VvIiwiaHR0cHM6Ly9zaGVycGFpci5pby93ZWF0aGVyNHMvbG9hZGVyIl0sImV4cCI6MTU3NDg3NDU0NiwibmJmIjoxNTc0ODY3MzQ2LCJpYXQiOjE1NzQ4NjczNDYsImlkIjo1LCJhY2NvdW50SWQiOiJ1Y2xpbyIsImZpcnN0TmFtZSI6IlVjbGlvIiwibGFzdE5hbWUiOiJEaW5vYmkiLCJnZW9JZCI6IjEyMzQ1NiIsImNvdW50cnkiOiJJVCIsInJvbGUiOiJNZW1iZXIiLCJjcmVhdGVkQXQiOiIyMDE5LTExLTI3VDE0OjQyOjQ5LjA5OTQwMVoifQ.TgrTEP8Mlj8oF2SgIyvenoZ6itpYzpkk7fnqRnir8qy7-IkD06q5yRLESCT38fFTxXhF8gPgLnJ_smfkgDZ-5cEyNyBf6MNWJhtumxvFLt0Bm7iXdlz4IQStFEOKomuhaNXpNoykl0pufgj3Qow9ECtNoKbA8JPwJyyGJOMiF9whNM1mQDCguY8DyLTODnPmjP8zeXVAHfJXVgNHam5akUWn_VcAw38c5TtV58NZlv7fnr_qxbXLItsimr1j-QGcBeuPxCIgE_lu2qLazfN2cuWwBUbWfEDCuE8fhbm2GxxoGAYS_FZsQ2_KNYkMRj2dNVb1CuKtjjlJQmwCZll-dg'
```
> HTTP/1.1 200 OK  
Content-Type: application/json; charset=UTF-8  
Date: Wed, 27 Nov 2019 15:25:24 GMT  
Transfer-Encoding: chunked  
[  
&nbsp;&nbsp;{  
&nbsp;&nbsp;&nbsp;&nbsp;"analyzer": "english",  
&nbsp;&nbsp;&nbsp;&nbsp;"code": "gb",  
&nbsp;&nbsp;&nbsp;&nbsp;"localities": 64210,  
&nbsp;&nbsp;&nbsp;&nbsp;"name": "United Kingdom",  
&nbsp;&nbsp;&nbsp;&nbsp;"updated": 1574346988978  
&nbsp;&nbsp;},  
&nbsp;&nbsp;{  
&nbsp;&nbsp;&nbsp;&nbsp;"analyzer": "stop",  
&nbsp;&nbsp;&nbsp;&nbsp;"code": "zw",  
&nbsp;&nbsp;&nbsp;&nbsp;"localities": 24821,  
&nbsp;&nbsp;&nbsp;&nbsp;"name": "Zimbabwe",  
&nbsp;&nbsp;&nbsp;&nbsp;"updated": 1574523193335  
&nbsp;&nbsp;},  
]

- **Download a "*not-available-yet*" country**
```shell
$ http --verify no PUT https://0.0.0.0:8443/geo/country/luxembourg 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NoZXJwYWlyLmlvL3dlYXRoZXI0cy9hdXRoIiwic3ViIjoiNSIsImF1ZCI6WyJodHRwczovL3NoZXJwYWlyLmlvL3dlYXRoZXI0cy9hdXRoIiwiaHR0cHM6Ly9zaGVycGFpci5pby93ZWF0aGVyNHMvZ2VvIiwiaHR0cHM6Ly9zaGVycGFpci5pby93ZWF0aGVyNHMvbG9hZGVyIl0sImV4cCI6MTU3NDg3NDU0NiwibmJmIjoxNTc0ODY3MzQ2LCJpYXQiOjE1NzQ4NjczNDYsImlkIjo1LCJhY2NvdW50SWQiOiJ1Y2xpbyIsImZpcnN0TmFtZSI6IlVjbGlvIiwibGFzdE5hbWUiOiJEaW5vYmkiLCJnZW9JZCI6IjEyMzQ1NiIsImNvdW50cnkiOiJJVCIsInJvbGUiOiJNZW1iZXIiLCJjcmVhdGVkQXQiOiIyMDE5LTExLTI3VDE0OjQyOjQ5LjA5OTQwMVoifQ.TgrTEP8Mlj8oF2SgIyvenoZ6itpYzpkk7fnqRnir8qy7-IkD06q5yRLESCT38fFTxXhF8gPgLnJ_smfkgDZ-5cEyNyBf6MNWJhtumxvFLt0Bm7iXdlz4IQStFEOKomuhaNXpNoykl0pufgj3Qow9ECtNoKbA8JPwJyyGJOMiF9whNM1mQDCguY8DyLTODnPmjP8zeXVAHfJXVgNHam5akUWn_VcAw38c5TtV58NZlv7fnr_qxbXLItsimr1j-QGcBeuPxCIgE_lu2qLazfN2cuWwBUbWfEDCuE8fhbm2GxxoGAYS_FZsQ2_KNYkMRj2dNVb1CuKtjjlJQmwCZll-dg'
```
> HTTP/1.1 204 No Content  
Date: Wed, 27 Nov 2019 15:33:08 GMT

- **Suggest a locality**
```shell
$ http --verify no https://0.0.0.0:8443/geo/suggest/gb/lond?maxSuggestions=3
```
> HTTP/1.1 200 OK  
Content-Type: application/json; charset=UTF-8  
Date: Wed, 27 Nov 2019 15:38:25 GMT  
Transfer-Encoding: chunked  
[  
&nbsp;&nbsp;{  
&nbsp;&nbsp;&nbsp;&nbsp;"coord": {  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"lat": 51.49227,  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"lon": -0.30864  
&nbsp;&nbsp;&nbsp;&nbsp;},  
&nbsp;&nbsp;&nbsp;&nbsp;"name": {  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"input": "London",  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"weight": 8787892  
&nbsp;&nbsp;&nbsp;&nbsp;},  
&nbsp;&nbsp;&nbsp;&nbsp;"tz": "Europe/London"  
&nbsp;&nbsp;},  
&nbsp;&nbsp;{  
&nbsp;&nbsp;&nbsp;&nbsp;"coord": {  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"lat": 54.99721,  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"lon": -7.30917  
&nbsp;&nbsp;&nbsp;&nbsp;},  
&nbsp;&nbsp;&nbsp;&nbsp;"name": {  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"input": "Londonderry County Borough",  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"weight": 87153  
&nbsp;&nbsp;&nbsp;&nbsp;},  
&nbsp;&nbsp;&nbsp;&nbsp;"tz": "Europe/London"  
&nbsp;&nbsp;},  
&nbsp;&nbsp;{  
&nbsp;&nbsp;&nbsp;&nbsp;"coord": {  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"lat": 52.89855,  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"lon": -1.27136  
&nbsp;&nbsp;&nbsp;&nbsp;},  
&nbsp;&nbsp;&nbsp;&nbsp;"name": {  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"input": "Long Eaton",  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"weight": 47898  
&nbsp;&nbsp;&nbsp;&nbsp;},  
&nbsp;&nbsp;&nbsp;&nbsp;"tz": "Europe/London"  
&nbsp;&nbsp;}  
]

[cats-badge]: https://typelevel.org/cats/img/cats-badge-tiny.png
[cats-link]: https://typelevel.org/cats/
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/sherpair/weather4s

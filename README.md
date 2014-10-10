vertxapitest
============

Test project for building a REST Api server with Vertx and Scala

# Demo

First run the RESR API server:

```
mvn exec:java
```

Note that the first time it will take longer because Vertx-Scala module has to be downloaded.

After the server shows a message that it's listening at 8080, open a console and request the unique user that it's in the server memory rightnow:

```
curl http://localhost:8080/users/mike
```

Now add a user and retrieve it!:

```
curl -H "Content-Type: application/json" -d '{"id":"charles","fname":"Carlos","lname":"Ballesteros Velasco"}' http://localhost:8080/users
curl http://localhost:8080/users/charles
```
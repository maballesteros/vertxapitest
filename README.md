vertxapitest
============

Test project for building a REST Api server with Vertx and Scala

## Demo

First run the REST API server:

```
mvn compile exec:java
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

## Routing made simple

Note how easy is to route REST calls and map them to Scala case classes:

```
    GET[UserIdParams, User]("/users/:userId")(
      (ur, ok) => ok(UserRepo.get(ur.userId))
    )

    POST[EmptyParams, User, User]("/users")(
      (er, user, ok) => {
        UserRepo.put(user)
        ok(user)
    })
```

`GET` and `POST` are parametrizable `VertxRest` object methods that:

 - map URL params and reques body to case classes
 - call your function, with the mapped objects and a "success" (`ok` in the example) function when the async work is done
 - serialize your response object into a JSON REST response
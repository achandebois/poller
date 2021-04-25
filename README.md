# Poller services

The goal is to add service urls with names and to poll them. Services are periodically polled

you can make all these actions on the api

- /GET /api/v1/services/
- /POST /api/v1/services/
- /UPDATE /api/v1/services/{id}
- /DELETE /api/v1/services/{id}

NB: 
- Scroll/Pagination is not implemented and a hardcoded value of 100 is put in the repository
- A timeout has been added on the webclient
- A pattern regexp validator has been added to check URL (maybe use URL from java to check it)

# Building
In intelliJ, on the main project
```
src -> test -> Right click -> Run 'All Tests'"
```

You can also run gradle directly from the command line:
```
./gradlew clean test
```

# Building
In intelliJ, choose
```
New -> New from existing sources -> Import project from external model -> Gradle -> select "use gradle wrapper configuration"
```

You can also run gradle directly from the command line:
```
./gradlew clean run
```

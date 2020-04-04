## Running the application

The application can be run using the following command

> Note: The variables in `.envrc.template.service` enumerate all environment variables needed to run these tests

```bash
$ mvn clean package spring-boot:repackage -f app/
$ java -jar $(find app/target/ -name '*.jar')
```

## Integration Tests

The application integration tests can be run using the following command.

> Note: The variables in `.envrc.template.integtests` enumerate all environment variables needed to run these tests

```bash
$ mvn clean test -f integration-tests/
```

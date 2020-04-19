## Running the application

The application can be run using the following command

> Note: The variables in `.envrc.template.service` enumerate all environment variables needed to run these tests

```bash
$ mvn clean package -f app/ && java -jar $(find app/target/ -name '*.jar')
```

## Integration Tests

The application integration tests can be run using the following command.

> Note: The variables in `.envrc.template.integtests` enumerate all environment variables needed to run these tests

```bash
$ mvn clean test -f integration-tests/
```

## Run the UI locally

```bash
$ npm start --prefix ui
```

> Note: due to CORS, you may want to use a proxy like `nginx` to have the UI and server run behind the same port.
> The following config can be used in your `nginx` config (possibly `/usr/local/etc/nginx/nginx.conf`)

```
    server {
            listen       8000;
            server_name  localhost;
            location / {
                proxy_pass http://localhost:3000;
            }
            location /api/ {
                proxy_pass http://localhost:8080;
            }
    }
```

## Build the UI

```bash
$ npm run build --prefix ui/
```
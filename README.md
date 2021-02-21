![Deploy Java Web App to Azure](https://github.com/nmiodice/audio-transcription-svc/workflows/Deploy%20Java%20Web%20App%20to%20Azure/badge.svg)

# Audio Transcription Service

This application is comprised of a backend server (written in Kotlin, built with Spring), and a front-end webapplication (written in Javascript, built with React). Together, these components enable full-text search over audio content (primarially podcasts or other spoken word audio). Think of it as Google for audio content.

The backend service will periodically poll from a configured (manually seeded data in Cosmos DB) set of media sources to check new media content. When new content is found, the audio is downloaded and run through Azure Speech to Text. The transcripts are then uploaded into an Azure Search Service instance so that the full text content becomes queryable.

The front-end website provides a search interface that enables users to query for audio content using key terms. For each piece of matched audio, the user is able to: click a link to view the source media; play the source media beginning at the exact point that the keywords were found in the speech to text transcript; and read the audio transcript that is relevant for the keyword search.

The application was showcased on the front page of [Hacker News](https://news.ycombinator.com/item?id=23036016) in May 2020.



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

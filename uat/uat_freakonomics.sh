#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "Freakonomics Radio",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/46/episodes?order=newest&pageSize=10",
  "trackListIsSorted": true,
  "image": "https://freakonomics.com/wp-content/themes/freako_2.0/images/logo.png",
  "homepage": "https://freakonomics.com/"
}


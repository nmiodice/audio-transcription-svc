#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "Coronavirus Global Update",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/309060/episodes?order=newest&pageSize=50",
  "trackListIsSorted": true,
  "image": "https://files.hubhopper.co/podcast/309060/coronavirus-global-update.jpg",
  "homepage": "https://www.bbc.co.uk/programmes/w13xtv39/episodes/downloads"
}


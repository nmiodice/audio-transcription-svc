#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "Mountain Bike Radio",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/166203/episodes?order=newest&pageSize=15",
  "trackListIsSorted": true,
  "titleFilter": "Second Spin Cycles",
  "image": "http://assets.libsyn.com/content/69301064/?height=128&height=128",
  "homepage": "http://mountainbikeradio.com/"
}


#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "Mountain Bike Radio",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/166203/episodes?order=newest&pageSize=50",
  "trackListIsSorted": true,
  "titleFilter": "New England Dirt",
  "image": "https://ssl-static.libsyn.com/p/assets/e/8/b/0/e8b09278cbbb201c/New_England_Dirt_Libsyn.jpg",
  "homepage": "http://mountainbikeradio.com/"
}


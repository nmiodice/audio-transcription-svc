#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "Mountain Bike Radio",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/166203/episodes?order=newest&pageSize=15",
  "trackListIsSorted": true,
  "titleFilter": "Front Lines MTB",
  "image": "https://ssl-static.libsyn.com/p/assets/5/3/c/e/53ce7ccbebc3b0e8/LARGE_frontlines_podcast.png",
  "homepage": "http://mountainbikeradio.com/"
}


#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "The Path Podcast",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/166203/episodes?order=newest&pageSize=50",
  "trackListIsSorted": true,
  "titleFilter": "The Path Podcast",
  "image": "https://ssl-static.libsyn.com/p/assets/6/c/5/2/6c52472f6f81fa68/Header.jpg"
}


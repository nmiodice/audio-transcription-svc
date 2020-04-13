#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "Bens' Week in Review",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/166203/episodes?order=newest&pageSize=50",
  "trackListIsSorted": true,
  "titleFilter": "Bens' Week in Review",
  "image": "https://ssl-static.libsyn.com/p/assets/c/f/8/6/cf86c0e12813d8f4/BensWIR3_Follow_Hollow.jpg"
}


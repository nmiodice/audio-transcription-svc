#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "On Point - Week In The News",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/2026/episodes?order=newest&pageSize=40",
  "trackListIsSorted": true,
  "titleFilter": "Week In The News",
  "image": "https://d279m997dpfwgl.cloudfront.net/wp/2016/06/masthead-on-point-2.png",
  "homepage": "https://www.wbur.org/onpoint"
}


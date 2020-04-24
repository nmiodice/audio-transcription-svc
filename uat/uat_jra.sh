#

curl -s -v -X POST "$SERVICE_ENDPOINT/api/v1/source/" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @- << EOF
{
  "name": "Mountain Bike Radio",
  "trackListEndpoint": "https://hubhopper.com/api/podcasts/166203/episodes?order=newest&pageSize=100",
  "trackListIsSorted": true,
  "titleFilter": "Just Riding Along",
  "image": "http://mountainbikeradio.com/wp-content/uploads/2018/12/MBR001_logo-large.jpg",
  "homepage": "http://mountainbikeradio.com/"
}


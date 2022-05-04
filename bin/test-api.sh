#!/bin/sh
set -ex
host=http://localhost:8080

curl --fail -i "$host/api/v1/status"
curl --fail -i "$host/api/firmware_builds"
curl --fail -I "$host/updates-ouya_1_1.json"
curl --fail -i "$host/api/v1/developers/fc215877-e840-4a4f-b07a-b5696ac1b7ff/products"
curl --fail -I "$host/api/v1/details?app=org.blockinger.game"
curl --fail -i "$host/api/v1/details?app=does.not.exist"
curl --fail -i "$host/api/v1/apps/de.eiswuxe.blookid2"
curl --fail -i "$host/api/v1/apps/780688a9-95ee-429a-8755-69a8d0c88fe0/download"
curl --fail -i "$host/api/v1/developers/5a3fbb4d-852b-4af4-becc-324dce6a3b42/products/"
curl --fail -i "$host/api/v1/developers/5a3fbb4d-852b-4af4-becc-324dce6a3b42/products/?only=blookid2_full"
curl --fail -I "$host/api/v1/discover"
curl --fail -I "$host/api/v1/discover/home"
curl --fail -I "$host/api/v1/discover/tutorials"
curl -I "$host/api/v1/gamers"
curl --fail -I "$host/api/v1/gamers/me"
curl --fail -I "$host/api/v1/games/de.eiswuxe.blookid2/purchases"
curl --fail -I "$host/api/v1/games/does.not.exist/purchases"
curl --fail -I "$host/api/v1/queued_downloads"
curl --fail -I "$host/api/v1/search?q=1"

echo "done"

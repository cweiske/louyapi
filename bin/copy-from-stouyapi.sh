#!/bin/sh
# copy all generated static files from stouyapi to here
stouyapidir=/home/cweiske/dev/ouya/stouyapi
targetdir=./src/main/assets/stouyapi-www

#cd to louyapi directory
SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
cd "$SCRIPTPATH/../$targetdir"

rm -r *
cp -a "$stouyapidir/www/agreements" .
cp -a "$stouyapidir/www/api" .
cp -a "$stouyapidir/www/favicon.ico" .
cp -a "$stouyapidir/www/gen-qr" .
cp -a "$stouyapidir/www/game-data-version" .
cp -a "$stouyapidir/www/updates-ouya_1_1.json" .

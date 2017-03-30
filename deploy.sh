#!/bin/bash

set -e
echo "${TRAVIS_PULL_REQUEST}"
echo "${TRAVIS_EVENT_TYPE}"

git clone https://github.com/1haodian/eagle.git eagle
cd eagle
git checkout  travis-ci-test
git status


echo "Git remote..."
git remote add upstream https://github.com/apache/eagle.git
git remote set-url origin "https://${GH_TOKEN}@${GH_REF}"

echo "Set git id..."
git config  user.name "Travis-CI"
git config  user.email "travis@yhd.com"

echo "Fetch..."
git fetch upstream

echo "Rebase..."
git rebase upstream/master 

exit 0

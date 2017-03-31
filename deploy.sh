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
git status
echo "Fetch... end"

echo "Rebase..."
git rebase upstream/master 
git status
echo "Rebase... end"

echo "Pushing with force ..."
git push --force origin  travis-ci-test > /dev/null 2>&1 || exit 1
echo "Pushed deployment successfully"

pwd
git status
git rev-list travis-ci-test --committer=Travis-CI --pretty=format:"%H %s"
touch ${HOME}/yhdhash
touch ${HOME}/apachehash
chmod 777 ${HOME}/yhdhash
chmod 777 ${HOME}/apachehash
 ls -l   ${HOME}
 echo "yyyyy" >> /home/travis/yhdhash
 cat /home/travis/yhdhash
 cat  ${HOME}/yhdhash
"echo 'export YHD_FIRST_COMMIT_HASH=$(git rev-list master --committer=Travis-CI --pretty=format:"%H" |tail -1)' >> ${HOME}/yhdhash"
"echo 'export APACHE_LAST_COMMIT_HASH=$(git rev-list $YHD_FIRST_COMMIT_HASH^1 -1)' >>  ${HOME}/apachehash"
echo "deploying eagle-0.5.0-SNAPSHOT-bin-$APACHE_LAST_COMMIT_HASH.tar.gz to GitHub releases"

exit 0

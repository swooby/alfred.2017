#!/bin/bash
if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  sudo -H easy_install --upgrade requests
  sudo -H easy_install --upgrade google-api-python-client
fi
$(dirname $0)/travis_ci.py

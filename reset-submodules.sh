#!/bin/bash

# If some files in any of the submodules changed during the build, prevent them from
# being committed by resetting them:
for submodule in $(git status -s | grep 'm ' | cut -f2 -d'm'); do
  cd $submodule;

  for datei in $(git status -s | cut -f2 -d'M'); do
    git checkout -- $datei
  done
  cd -
done

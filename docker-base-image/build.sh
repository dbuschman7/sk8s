#! /bin/bash

docker build -t sk8s-java-base:latest    .

docker images | grep sk8s | grep latest
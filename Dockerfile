FROM gradle:4.4.1-jre8-alpine
MAINTAINER Anthony Heng "anthony.heng@mq.edu.au"
ARG DEBIAN_FRONTEND=noninteractive
USER root
RUN apk update && apk add bash
RUN apk add openjdk8

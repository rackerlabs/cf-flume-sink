#!/bin/sh

getent group flume >> /dev/null 2>&1 || groupadd -r flume
getent passwd repose >> /dev/null 2>&1 || useradd -r -g flume -s /sbin/nologin -d /usr/share/flume -c "Flume" flume

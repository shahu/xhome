#!/bin/bash

crtmpserver_home=/usr/local/crtmpserver
crtmpserver_bin=crtmpserver
crtmpserver_conf=crtmpserver.lua
restart_log="/home/logs/crtmpserver/push_streams.log"

crtmpserver_num=`ps -wef| grep ${crtmpserver_bin} | grep ${crtmpserver_conf} | grep -v grep | wc -l`

if test ${crtmpserver_num} -lt 1
then
	cd ${crtmpserver_home}
	./${crtmpserver_bin} ./${crtmpserver_conf}

	timestamp=`date '+%F %T'`
	echo "${timestamp}|WARNING|monitor.sh|restart crtmpserver!" >> ${restart_log} 
fi
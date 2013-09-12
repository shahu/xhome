#!/bin/bash

wsgi_name=(wsgi_flv2ts wsgi_flv2m3u wsgi_flv2manifest wsgi_flv2fragment wsgi_drm)

for name in ${wsgi_name[*]}
do
	ps -ef|grep ${name}|grep -v grep|awk '{print $2}'|xargs kill -9
done

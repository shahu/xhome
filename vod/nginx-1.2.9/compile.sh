#!/bin/bash

make clean

chmod 755 ./configure

./configure --prefix=/usr/local/nginx \
	--without-http_memcached_module \
	--without-http_empty_gif_module \
	--without-http_auth_basic_module \
	--without-http_map_module \
	--without-http_browser_module \
	--without-mail_pop3_module \
	--without-mail_imap_module \
	--without-mail_smtp_module \
	--http-proxy-temp-path=/mnt/resource/ramdisk/proxy_temp \
	--http-fastcgi-temp-path=/mnt/resource/ramdisk/fastcgi_temp \
	--http-uwsgi-temp-path=/mnt/resource/ramdisk/uwsgi_temp \
	--http-client-body-temp-path=/mnt/resource/ramdisk/client_body_temp \
	--http-scgi-temp-path=/mnt/resource/ramdisk/scgi_temp \
	--add-module=./src/nginx-rtmp-module/ \
	--add-module=./src/lua-nginx-module/ \
	--add-module=./src/headers-more-nginx-module/ \
	--add-module=./src/nginx_upstream_hash/ \
	--with-http_perl_module \
	--with-http_ssl_module \
	--with-http_sub_module \
	--with-http_stub_status_module \
	--with-http_xslt_module \
	--with-ipv6 \
	--with-cc-opt='-D_FILE_OFFSET_BITS=64'
	
	#--with-debug --with-cc-opt='-g -pg -O0 -D_FILE_OFFSET_BITS=64'

make -j8

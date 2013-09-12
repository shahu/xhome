#!/bin/env python
# -*- coding: utf-8 -*-

import os, sys, socket, datetime

def HttpServerRun():
	port = 7777
	server_name = "rtmp manage/0.1"
	GMT_FORMAT = '%a, %d %b %Y %H:%M:%S GMT'

	s = socket.socket()
	s.bind(("", port))
	s.listen(32)

	while True:
		c, addr = s.accept()

		request = c.recv(1024)
		request_lines = request.split('\r\n')
		
		if (len(request_lines[0]) > 0):
			request_method = request_lines[0].split(' ')[0]
			request_url = request_lines[0].split(' ')[1]
			request_http_ver = request_lines[0].split(' ')[2].split('/')[1]
		else:
			continue
		
		try:
			if request_method == "GET" and request_url == "/rtmp_channel_update":
				response_status = "200 OK"
				response_content = "Ok, rtmp channels update"
				os.system("/bin/bash /usr/local/crtmpserver/push_streams.sh monitor &")
			else:
				response_status = "500 Internal Error"
				response_content = "500 Internal Error"
		
		finally:
			response = '''HTTP/1.1 %s\r\nServer: %s\r\nDate: %s\r\nContent-Length: %s\r\nContent-Type: text/html\r\n\r\n%s''' % (
					response_status,
					server_name,
					datetime.datetime.utcnow().strftime(GMT_FORMAT),
					len(response_content),
					response_content)
			c.send(response)
			c.close()

if __name__ == "__main__":
	# fork twice to put into daemon mode
	try:
		for i in range(0, 1):
			pid = os.fork()
			if pid > 0:
				sys.exit(0)

	except OSError, e:
		sys.stderr.write("Could not fork: %d (%s)\n" % (e.errno, e.strerror))
		sys.exit(1)

	# detach from parent environment
	os.chdir('/')
	if -1 == os.setsid():
		sys.stderr.write("Daemon create new session failed!")
		sys.exit(1)
	os.umask(0)

	# close stdio
	sys.stdin.close()
	sys.stdout.close()
	#sys.stderr.close()

	HttpServerRun()

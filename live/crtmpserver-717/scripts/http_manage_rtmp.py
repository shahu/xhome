#!/bin/env python
# -*- coding: utf-8 -*-

import os, sys
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from SocketServer import ThreadingMixIn

class WebRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
		if self.path == '/rtmp_channel_update':
			self.send_response(200, "OK")
			self.send_header("Content-type", "text/html")
			message="Ok, rtmp channels update"
			self.send_header("Content-Length", len(message))
			self.end_headers()
			self.wfile.write(message)

			os.system("/bin/bash /usr/local/crtmpserver/push_streams.sh monitor")

		else:	
			self.send_error(404, 'File Not Found: %s' % self.path)

class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
	"""Handle requests in a separate thread."""

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

	server = ThreadedHTTPServer(('', 7777), WebRequestHandler)
	server.serve_forever()

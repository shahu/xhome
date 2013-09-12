#!/bin/env python
# -*- coding: utf-8 -*-

import os, sys, time
import httplib, socket
import logging, logging.handlers, ConfigParser
from Queue import Queue, Empty
from urlparse import urlparse
from threading import Thread, Event

class ParseConfig:
	def __init__(self, conf_file):
		self._conf_file = conf_file;
		try:
			self._config = ConfigParser.ConfigParser();
			self._config.read(self._conf_file);

			self.init_logger();

			self._crtmpserver_list = self._config.items('crtmpserver');
			self._crtmpserver_dict = {};
			for crtmpserver in self._crtmpserver_list:
				addr, server = crtmpserver[0], crtmpserver[1];
				self._crtmpserver_dict[addr] = server;

			self._flv_stream_url_dict = {};
			self.init_flv_streams(self._config.get("channelstream", "channel_ini"));

		except Exception, e:
			print("config:%s|error:%s" % (self._conf_file, e));
			raise e;

	def init_logger(self):
		global logger;

		log_file = self._config.get("pushlog", "log_file");
		log_level = self._config.get("pushlog", "log_level");
		log_datefmt = self._config.get("pushlog", "log_datefmt");
		log_max_size = self._config.get("pushlog", "log_max_size");
		log_back_count = self._config.get("pushlog", "log_back_count");
		
		logger = logging.getLogger();
		handler = logging.handlers.RotatingFileHandler(log_file, maxBytes=log_max_size, backupCount=log_back_count)
		formatter = logging.Formatter('%(asctime)s|%(levelname)s|%(threadName)s|%(lineno)d|%(message)s', log_datefmt);
		handler.setFormatter(formatter);
		
		logger.addHandler(handler);
		logger.setLevel(self.get_logger_level(log_level));

	def init_flv_streams(self, channel_ini):
		# parse flv stream config
		config = ConfigParser.ConfigParser();
		config.read(channel_ini);

		for section in config.sections():
			self._flv_stream_url_dict[section] = config.get(section, "url");

	def get_logger_level(self, level):
		# set root logger level to warning
		LEVELS = { 'notset':logging.WARNING,
					'debug':logging.DEBUG,
					'info':logging.INFO,
					'warning':logging.WARNING,
					'error':logging.ERROR,
					'critical':logging.CRITICAL };

		return LEVELS.get(level.lower());

	def get_crtmp_server_list(self):
		return self._crtmpserver_list;

	def get_crtmp_sock_addr(self, addr):
		crtmpserver_ip_port = self._crtmpserver_dict[addr].split(":");

		return crtmpserver_ip_port[0], int(crtmpserver_ip_port[1]);

	def get_flv_stream_addr(self, guid):
		url_tuple = urlparse(self._flv_stream_url_dict[guid]);
		netloc_list, path, params = url_tuple[1].split(":"), url_tuple[2], url_tuple[4];
		stream_ip, stream_port = netloc_list[0], netloc_list[1]
		if (len(params) > 0):
			stream_uri = path + "?" + params;
		else:
			stream_uri = path;

		return stream_ip, stream_port, stream_uri;

class ThreadTunnel:
	def __init__(self, queue):
		self._event_sock_error = Event();
		self._event_sock_error.clear();
		self._event_sock_renew = Event();
		self._event_sock_renew.clear();
		self._queue = queue;

	def set_push_thead(self, thread):
		self._push_thead = thread;

	def set_pull_thead(self, thread):
		self._pull_thead = thread;

class PullFlvStream(Thread):
	def __init__(self, guid, stream_ip, stream_port, stream_uri, tunnels):
		Thread.__init__(self, name="pull_thread");
		self._guid = guid;
		self._tunnels = tunnels
		self._stream_ip, self._stream_port, self._stream_uri = stream_ip, stream_port, stream_uri;

	def run(self):
		# pull flv stream from http
		while True:
			logger.warning("%s|pull from http://%s:%s%s" % (self._guid, self._stream_ip, self._stream_port, self._stream_uri));
			self.pull_stream();
			time.sleep(1);

	def pull_stream(self):
		try:
			conn = httplib.HTTPConnection(self._stream_ip, self._stream_port);
			conn.request("GET", self._stream_uri);
			conn.sock.settimeout(5);
			response = conn.getresponse();

			if (response.status != 200):
				raise httplib.HTTPException("http status:%d, %s" % (response.status, response.reason));

			# read 13 bytes of flv header from stream and inject streamName tag
			flv_header = response.read(13);

			if (flv_header[0:3] != "FLV"):
				raise Exception("%s|invalid flv header from http://%s:%s%s" % (self._guid, self._stream_ip, self._stream_port, self._stream_uri));

			flv_metadata = self.insert_streamname_tag();

			for tunnel in self._tunnels:
				# record flv header in push thread
				tunnel._push_thead.set_flv_header_metadata(flv_header + flv_metadata);

				# send flv header to queue
				if not tunnel._queue.full():
					tunnel._queue.put(flv_header + flv_metadata);
				else:
					tunnel._event_sock_renew.set();

			while True:
				is_renew = False;
				for tunnel in self._tunnels:
					if (tunnel._event_sock_renew.isSet()):
						is_renew = True;
						tunnel._event_sock_error.clear();

				# reconnect all push and pull threads
				if is_renew:
					for tunnel in self._tunnels:
						tunnel._event_sock_renew.set();

					logger.warning("%s|sock reconnect, renew all threads|http://%s:%s%s" % (self._guid, self._stream_ip, self._stream_port, self._stream_uri));
					raise httplib.HTTPException("flv stream http connection reset!");

				flv_piece = response.read(4096);
				if (len(flv_piece) > 0):
					for tunnel in self._tunnels:
						# reconnect if queue is null
						if tunnel._queue.full():
							tunnel._event_sock_renew.set();
						# push to queue without blocking and exception
						else:
							if not tunnel._event_sock_error.isSet():
								tunnel._queue.put(flv_piece, 0);
				else:
					raise httplib.HTTPException("http status:%d, %s" % (response.status, response.reason));
				
				# yield to push thread
				time.sleep(0);

		except Exception, e:
			# close http flv stream connection
			conn.close();
			# clear queue data in which without flv header
			for tunnel in self._tunnels:
				logger.warning("%s|queue size:%d|http://%s:%s%s|%s" % (self._guid, tunnel._queue.qsize(), self._stream_ip, self._stream_port, self._stream_uri, e));
				while not tunnel._queue.empty():
					try:
						tunnel._queue.get_nowait();
					except Exception, e:
						logger.warning("%s|clear empty queue|%s" % (self._guid, e));

	def insert_streamname_tag(self):
		meta_head = [ 0x12, 0x00, 0x00, 0x44, 0x00, 0x00, 0x00, 0x00,
					 0x00, 0x00, 0x00, 0x02, 0x00, 0x0A, 0x6F, 0x6E,
					 0x4D, 0x65, 0x74, 0x61, 0x44, 0x61, 0x74, 0x61,
					 0x08, 0x00, 0x00, 0x00, 0x01, 0x00, 0x0A, 0x73,
					 0x74, 0x72, 0x65, 0x61, 0x6D, 0x4E, 0x61, 0x6D,
					 0x65, 0x02, 0x00, 0x20 ];

		meta_tail = [ 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x4F ];

		meta_tag = '';
		for c in meta_head:
			meta_tag += chr(c);
		for c in list(self._guid):
			meta_tag += c;
		for c in meta_tail:
			meta_tag += chr(c);

		return meta_tag;

class PushFlvStream(Thread):
	def __init__(self, guid, crtmpserver_ip, crtmpserver_port, addr, tunnel):
		Thread.__init__(self, name = addr);
		self._guid = guid;
		self._crtmpserver_ip = crtmpserver_ip;
		self._crtmpserver_port = crtmpserver_port;
		self._tunnel = tunnel;
		
	def set_flv_header_metadata(self, flv_header_metadata):
		self._flv_header_metadata = flv_header_metadata;
	
	def run(self):
		# push flv stream to crtmpserver
		while True:
			logger.warning("%s|pushing to tcp://%s:%s" % (self._guid, self._crtmpserver_ip, self._crtmpserver_port));
			self.push_stream();
			time.sleep(1);

	def push_stream(self):
		try:
			sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
			sock.connect((self._crtmpserver_ip, self._crtmpserver_port));
			self._tunnel._event_sock_renew.clear();

			# push thread sock error, send flv header only
			if (self._tunnel._event_sock_error.isSet()):
				logger.warning("%s|reconnect, send flv header only|tcp://%s:%s" % (self._guid, self._crtmpserver_ip, self._crtmpserver_port));

				sock_sent_len = sock.send(self._flv_header_metadata);
				# test reconnect to crtmpserver success
				if (sock_sent_len == len(self._flv_header_metadata)):
					self._tunnel._event_sock_renew.set();
					sock.close();
					logger.warning("%s|reconnect to crtmpserver success|tcp://%s:%s" % (self._guid, self._crtmpserver_ip, self._crtmpserver_port));
			else:
				while True:
					# break current connection, recycle push thread
					if self._tunnel._event_sock_renew.isSet():
						sock.close();
						break;
					try:
						# if no data available, block 1s
						flv_piece = self._tunnel._queue.get(True, 1);
						if (len(flv_piece) > 0):
							sock_sent_len = sock.send(flv_piece);

					except Empty:
						pass;

				logger.warning("%s|reconnect to tcp://%s:%s" % (self._guid, self._crtmpserver_ip, self._crtmpserver_port));

		except Exception, e:
			self._tunnel._event_sock_error.set();
			sock.close();
			logger.error("%s|queue size:%d|close tcp://%s:%s|error:%s" % (self._guid, self._tunnel._queue.qsize(), self._crtmpserver_ip, self._crtmpserver_port, e));
			# clear queue data
			while not self._tunnel._queue.empty():
				self._tunnel._queue.get_nowait();

def main(guid, conf_file):
	# load flv stream conf
	try:
		config = ParseConfig(conf_file);

	except Exception, e:
			logger.error("config:%s|error:%s!|process exit!" % (conf_file, e));
			sys.exit();

	thread_tunnel_list = [];

	# init flv stream pushing threads
	crtmpserver_list = config.get_crtmp_server_list();
	for crtmpserver in crtmpserver_list:
		queue = Queue(maxsize = 512);
		thread_tunnel = ThreadTunnel(queue);

		addr = crtmpserver[0];
		crtmpserver_ip, crtmpserver_port = config.get_crtmp_sock_addr(addr);
		thread_push = PushFlvStream(guid, crtmpserver_ip, crtmpserver_port, addr, thread_tunnel);

		thread_tunnel.set_push_thead(thread_push);
		thread_tunnel_list.append(thread_tunnel);
		thread_push.start();

	# init flv stream pulling thread
	stream_ip, stream_port, stream_uri = config.get_flv_stream_addr(guid);
	thread_pull = PullFlvStream(guid, stream_ip, stream_port, stream_uri, thread_tunnel_list);
	thread_pull.start();	

	for thread_tunnel in thread_tunnel_list:
		thread_tunnel.set_pull_thead(thread_pull);

	thread_pull.join();

if __name__ == "__main__":
	if len(sys.argv) < 2:
		print("usage: %s guid") % sys.argv[0];
		sys.exit(1);
	else:
		main(sys.argv[1], sys.path[0] + "/flv_stream.conf");

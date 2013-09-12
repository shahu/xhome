#!/usr/bin/python
#encoding : utf-8

# live2 flv url: http://ip:port/live/channel/time.block
# live2 m3u url: http://ip:port/live/interval/delay/channel.m3u
# WSGIServer module: http://trac.saddi.com/flup
# http live streaming: http://tools.ietf.org/html/draft-pantos-http-live-streaming-07

import os, time, logging, ConfigParser
import wsgi_drm

# load wsgi conf
config = ConfigParser.ConfigParser()
config.read("/usr/local/nginx/wsgi/wsgi.conf")
logging.basicConfig( level = logging.INFO,
					 filename = os.path.join(config.get("log", "log_path"), 'wsgi.log'),
					 datefmt = config.get("log", "log_datefmt"),
					 format='%(asctime)s|%(levelname)s|%(message)s' )

len_before_delay= config.getint("m3u8", "len_before_delay")
len_after_delay= config.getint("m3u8", "len_after_delay")

def M3uBuilder(host, uri_m3u8, args_m3u8):
	# parse uri parameters
	uri_para_list = uri_m3u8.split("/")
	interval, delay, channel = int(uri_para_list[2]), int(uri_para_list[3]), uri_para_list[4].split(".")[0]

	# first timestamp in m3u8 playlist
	time_start = int(time.time() / interval) * interval - delay - len_before_delay * interval

	# define m3u8 header
	m3u8_header = [	'#EXTM3U\r\n',
					'#EXT-X-TARGETDURATION:'+ str(interval) + '\r\n',
					'#EXT-X-VERSION:3' + '\r\n',
					'#EXT-X-MEDIA-SEQUENCE:' + str(time_start/interval) + '\r\n' ]
	m3u8_content = ''.join(m3u8_header)
	if True == config.getboolean("drm", "protect"):
		drm_worker = wsgi_drm.DrmWorker()
		key_tag = drm_worker.format_key_tag(channel)
		if not key_tag is None:
			m3u8_content += key_tag

	# define m3u8 playlist without end tag
	for i in range(0, len_before_delay + len_after_delay):
		ts_timestamp = time_start + i*interval
		ts_interval = "#EXTINF:%s,\r\n" % str(interval)
		ts_uri = "http://%s/live/%s/%s.ts%s\r\n" % (host, channel, str(ts_timestamp), args_m3u8)
		m3u8_content = m3u8_content + ts_interval + ts_uri

	# record m3u8 length
	m3u8_length = 0
	for m3u8_string in m3u8_content:
		m3u8_length = m3u8_length + len(m3u8_string)
	logging.info(uri_m3u8 + '|' + str(time_start) + '|' + str(m3u8_length) + '|' + str(len_before_delay + len_after_delay))
	#logging.info(m3u8_content)

	return m3u8_content

def application(env, start_response):
	# parse env parameters
	host = config.get("m3u8", "ts_host")
	if host=="localhost":
		host = env["HTTP_HOST"]

	uri_m3u8 = env["REQUEST_URI"]
	args_m3u8 = env["QUERY_STRING"]
	if args_m3u8 <> '':
		args_m3u8 = '?' + args_m3u8

	# generate http body
	response_body = M3uBuilder(host, uri_m3u8, args_m3u8)

	# response http header
	response_status = '200 OK'
	response_headers = [ ('Content-Type', 'application/x-mpegURL'),
						 ('Cache-Control', 'no-cache'),
						 ('Connection', 'keep-alive'),
						 ('Content-Length', str(len(response_body)))]
	start_response(response_status, response_headers)

	# response http body
	return response_body

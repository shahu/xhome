#!/usr/bin/python
#encoding : utf-8

# live2 flv url: http://ip:port/live/channel/time.block
# live2 fragment url: http://ip:port/live/interval/delay/channel/(audio|video)/time.fragment
# WSGIServer module: http://trac.saddi.com/flup

import os, time, logging, subprocess, ConfigParser

# load wsgi conf
config = ConfigParser.ConfigParser()
config.read("/usr/local/nginx/wsgi/wsgi.conf")
logging.basicConfig( level = logging.INFO,
		     filename = os.path.join(config.get("log", "log_path"), 'wsgi.log'),
		     datefmt = config.get("log", "log_datefmt"),
		     format='%(asctime)s|%(levelname)s|%(message)s' ) 

# key : value = channel_id : last_uri
DICT_LAST_URI = {"" : ""}

def FfmpegWorker(stream_cmd, channel_id, av_track, seq_id):
	# transform flv to fragment using ffmpeg
	if "video" == av_track:
		ffmpeg_cmd = '/usr/local/nginx/wsgi/ffmpeg -i - -vcodec copy -an -f ismv -frag_size 2000000 -ism_lookahead 2 -ppl_blockduration 5 -ppl_blockid '+ seq_id + ' -y '
		return RamdiskWorker(stream_cmd + '|' + ffmpeg_cmd, channel_id, seq_id + '.ismv')

	if "audio" == av_track:
		ffmpeg_cmd = '/usr/local/nginx/wsgi/ffmpeg -i - -acodec copy -vn -f ismv -frag_size 2000000 -ism_lookahead 2 -ppl_blockduration 5 -ppl_blockid '+ seq_id + ' -y '
		return RamdiskWorker(stream_cmd + '|' + ffmpeg_cmd, channel_id, seq_id + '.isma')

def RamdiskWorker(stream_cmd, channel_id, seq_file):
	# save temp ramdisk file
	ramdisk_path = "%s/%s/" % (config.get("sstr", "ramdisk_path"), channel_id)
	if False == os.path.exists(ramdisk_path):
		os.mkdir(ramdisk_path, 0777)

	ramdisk_file = ramdisk_path + seq_file
	os.system(stream_cmd + ramdisk_file)

	try:
		seg_fp = None
		seg_fp = open(ramdisk_file, 'r')
		stream = seg_fp.readlines()
	except:
		stream = ''
	if None != seg_fp:
		seg_fp.close()

	# remove temp fragment store file from ramdisk
	try:
		os.remove(ramdisk_file)
	except:
		logging.warning("[Except] os.remove ramdisk_file: %s" % ramdisk_file)

	return stream

def application(env, start_response):
	# parse uri parameters
	uri_req = env["REQUEST_URI"]
	uri_para_list = uri_req.split("/")
	channel_id, av_track, seq_id = uri_para_list[2], uri_para_list[3], uri_para_list[4][0:10]

	# curl flv block from localhost
	uri_block = "%s/live/%s/%s.block?type=flv2frag" % (config.get("sstr", "ip_port_block"), channel_id, seq_id)
	curl_flv_cmd = "curl -H Range:bytes=%s %s" % (config.get("sstr", "range_flv"), uri_block)

	try:
		# record block deliver and transcode time-consuming
		time_start = time.time()
		fragment_stream = FfmpegWorker(curl_flv_cmd, channel_id, av_track, seq_id)
		time_consuming = time.time() - time_start

		# record fragment length after transcoding
		fragment_len = 0
		for fragment_str in fragment_stream:
			fragment_len += len(fragment_str)

		if (fragment_len > 0):
			# record last normal fragment request by global DICT_LAST_URI
			global DICT_LAST_URI
			DICT_LAST_URI[channel_id]= uri_block

			response_status = '200 OK'
			response_headers = [ ('Content-Type', 'application/octet-stream'),
						('Cache-Control', 'cache'),
						('Content-Length', str(fragment_len)) ]

			logging.info("%s|%s|%s" % (uri_req, str(time_consuming), str(fragment_len)))

		else:
			# transform flv block to fragment failed
			symbol_flv = RamdiskWorker(curl_flv_cmd + " -o ", channel_id, seq_id + ".flv")[0][0:3]

			if symbol_flv == config.get("sstr", "symbol_flv"):
				curl_hold_cmd = "curl -H Range:bytes=%s %s -o " % (config.get("sstr", "range_hold"), uri_block)
				symbol_hold = RamdiskWorker(curl_hold_cmd, channel_id, seq_id + ".flv")[0][0:5]
				# position hold block
				if symbol_hold == config.get("sstr", "symbol_hold"):
					logging.warning("%s|%s|%s" % (uri_req, str(time_consuming), 'hold block'))

					# subsitute with last fragment stream
					uri_fragment_switch = "%s%s" % (config.get("sstr", "ip_port_block"), DICT_LAST_URI[channel_id])

					time_start = time.time()
					curl_flv_cmd = "curl -H Range:bytes=%s %s" % (config.get("sstr", "range_flv"), DICT_LAST_URI[channel_id])
					fragment_stream = FfmpegWorker(curl_flv_cmd, channel_id, av_track, seq_id)
					time_consuming = time.time() - time_start

					logging.warning("%s|%s|%s" % (DICT_LAST_URI[channel_id], str(time_consuming) , 'hold replace'))

				# blank block
				else:
					logging.warning("%s|%s|%s" % (uri_req, str(time_consuming), 'blank block'))

				response_status = '200 OK'
				response_headers = [ ('Content-Type', 'application/octet-stream'),
							 ('Cache-Control', 'cache') ]

			else:
				response_status = '404 Not Found'
				response_headers = [('Content-Type', 'text/html')]

				logging.error("%s|%s|%s" % (uri_req, str(time_consuming), '404 Not Found'))
	except:
		response_status = '502 Bad Gateway'
		response_headers = [('Content-Type', 'text/html')]
	
	# response http header
	start_response(response_status, response_headers)

	# response http body
	return fragment_stream

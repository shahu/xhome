#!/usr/bin/python
#encoding : utf-8

# live2 flv url: http://ip:port/live/channel/time.block
# live2 ts url: http://ip:port/live/channel/time.ts
# WSGIServer module: http://trac.saddi.com/flup

import os, time, logging, subprocess, ConfigParser
import wsgi_drm

# load wsgi conf
config = ConfigParser.ConfigParser()
config.read("/usr/local/nginx/wsgi/wsgi.conf")
logging.basicConfig( level = logging.INFO,
		     filename = os.path.join(config.get("log", "log_path"), 'wsgi.log'),
		     datefmt = config.get("log", "log_datefmt"),
		     format='%(asctime)s|%(levelname)s|%(message)s' ) 

# key : value = channel_id : last_uri
DICT_LAST_TS_URI = {"" : ""}

def FfmpegWorker(stream_cmd, channel_id, seq_id):
	# transform flv to mpegts using ffmpeg
	ffmpeg_cmd = '/usr/local/nginx/wsgi/ffmpeg -i - -acodec aac -strict experimental -ab 48k -vcodec copy -vbsf h264_mp4toannexb -f mpegts -y '
	#ffmpeg_cmd = 'ffmpeg -i - -acodec libmp3lame -ar 48000 -ab 64k -vcodec copy -vbsf h264_mp4toannexb '

	return RamdiskWorker(stream_cmd + '|' + ffmpeg_cmd, channel_id, seq_id)

def PpboxSDKWorker(stream_cmd, channel_id, seq_id):
	# transform flv to mpegts using PPbox-sdk
	ramdisk_path = config.get("ts", "ramdisk_path") + '/' + channel_id + '/'
	ramdisk_file = ramdisk_path + seq_id
	ramdisk_file_flv = ramdisk_path + seq_id + '.flv'
	if False == os.path.exists(ramdisk_path):
		os.mkdir(ramdisk_path, 0777)
	sdk_cmd = "%s --TransferModule.input_file=ppfile-flv://%s --TransferModule.output_format=ts --TransferModule.output_file=ppfile://%s" % (config.get("ts", "pptransfer_program"), ramdisk_file_flv, ramdisk_file)

	#return RamdiskWorker(stream_cmd + ';' + sdk_cmd, channel_id, seq_id)
	cmd_line = stream_cmd + ' --connect-timeout 5 -m 15 -o ' + ramdisk_file_flv + ' ;' + sdk_cmd
	try:
		exit_status = os.system(cmd_line)
		if True == config.getboolean("drm", "protect"):
			drm_worker = wsgi_drm.DrmWorker()
			drm_worker.encrypt_mpegts(ramdisk_file, channel_id)
	except:
		logging.info('[Except] os.system: ' + cmd_line)

	try:
		ts_fp = None
		ts_fp = open(ramdisk_file, 'r')
		stream = ts_fp.readlines()
	except:
		stream = ''
		logging.info('[Except] read ramdisk_file: ' + ramdisk_file)
		try:
			if (True == os.path.exists(ramdisk_file_flv) and 2048 < os.path.getsize(ramdisk_file_flv)):
				if 0 != exit_status:
					logging.info('Cmd exit status: ' + str(exit_status))

                                if config.getboolean("ts","bak_failed_ts"):
                                        failed_path = config.get("ts", "ramdisk_path") + '/failed/'
                                        if False == os.path.exists(failed_path):
                                                os.mkdir(failed_path, 0777)
                                        os.system('cp ' + ramdisk_file_flv + ' ' + failed_path)
		except:
			# What's a bad luck!!!
			logging.info('[Except] os.system: cp ' + ramdisk_file_flv + ' ' + failed_path)
	if None != ts_fp:
		ts_fp.close()
	
	# remove temp mpegts store file from ramdisk
	try:
		os.remove(ramdisk_file_flv)
	except:
		logging.info('[Except] os.remove ramdisk_file_flv: ' + ramdisk_file_flv)
	try:
		os.remove(ramdisk_file)
	except:
		logging.info('[Except] os.remove ramdisk_file: ' + ramdisk_file)
	
	return stream

def StreamWorker(stream_cmd, channel_id, seq_id):
	stream_cmd = stream_cmd + ' -o '
	
	return RamdiskWorker(stream_cmd, channel_id, seq_id)

def RamdiskWorker(stream_cmd, channel_id, seq_id):
	# save temp ramdisk file
	ramdisk_path = config.get("ts", "ramdisk_path") + '/' + channel_id + '/'
	ramdisk_file = ramdisk_path + seq_id
	if False == os.path.exists(ramdisk_path):
		os.mkdir(ramdisk_path, 0777)
	
	os.system(stream_cmd + ramdisk_file)

	try:
		ts_fp = None
		ts_fp = open(ramdisk_file, 'r')
		stream = ts_fp.readlines()
	except:
		stream = ''
	if None != ts_fp:
		ts_fp.close()

	# remove temp mpegts store file from ramdisk
	try:
		os.remove(ramdisk_file)
	except:
		logging.info('[Except] os.remove ramdisk_file: ' + ramdisk_file)

	return stream

def application(env, start_response):
	# parse uri parameters
	uri_req = env["REQUEST_URI"]
	uri_para_list = uri_req.split("/")
	channel_id, seq_id = uri_para_list[2], uri_para_list[3][0:10]

	# curl flv block from localhost
	uri_block = "%s/live/%s/%s.block?type=flv2ts" % (config.get("ts", "ip_port_block"), channel_id, seq_id)
	curl_flv_cmd = 'curl -H Range:bytes=' + config.get("ts", "range_flv") + ' ' + uri_block
	trans_method = config.get("ts", "method")

	try:
		# record block deliver and transcode time-consuming
		time_start = time.time()
		if 'pptransfer' == trans_method:
			ts_stream = PpboxSDKWorker(curl_flv_cmd, channel_id, seq_id)
		else:
			ts_stream = FfmpegWorker(curl_flv_cmd, channel_id, seq_id)

		time_consuming = time.time() - time_start
	
		# record mpegts length after transcoding
		ts_length = 0
		for ts_string in ts_stream:
			ts_length = ts_length + len(ts_string)

		if (ts_length > 0):
			# record last normal ts request by global DICT_LAST_TS_URI
			global DICT_LAST_TS_URI
			DICT_LAST_TS_URI[channel_id]= uri_req

			response_status = '200 OK'
			response_headers = [ ('Content-Type', 'application/octet-stream'),
					     ('Cache-Control', 'cache'),
					     ('Content-Length', str(ts_length)) ]

			logging.info(uri_req + '|' + str(time_consuming) + '|' + str(ts_length))

		else:
			# transform flv block to ts failed
			symbol_flv = StreamWorker(curl_flv_cmd, channel_id, seq_id)[0][0:3]
	
			if symbol_flv==config.get("ts", "symbol_flv"):
				curl_hold_cmd = 'curl -H Range:bytes=' + config.get("ts", "range_hold") + ' ' + uri_block
				symbol_hold = StreamWorker(curl_hold_cmd, channel_id, seq_id)[0][0:5]

				# position hold block
				if symbol_hold==config.get("ts", "symbol_hold"):
					logging.warning(uri_req + '|' + str(time_consuming) + '|' + 'hold block')
					
					# subsitute with last ts stream
					uri_ts_switch = config.get("ts", "ip_port_ts") + DICT_LAST_TS_URI[channel_id]
					
					time_start = time.time()
					ts_stream = StreamWorker('curl ' + uri_ts_switch, channel_id, seq_id)
					time_consuming = time.time() - time_start
	
					logging.warning(DICT_LAST_TS_URI[channel_id] + '|' + str(time_consuming) + '|' + 'hold replace')
				
				# blank block
				else:
					logging.warning(uri_req + '|' + str(time_consuming) + '|' + 'blank block')
				
				response_status = '200 OK'
				response_headers = [ ('Content-Type', 'application/octet-stream'),
						     ('Cache-Control', 'cache') ]

			else:
				response_status = '404 Not Found'
				response_headers = [('Content-Type', 'text/html')]

				logging.error(uri_req + '|' + str(time_consuming) + '|' + '404 Not Found')

	except:
		response_status = '502 Bad Gateway'
		response_headers = [('Content-Type', 'text/html')]
	
	# response http header
	start_response(response_status, response_headers)

	# response http body
	return ts_stream

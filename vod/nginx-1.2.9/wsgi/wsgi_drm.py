#!/usr/bin/python
#encoding : utf-8

try:
	import json
except:
	import simplejson as json

import os, sys, time, urllib2, ConfigParser, threading, base64, binascii

# load wsgi conf
config = ConfigParser.ConfigParser()
config.read("/usr/local/nginx/wsgi/wsgi.conf")

class DrmWorker(object):
	channel_drm = {}

	def __init__(self):
		self.load_channel()

	def timing_update(self):
		while True:
			self.update_channel()
			time.sleep(config.getint("drm", "interval"))

	def update_channel(self):
		response = urllib2.urlopen(config.get("drm", "drmlist"))
		data_str = response.read()

		f = open(config.get("drm", "drmjson"), 'w')
		f.write(data_str)
		f.close()

	def load_channel(self):
		if not os.path.exists(config.get("drm", "drmjson")):
			self.update_channel()

		data_lst = json.load(file(config.get("drm", "drmjson")))

		self.channel_drm.clear()
		for result in data_lst['result']:
			channel = result['streamGUID']
			
			key_base64 = result['drmkey'].encode('utf-8')
			key_binary = base64.decodestring(key_base64)
			key_hex = binascii.b2a_hex(key_binary)
	
			drm_url = result['drmUrl'].encode('utf-8')
			
			self.channel_drm[channel] = (key_hex, drm_url)
	
	def format_key_tag(self, channel):
		key_tag = None

		if self.channel_drm.has_key(channel):
			drm_url = self.channel_drm[channel][1]
			key_tag = "#EXT-X-KEY:METHOD=%s,URI=\"%s\",IV=0x%s\r\n" % (config.get("drm", "method"), drm_url, config.get("drm", "iv"))
		
		return key_tag
	
	def encrypt_mpegts(self, raw_file, channel):
		raw_file_tmp = raw_file + ".raw"
		
		key_hex = self.channel_drm[channel][0]
	
		cmd_line = "mv %s %s;/usr/bin/openssl %s -e -in %s -out %s -p -nosalt -iv %s -K %s" % (raw_file, raw_file_tmp, config.get("drm", "openssl_alg"), raw_file_tmp, raw_file, config.get("drm", "iv"), key_hex)
	
		exit_status = os.system(cmd_line)
	
		try:
			os.remove(raw_file_tmp)
		except:
			print "Remove %s Failed" % raw_file_tmp

		return exit_status

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

	worker = DrmWorker()
	#print worker.format_key_tag("44f76bfee0d74043b0f7e96efab77824")
	
	t = threading.Timer(0, worker.timing_update())
	t.start()

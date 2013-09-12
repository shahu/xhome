#!/usr/bin/python
#encoding : utf-8

# live2 flv url: http://ip:port/live/channel/time.block
# live2 manifest url: http://ip:port/live/interval/delay/channel.manifest

import os, time, logging, ConfigParser
from flvlib import tags
from flvlib.astypes import MalformedFLV

manifest_template="""\
<?xml version="1.0" encoding="UTF-8"?>
<SmoothStreamingMedia MajorVersion="2" MinorVersion="0" Duration="0" TimeScale="%d" IsLive="TRUE" LookAheadFragmentCount="%d" DVRWindowLength="0" CanSeek="TRUE" CanPause="TRUE">
<StreamIndex Type="video" Name="video" Subtype="H264" Chunks="0" TimeScale="%d" Url="%s/video/{start time}.fragment%s">
<QualityLevel Index="0" Bitrate="%d" FourCC="H264" MaxWidth="%d" MaxHeight="%d" CodecPrivateData="%s" />
<c t="%d" d="%d" r="%d"/>
</StreamIndex>
<StreamIndex Type="audio" Name="audio" Subtype="AACL" Chunks="0" TimeScale="%d" Url="%s/audio/{start time}.fragment%s">
<QualityLevel Index="0" Bitrate="%d" FourCC="AACL" AudioTag="255" Channels="%d" SamplingRate="%d" BitsPerSample="16" PacketSize="4" CodecPrivateData="" />
<c t="%d" d="%d" r="%d"/>
</StreamIndex>
</SmoothStreamingMedia>
"""

# load wsgi conf
config = ConfigParser.ConfigParser()
config.read("/usr/local/nginx/wsgi/wsgi.conf")
logging.basicConfig( level = logging.INFO,
		     filename = os.path.join(config.get("log", "log_path"), "wsgi.log"),
		     datefmt = config.get("log", "log_datefmt"),
		     format="%(asctime)s|%(levelname)s|%(message)s" ) 

def ManifestBuilder(uri_manifest, args_manifest):
	# parse uri parameters
	uri_para_list = uri_manifest.split("/")
	interval, delay, channel = int(uri_para_list[2]), int(uri_para_list[3]), uri_para_list[4].split(".")[0]

	# timestamp tags in manifest
	repeat_times = config.getint("sstr", "repeat_times")
	lookahead_count = config.getint("sstr", "lookahead_count")
	time_scale = config.getint("sstr", "time_scale")

	block_id_first = int(time.time() / interval - repeat_times + lookahead_count) * interval - delay
	time_start = block_id_first * time_scale
	time_during = interval * time_scale

	# parse flv metadata
	video_bitrate, video_width, video_height, video_codec_private, audio_bitrate, audio_channels, audio_samplingrate = FlvMetaDataParser(channel, str(block_id_first))


	manifest_content = manifest_template % ( time_scale, lookahead_count,
	time_scale, channel, args_manifest, video_bitrate, video_width, video_height, video_codec_private, time_start, time_during, repeat_times,
	time_scale, channel, args_manifest, audio_bitrate, audio_channels, audio_samplingrate, time_start, time_during, repeat_times)

	logging.info("%s|%s|%s" % (uri_manifest, str(time_start), str(len(manifest_content))))

	return manifest_content

def FlvMetaDataParser(channel, block_id_first):
	# save temp ramdisk file
	ramdisk_path = "%s/%s" % (config.get("sstr", "ramdisk_path"), channel)
	if False == os.path.exists(ramdisk_path):
		os.mkdir(ramdisk_path, 0777)

	block_uri = "%s/live/%s/%s.block" % (config.get("sstr", "ip_port_block"), channel, block_id_first)
	ramdisk_file = "%s/%s.flv" % (ramdisk_path, block_id_first)
	curl_flv_cmd = "curl -H Range:bytes=%s %s -o %s" % (config.get("sstr", "range_flv"), block_uri, ramdisk_file)
	os.system(curl_flv_cmd)

	try:
		f = open(ramdisk_file, "rb")
	except IOError, (errno, strerror):
		logging.error("Failed to open %s: %s", ramdisk_file, strerror)

	flv = tags.FLV(f)
	flv_metadata_dict = dict()
	try:
		tag_generator = flv.iter_tags()
		for i, tag in enumerate(tag_generator):
			if (isinstance(tag, tags.ScriptTag) and tag.name == "onMetaData"):
				for key in tag.variable:
					flv_metadata_dict[key] = tag.variable[key]
			if (isinstance(tag, tags.VideoTag)) and tag.video_codec_private != None:
					flv_metadata_dict["video_codec_private"] = tag.video_codec_private
					break;

	except MalformedFLV, e:
		message = e[0] % e[1:]
		logging.error("The file %s is not a valid FLV file: %s", ramdisk_file, message)

	# remove temp mpegts store file from ramdisk
	try:
		f.close()
		os.remove(ramdisk_file)
	except:
		logging.info("[Except] os.remove ramdisk_file: %s" % ramdisk_file)

	if flv_metadata_dict.has_key("videodatarate"):
		video_bitrate = flv_metadata_dict["videodatarate"] * 1000
		if 0 == video_bitrate:
			video_bitrate = config.getint("sstr", "default_videodatarate")
	else:
		video_bitrate = config.getint("sstr", "default_videodatarate")

	if flv_metadata_dict.has_key("width"):
		video_width = flv_metadata_dict["width"]
	else:
		video_width = config.getint("sstr", "default_width")

	if flv_metadata_dict.has_key("height"):
		video_height = flv_metadata_dict["height"]
	else:
		video_height = config.getint("sstr", "default_height")

	if flv_metadata_dict.has_key("video_codec_private"):
		video_codec_private = flv_metadata_dict["video_codec_private"]
	else:
		video_codec_private = config.get("sstr", "default_video_codec_private")

	if flv_metadata_dict.has_key("audiodatarate"):
		audio_bitrate = flv_metadata_dict["audiodatarate"] * 1000
		if 0 == audio_bitrate:
			audio_bitrate = config.getint("sstr", "default_audiodatarate")
	else:
		audio_bitrate = config.getint("sstr", "default_audiodatarate")

	if flv_metadata_dict.has_key("stereo") and flv_metadata_dict["stereo"]:
		audio_channels = 2
	else:
		audio_channels = 1

	if flv_metadata_dict.has_key("audiosamplerate"):
		audio_samplingrate = flv_metadata_dict["audiosamplerate"]
	else:
		audio_samplingrate = config.getint("sstr", "default_audiosamplerate")

	return video_bitrate, video_width, video_height, video_codec_private, audio_bitrate, audio_channels, audio_samplingrate

def application(env, start_response):
	# response http header
	response_status = "200 OK"
	response_headers = [ ("Content-Type", "text/xml"),
			     ("Cache-Control", "no-cache") ]
	start_response(response_status, response_headers)

	# parse env parameters
	uri_manifest = env["REQUEST_URI"]
	args_manifest = env["QUERY_STRING"]
	if args_manifest <> "":
		args_manifest = args_manifest.replace("&", "&amp;")
		args_manifest = "?" + args_manifest

	# response http body
	return ManifestBuilder(uri_manifest, args_manifest)

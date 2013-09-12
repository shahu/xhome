#!/bin/env python
#coding=utf-8

import os, sys, time
import logging, logging.handlers, ConfigParser
from xml.dom import minidom

class ConfigManager:
	def __init__(self, conf_file):
		self._channel_dict = {};
		self._conf_file = conf_file;

		try:
			self._config = ConfigParser.ConfigParser();
			self._config.read(self._conf_file);
			self.init_logger();

		except Exception, e:
			print("config:%s|error:%s" % (self._conf_file, e));
			raise e;

	def init_logger(self):
		global logger;

		log_file = self._config.get("updatelog", "log_file");
		log_level = self._config.get("updatelog", "log_level");
		log_datefmt = self._config.get("updatelog", "log_datefmt");
		log_max_size = self._config.get("updatelog", "log_max_size");
		log_back_count = self._config.get("updatelog", "log_back_count");
		
		logger = logging.getLogger();
		handler = logging.handlers.RotatingFileHandler(log_file, maxBytes=log_max_size, backupCount=log_back_count)
		formatter = logging.Formatter('%(asctime)s|%(levelname)s|%(threadName)s|%(lineno)d|%(message)s', log_datefmt);
		handler.setFormatter(formatter);
		
		logger.addHandler(handler);
		logger.setLevel(self.get_logger_level(log_level));			

	def get_logger_level(self, level):
		# set root logger level to warning
		LEVELS = { 'notset':logging.WARNING,
					'debug':logging.DEBUG,
					'info':logging.INFO,
					'warning':logging.WARNING,
					'error':logging.ERROR,
					'critical':logging.CRITICAL };
		
		return LEVELS.get(level.lower());

	def parse_channel_xml(self):
		try:
			rtmp_channel_xml = self._config.get("channelstream", "channel_xml");
			xml_fd = open(rtmp_channel_xml, "r");
			dom_doc = minidom.parse(xml_fd);
			dom_root = dom_doc.documentElement;
			item_list = dom_root.getElementsByTagName('items');

			for item in item_list:
				stream_list = item.getElementsByTagName('stream');
				for stream in stream_list:
					id = stream.getAttribute('id');
					title = stream.getAttribute('title');
					guid = stream.getAttribute('guid').lower();
					url = stream.getAttribute('url');
		
					self._channel_dict[guid] = (id, title, url);

		except Exception, e:
			logger.error("Can't parse Xml File: %s" % e);

	def update_channel_ini(self):
		try:
			rtmp_channel_ini = self._config.get("channelstream", "channel_ini");
			conf_fd = open(rtmp_channel_ini, "w+");
			conf_ini = ConfigParser.ConfigParser();
			conf_ini.readfp(conf_fd);

			target_guid = self._config.get("channelstream", "channel_index");
			proper_list = target_guid.split(",");

			# traverse channels locate proper guids
			for guid in self._channel_dict.keys():
				# proper guids update guid sections
				if guid[0] in proper_list:
					if not conf_ini.has_section(guid):
						conf_ini.add_section(guid);
						desc = self._channel_dict[guid][1];
						url = self._channel_dict[guid][2];

						conf_ini.set(guid, "url", url);
						conf_ini.set(guid, "desc", desc.encode("utf-8"));

						logger.warning("add section:%s desc:%s url:%s to %s" % (guid, desc, url, rtmp_channel_ini));
				# improper guids delete guid sections
				else:
					if conf_ini.has_section(guid):
						conf_ini.remove_section(guid);
						logger.warning("remove section:%s from %s" % (guid, rtmp_channel_ini));

			conf_ini.write(conf_fd);
			conf_fd.close();

		except Exception, e:
			logger.error("Can't parse Conf File: %s" % e);

if __name__ == "__main__":
	# load flv stream conf
	try:
		conf_file = sys.path[0] + "/flv_stream.conf";
		config = ConfigManager(conf_file);
		config.parse_channel_xml();
		config.update_channel_ini();

	except Exception, e:
		print("config:%s|error:%s!|process exit!" % (conf_file, e));
		sys.exit(1);

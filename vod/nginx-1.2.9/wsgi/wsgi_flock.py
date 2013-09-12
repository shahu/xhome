#!/usr/bin/python
#encoding : utf-8

import fcntl

class FileLock(object):

	def __init__(self, file):
		self.filepath = file
		self.fd = 0

	def exclusive_write(self, txt):
		self.fd = open(self.filepath, 'w')

		if self.fd:
			try:
				fcntl.flock(self.fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
				self.fd.write(txt)

			finally:
				fcntl.flock(self.fd, fcntl.LOCK_UN)
				self.fd.close()

	def shared_read(self):
		self.fd = open(self.filepath, 'r')
		txt = ''

		if self.fd:
			try:
				fcntl.flock(self.fd, fcntl.LOCK_SH | fcntl.LOCK_NB)
				txt = self.fd.read()

			finally:
				fcntl.flock(self.fd, fcntl.LOCK_UN)
				self.fd.close()

		return txt

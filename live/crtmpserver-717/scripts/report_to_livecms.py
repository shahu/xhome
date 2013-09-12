#!/bin/env python
# -*- coding: utf-8 -*-

import traceback
from urlparse import urlparse
import time
import httplib

USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:8.0) Gecko/20100101 Firefox/8.0"
push_zabbix_interface = "/tmp/webroot/zabbix_rtmp_error"
live_cms = "http://live-cms.synacast.com/api_stream_status?GUID=%s&Status=%d"

def fetch_url(url,read=False):
    '''通用的url访问函数，read参数默认为False不返回读取的数据，否则返回读url数据，
    返回值根据read的状态进行变化：
    read:True
        （http状态码,数据,错误标记)
    read:False
        （http状态码，耗时，数据大小，错误标记
    '''
    METHOD = "GET"
    code = 0
    data = ''
    elapsed = 0
    length = 0
    err = False
    try:
            scheme,hostname,path,param,query,fraid = urlparse(url)
            host = hostname.split(":")
            if len(host)>1 :
                    port = int(host[1])
                    host = host[0]
            else:
                    port = 80
                    host = host[0]
            uri = "%s?%s" % (path,query)
            start = time.time()
            conn = httplib.HTTPConnection(host,port)
            headers = {'User-Agent': USER_AGENT}
            conn.request(METHOD,uri,headers=headers)
            response = conn.getresponse()
            data = response.read()
            elapsed = time.time() - start
            code = response.status
            length = response.length
            if not length :
                    length = len(data)
            conn.close()
    except Exception,e:
            print("err","fetch_url occured error:%s\n%s" % (str(e),traceback.format_exc()))
            err = True
    if read :
            return code,data,err
    else:
            return code,elapsed,length,err,
        
def report_to_livecms(channel_id , status):
    stat = 2
    if status == "0":
            stat = 1
            
    url = live_cms % (channel_id , stat)
    code,data,err = fetch_url(url,True)
    print code,data,err
    if err or code != 200:
        print "error, return code: %d" % (code)
    else:
        print "successed: %s" %(data)

for line in open(push_zabbix_interface):
    line = line.strip("\n")
    if len(line) > 0:
        items = line.split(":")
        report_to_livecms( items[0] , items[1] )
    

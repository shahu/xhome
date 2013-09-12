#!/bin/env python
# -*- coding: utf-8 -*-

import sys,codecs,os,re
from StringIO import StringIO
from datetime import datetime,timedelta
from optparse import OptionParser

def format_html(data):
    '''表格样式和通用html部分'''
    css = '''<style type="text/css">
        table
        {
          border-collapse:collapse;
          width:100%;
        }

        table, td, th,tr
        {
            border:1px solid #4BACC6;
            font-size:10pt;
            text-align: center;
        }
         
        .header
        {
            background:#d2eaf1;
            font-weight:bold;
            text-align:center;
        }
        .title
        {
            font-size:11pt;
            font-weight:bold;
            text-align:center;
        }
        </style>'''
        
    return '''<html><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
            <meta http-equiv="Content-Language" content="zh-CN"/>%s</head><body>%s</body></html>''' % (css,data)

def process_overview(speedFile,monitorFile,detailLink,noTable=True):
    '''向上回源平均速度、下载平均速度、错误数量'''
    dirSpeeds = {}
    if os.path.exists(speedFile) :
        sf = open(speedFile)
        try:
            for line in sf:
                try:
                    time,stream,type,dir,speed,remote,local = line.split(",")
                    dirValue = dir.split(":")[1]
                    speedValue = speed.split(":")[1]
                    dirSpeeds.setdefault(dirValue,{})
                    dirSpeeds[dirValue].setdefault("total",0)
                    dirSpeeds[dirValue].setdefault("count",0)
                    dirSpeeds[dirValue]["total"] += float(speedValue)
                    dirSpeeds[dirValue]["count"] +=1 
                except:
                    print "read speed error" 
        finally:
            sf.close()
    
    errCount = 0
    if os.path.exists(monitorFile) :
        mf = open(monitorFile)
        try:
            for line in mf:
                try:
                    stream,status,counter = line.split(":")
                    counter = int(counter)
                    if counter > 0 :
                        errCount += counter 
                except:
                    print "read monitor error"
        finally:
            mf.close()
    
    fileName = detailLink.split(os.sep)[-1]
    timeValue = fileName.split(".")[0]
    iostream = StringIO()
    downSpeed = 0
    upSpeed = 0
    if dirSpeeds.has_key("up") :
        upSpeed = dirSpeeds["up"]["total"]/dirSpeeds["up"]["count"]
    if dirSpeeds.has_key("down") :
        downSpeed = dirSpeeds["down"]["total"]/dirSpeeds["down"]["count"]

    if not noTable :
        title = "rtmp info"
        iostream.write('''<table><thead><tr ><th colspan="5" class="title">%s</th></tr><tr class="header"><th>time</th><th>upstream speed</th><th>downstream speed</th>
            <th>errors</th><th>detail link</th></tr></thead>''' % (title))
    iostream.write('''<tr><td>%s</td><td>%.2f</td><td>%.2f</td><td>%d</td><td><a href="%s">detail</a></td></tr>''' % 
        (timeValue,upSpeed,downSpeed,errCount,fileName))
    if not noTable:
        iostream.write("</table>")
    return iostream.getvalue() 


def process_streams(speedFile,monitorFile,direction):
    '''流回源的平均速度、及错误数量'''
    errCounts = {}
    if os.path.exists(monitorFile): 
        mf = open(monitorFile)
        try:
            for line in mf:
                try:
                    stream,status,counter = line.split(":")
                    counter = int(counter)
                    if counter > 0:
                        errCounts[stream] = counter 
                except:
                    print "read monitor error"
        finally:
            mf.close()
    streamSpeeds = {}
    if os.path.exists(speedFile):
        sf = open(speedFile)
        try:
            for line in sf:
                try:
                    time,stream,type,dir,speed,remote,local = line.split(",")
                    dirValue = dir.split(":")[1]
                    if dirValue == direction:
                        speedValue = speed.split(":")[1]
                        typeValue = type.split(":")[1]
                        streamValue = stream.split(":")[1]
                        streamSpeeds.setdefault(streamValue,{})
                        streamSpeeds[streamValue].setdefault(typeValue,{})
                        streamSpeeds[streamValue][typeValue].setdefault("total",0)
                        streamSpeeds[streamValue][typeValue].setdefault("count",0)
                        streamSpeeds[streamValue][typeValue]["total"] += float(speedValue)
                        streamSpeeds[streamValue][typeValue]["count"] += 1
                except:
                    print "read speed error"
        finally:
            sf.close()

    iostream = StringIO()
    title = "stream info " + direction
    iostream.write('''<table><thead><tr ><th colspan="4" class="title">%s</th></tr><tr class="header"><th>stream</th><th>speed</th>
        <th>type</th><th>errors</th></tr></thead>''' % (title))
    if len(streamSpeeds) :
        for k,v in streamSpeeds.iteritems():
            for t,val in v.iteritems():
                if errCounts.has_key(k):
                    errCount = errCounts[k]
                else:
                    errCount = 0
                iostream.write("<tr><td>%s</td><td>%.2f</td><td>%s</td><td>%d</td></tr>" % (k,val["total"]/val["count"],t,errCount))
    elif len(errCounts) :
        for k,v in errCounts.iteritems():
            iostream.write("<tr><td>%s</td><td>%.2f</td><td>%s</td><td>%d</td></tr>" % (k,0.0,"none",v))

    iostream.write("</table>")
    return iostream.getvalue() 

def process_users(speedFile):
    '''用户下载平均速度'''
    if os.path.exists(speedFile): 
        sf = open(speedFile)
        try:
            userSpeeds = {}
            for line in sf:
                try:
                    time,stream,type,dir,speed,remote,local = line.split(",")
                    dirValue = dir.split(":")[1]
                    if dirValue == "down":
                        speedValue = speed.split(":")[1]
                        typeValue = type.split(":")[1]
                        ipValue = remote.split(":")[1]
                        userSpeeds.setdefault(ipValue,{})
                        userSpeeds[ipValue].setdefault(typeValue,{})
                        userSpeeds[ipValue][typeValue].setdefault("total",0)
                        userSpeeds[ipValue][typeValue].setdefault("count",0)
                        userSpeeds[ipValue][typeValue]["total"] += float(speedValue)
                        userSpeeds[ipValue][typeValue]["count"] += 1
                except:
                    print "read speed error"
            iostream = StringIO()
            title = "users info"
            iostream.write('''<table><thead><tr ><th colspan="3" class="title">%s</th></tr><tr class="header"><th>ip</th><th>speed</th>
                <th>type</th></tr></thead>''' % (title))
            for k,v in userSpeeds.iteritems():
                for t,val in v.iteritems():
                    iostream.write("<tr><td><a target=\"_blank\" href=\"http://%s/rtmp.html\">%s</a></td><td>%.2f</td><td>%s</td></tr>" % (k,k,val["total"]/val["count"],t))
            iostream.write("</table>")
            return iostream.getvalue() 
        finally:
            sf.close()
    return '' 

def process_upstream(confFile, hostFile):
    '''回源地址'''
    if os.path.exists(confFile):
        f = open(confFile)
        try:
            try:
				upstream = re.search(r'\s+upstream\s*=\s*"(\S+)"',f.read()).group(1)
				host = upstream.split("/")[2].split(":")[0]

				ip = ""
				for line in open(hostFile):
					items = line.strip(" \r\n").replace("\t", " ").split(" ")
					if len(items) >= 2 and ( items[0][0] != '#' and items[1] == host):
						ip = items[0]

				return '''<table><thead><tr ><th class="title" colspan="2">upstream</th></tr><tr class="header"><th>upstream</th><th>host</th>
							</tr></thead><tr><td>%s</td><td><a target="_blank" href="http://%s/rtmp.html">%s</a></td></tr></table>''' % (upstream, ip, ip)
            except:
                pass
        finally:
            f.close()

	return None

def format_index(dir):
    '''生成日期列表和引用当前day文件的html'''
    now = datetime.now()
    curr_day = datetime(now.year,now.month,1)
    title = "Navigator"
    stream = StringIO()
    stream.write('''<table><thead><tr ><th class="title">%s</th></tr><tr class="header"><th>Date</th></tr></thead>''' % (title))
    for day in range(31):
        next_day = curr_day + timedelta(day)
        
        if next_day.month == curr_day.month and next_day.year == curr_day.year :
            daystr = next_day.strftime("%Y%m%d")
            filename = "%s/index_rtmp.html" % daystr
            if os.path.exists(os.path.join(dir,daystr)) :
                stream.write('''<tr><td><a href="%s" target="frame_content"> %s<a></td></tr>''' % (filename,daystr) )
    stream.write("</table>")
    
    return stream.getvalue()

def make_index(filename):
    '''生成主页html文件'''
    html = format_index(os.path.dirname(filename))
    f = open(filename,"w")
    try:
        f.write(format_html('''<script type="text/javascript"> function resetContent(){
					var today = new Date();
					var year = today.getFullYear();
					var month = today.getMonth()<9 ? "0"+(today.getMonth()+1) : today.getMonth()+1;
					var date =  today.getDate()<10 ? "0"+today.getDate() : today.getDate();
					var page = year.toString() + month.toString() + date.toString() + "/index_rtmp.html";
					document.getElementById('frame_content').src=page;
                }
                window.onload=resetContent;
                </script>
                <div><div style="float:left;width:100px;">%s</div><div style="float:left;width:auto;"><iframe
                style="width:1000px;height:800px" id="frame_content" name="frame_content" scrolling="yes" frameborder="0" >
                </iframe></div></div>''' % html))
    finally:
        f.close()

if __name__ == "__main__":
    parser = OptionParser()
    
    parser.add_option("-V", "--overview",
                        dest="overview", metavar="OVERVIEW", default=False,action="store_true",
                        help="process overview")
    parser.add_option("-M","--streams",
                      dest="streams",default=False,action="store_true",
                      help="process streams")
    parser.add_option("-U","--users",
                      dest="users",default=False,action="store_true",
                      help="process users")
    parser.add_option("-P","--upstream",
                      dest="upstream",default=False,action="store_true",
                      help="process upstream")
    parser.add_option("-T","--update",
                      dest="update",default=False,action="store_true",
                      help="not generate html table,update table rows")
    parser.add_option("-S","--speed",
                        dest="speed",default="",
                        help="specify the speed filename")
    parser.add_option("-C","--channel",
                        dest="channel",default="",
                        help="specify the channel filename")
    parser.add_option("-F","--config",
                        dest="config",default="",
                        help="specify the config filename")
    parser.add_option("-O","--output",
                        dest="output",default="",
                        help="specify the output filename")
    parser.add_option("-I","--index",
                        dest="index",default="",
                        help="specify the main index filename")
    parser.add_option("-L","--link",
                        dest="link",default="",
                        help="specify the detail link for overview")
    (options, args) = parser.parse_args()
    content = ""
    if options.upstream :
        if os.path.exists(options.config):
            content = process_upstream(options.config, "/etc/hosts")
        else:
            print "pls make sure all filenames is correct"

    if options.overview :
        if os.path.exists(options.channel) or os.path.exists(options.speed):
            if content: 
                content += "<br>"+process_overview(options.speed,options.channel,options.link)
            else:
                content = process_overview(options.speed,options.channel,options.link,options.update)
        else:
            print "pls make sure all filenames is correct"

    if options.streams :
        if os.path.exists(options.channel) or os.path.exists(options.speed):
            if content :
                content += "<br>"+process_streams(options.speed,options.channel, "up")
                content += "<br>"+process_streams(options.speed,options.channel, "down")
            else:
                content = process_streams(options.speed,options.channel, "up")
                content = process_streams(options.speed,options.channel, "down")
        else:
            print "pls make sure all filenames is correct"

    if options.users :
        if os.path.exists(options.speed):
            if content :
                content += "<br>"+process_users(options.speed)
            else:
                content = process_users(options.speed)
        else:
            print "pls make sure all filenames is correct"
    if content:
        html = ''
        if options.output:
            if options.update:
                data=''
                f = open(options.output,"r")
                try:
                    data = f.read()
                finally:
                    f.close()
                pos = data.rfind('''</table''')
                if pos != -1 :
                    html = "%s%s%s" % (data[:pos],content,data[pos:])
                else:
                    html = format_html(content)
            f = open(options.output,"w")
            try:
                if not options.update:
                    html = format_html(content)
                f.write(html)
            finally:
                f.close()
        else:
            print "pls specify output filename"

    if options.index:
        make_index(options.index)
    

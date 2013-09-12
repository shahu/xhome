#!/bin/bash

stream_conf_xml="rtmp_channel.xml";
stream_conf_ini="rtmp_channel.ini";
channel_script="rtmp_channel.py"
stream_script="flv_stream.py";
manage_script="sock_manage_rtmp.py";
crtmpserver_root="/usr/local/crtmpserver/"

channel_cms_api="http://live-cms.synacast.com/api_all_rtmp_channel"

push_zabbix_interval=180
push_zabbix_interface="/tmp/webroot/zabbix_rtmp_error"
push_log="/home/logs/crtmpserver/push_streams.log"
push_pid="/home/logs/crtmpserver/push_streams.pid"
report_to_livecms="report_to_livecms.py"

push_start()
{
	arr_guid=`grep '\[[0-9a-zA-Z]\{32\}\]' ${crtmpserver_root}${stream_conf_ini} | awk '{print substr($1,2,32)}'`
	
	for guid in ${arr_guid[@]};
	do
		if test `ps -wef | grep ${stream_script} | grep ${guid} | grep -v grep | wc -l` -eq 0
		then
			python ${crtmpserver_root}${stream_script} ${guid} &
			push_logger ${guid} "start flv_stream!"
		fi
	done

    ps -ef | grep ${stream_script} | grep -v grep; 
}

push_update()
{
    # fetch rtmp channel list from live-cms
	curl -s --connect-timeout 20 -m 20 ${channel_cms_api} -o ${crtmpserver_root}${stream_conf_xml}

	arr_guid_ini=`grep '\[[0-9a-zA-Z]\{32\}\]' ${crtmpserver_root}${stream_conf_ini} | awk '{print substr($1,2,32)}'`

	# clear update channels from cms
    for guid in ${arr_guid_ini[@]};
    do
        guid_url_ini=`awk '/\['"$guid"'\]/{a=1}a==1&&$1~/url/{gsub(/[[:blank:]]*/,"",$NF);print $NF;exit}' ${crtmpserver_root}${stream_conf_ini}`
        
        guid_url_xml=`grep ${guid} ${crtmpserver_root}${stream_conf_xml} | grep stream | awk '{gsub(/url=|"|amp;|\/>/, "");print $5}'`

        # clear removed and updated channels
        if [ -z ${guid_url_xml} ] || [ ${guid_url_ini} != ${guid_url_xml} ]
        then
            ps -ef | grep ${stream_script} | grep ${guid} | grep -v grep | awk '{print $2}' | xargs kill -9
			push_logger ${guid} "stop flv_stream!"
        fi
    done

    # select channels by guid hash
	python ${crtmpserver_root}${channel_script}

	# clear no exist channels
	arr_guid_proc=`ps -ef | grep ${stream_script} | grep -v grep | awk '{print $NF}'`

    for guid in ${arr_guid_proc[@]};
    do
		guid_ini=`grep ${guid} ${crtmpserver_root}${stream_conf_ini} | awk '{print substr($1,2,32)}'`

		if [ -z ${guid_ini} ]
		then
            ps -ef | grep ${stream_script} | grep ${guid} | grep -v grep | awk '{print $2}' | xargs kill -9
			push_logger ${guid} "stop flv_stream!"
		fi
	done
}

push_stop()
{
	ps -ef | grep ${stream_script} | grep -v grep | awk '{print $2}' | xargs kill -9
	push_logger "all channles" "stop flv_stream!"
}

push_logger()
{
	timestamp=`date '+%F %T'`
	echo "${timestamp}|WARNING|push_stream.sh|$1|$2" >> ${push_log} 
}

push_zabbix()
{
    cat /dev/null > ${push_zabbix_interface}

	# count log error within last 2000 items
	tail -n 2000 ${push_log} | awk -F'|' '{ timestamp=$1;status=$2;guid=$5;gsub(/[-|:]/, " ", timestamp);if (systime()-mktime(timestamp)<INTERVAL && status=="ERROR") {++num[guid]} } END { for (k in num) {printf "%s:%d\n",  k, num[k] > INTERFACE;} }' INTERVAL=${push_zabbix_interval} INTERFACE=${push_zabbix_interface}

    # count guid withou error
    arr_guid=`grep '\[[0-9a-zA-Z]\{32\}\]' ${crtmpserver_root}${stream_conf_ini} | awk '{print substr($1,2,32)}'`
    for guid in ${arr_guid[@]};
    do
        grep -i -q ${guid} ${push_zabbix_interface}
        if [ $? -eq 1 ]
        then
            echo "${guid}:0" >> ${push_zabbix_interface}
        fi
    done
}

push_livecms()
{
    python ${crtmpserver_root}/${report_to_livecms}
}

push_mutex()
{
	script_file=$1
	pid_file=$2
	pid_curr=`cat ${pid_file}`

	trap "" HUP
	(   
		# check exist process
		if [ ! -f ${pid_file} ]; then
			echo "noexist_push_pid" > ${pid_file}
		fi

		in_proc=`ps -ef | grep ${script_file} | grep ${pid_curr} | grep -v grep | wc -l`

 		# check exclusive lock
		flock -e -n 200
		in_lock=$?

		if [ ${in_proc} -eq 0 ] && [ ${in_lock} -eq 0 ]; then
			echo $$ > ${pid_file}
			return 0
		else
			return 1
		fi 
	) 200> ${pid_file}
}

manage_start()
{
	if test `ps -wef | grep ${manage_script} | grep -v grep | wc -l` -eq 0
	then
		python ${crtmpserver_root}${manage_script}
	fi
}

show_usage()
{
	echo "usage: push_streams.sh [start|stop|monitor]";
}

if [ $# != 1 ] 
then
	show_usage;
	exit 1;
fi

push_mutex $0 ${push_pid}
if [ $? -eq 1 ]
then
	echo "push_streams.sh already running"
	push_logger "push_streams.sh already running!" "stop new process!"
	exit 1;
fi

if [ ! -f ${crtmpserver_root}${stream_conf_ini} ]
then
	echo "flv_stream.conf does not exist!";
else
	case $1 in
		"start")
            push_update; push_start; manage_start;
			;;
		"stop")
			push_stop;
			;;
		"monitor")
            push_update; push_start; push_zabbix; push_livecms; manage_start;
			;;
		*)
			show_usage;
	esac
fi

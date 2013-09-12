#/bin/bash

RTMP_DUMP_BIN=rtmpdump
RTMP_DUMP_PATH=./rtmpdumpdata

rtmp_dump_batch()
{
	for ip in ${arr_ip[@]};
	do
		mkdir -p ${RTMP_DUMP_PATH}/${ip}
	
		for guid in ${arr_guid[@]};
		do
			./${RTMP_DUMP_BIN} -r rtmp://${ip}:1935/flvplayback/ -y ${guid} -o ${RTMP_DUMP_PATH}/${ip}/${guid} -s http://172.16.205.57/jwplayer/player.swf -v &
		done
	
		sleep 30
		ps -ef | grep ${RTMP_DUMP_BIN} | awk '{print $2}' | xargs kill -9
	done

	find ${RTMP_DUMP_PATH} -size 0 > ${RTMP_DUMP_PATH}/result.txt
}

if [ $# -eq 0 ]
then
	curl 'http://live-cms.synacast.com/api_edge_server_live2' -o live2_edge.xml
	arr_ip=`grep ip live2_edge.xml | awk '{gsub(/ip=|"|amp;|\/>/, "");print $3}'`
else
	arr_ip=($1)
fi

curl 'http://live-cms.synacast.com/api_all_rtmp_channel' -o rtmp_channel.xml
arr_guid=`grep guid rtmp_channel.xml | awk '{gsub(/guid=|"|amp;|\/>/, "");print $4}'`

mkdir -p ${RTMP_DUMP_PATH}
rm -rf ${RTMP_DUMP_PATH}/*

rtmp_dump_batch

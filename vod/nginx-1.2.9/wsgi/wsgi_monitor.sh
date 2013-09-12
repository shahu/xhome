#!/bin/bash
export PATH=/usr/kerberos/sbin:/usr/kerberos/bin:/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin

wsgi_home=/usr/local/nginx/wsgi
wsgi_log=/mnt/resource/logs/wsgi/

TIME=`date +%Y-%m-%d-%H-%M`

############# environment check #############

suffix=`uname -i`
ffmpeg_bin="/usr/local/nginx/wsgi/ffmpeg"

if [ ! -f ${ffmpeg_bin} ];
then
	ln -s ${ffmpeg_bin}.${suffix} ${ffmpeg_bin}
	if [ ! -f ${ffmpeg_bin} ];
	then
		echo "ffmpeg not properly installed"
		echo ${TIME} ": ffmpeg not properly installed, start failed!" >> ${wsgi_log}/restart.log;
		exit 0
	fi
fi

ramdisk_dir="/mnt/resource/ramdisk/"

if [ ! -d ${ramdisk_dir} ];
then
	mkdir -p ${ramdisk_dir}
	chmod 777 ${ramdisk_dir}
	chown -R atncloud:atncloud ${ramdisk_dir}
	mount -t tmpfs -o size=128M tmpfs ${ramdisk_dir}
	if [ ! -d ${ramdisk_dir} ];
	then
		echo "ramdisk not properly installed"
		echo ${TIME} ": ramdisk not properly installed, start failed!" >> ${wsgi_log}/restart.log;
		exit 0
	fi
fi

############# existence check #############

wsgi_name=(wsgi_flv2ts wsgi_flv2m3u wsgi_flv2manifest wsgi_flv2fragment)
cpu_count=`cat /proc/cpuinfo|grep processor|wc -l`

for name in ${wsgi_name[*]}
do
	wsgi_num=`ps -wef| grep ${name} | grep -v grep | wc -l`

	if test $wsgi_num -lt ${cpu_count}
	then
		proc_num=$(($cpu_count*2))

		rm -f ${wsgi_home}/${name}.pyc

		# kill leaving wsgi process
		if test $wsgi_num -gt 0
		then
			ps -ef|grep ${name}|grep -v grep|awk '{print $2}'|xargs kill -9
		fi

		# start new wsgi process
		/bin/rm -f /var/tmp/${name}.sock
		${wsgi_home}/uwsgi.${suffix} --wsgi-file ${wsgi_home}/${name}.py --master -s /var/tmp/${name}.sock -p ${proc_num} --uid atncloud --gid atncloud --module wsgi -d /mnt/resource/logs/wsgi/uwsgi.log

		echo ${TIME} ": find process ${name} num: ${wsgi_num}, restart!" >> ${wsgi_log}/restart.log;
	fi
done

############# drm protection check ################

grep -i -q "protect=True" ${wsgi_home}/wsgi.conf

if test $? -eq 0
then

	drm_num=`ps -wef| grep wsgi_drm.py | grep -v grep | wc -l`

	if test $drm_num -lt 1
	then
		python ${wsgi_home}/wsgi_drm.py
	fi
fi

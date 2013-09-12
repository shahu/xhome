#!/bin/bash
PATH=/sbin:/usr/sbin:/bin:/usr/bin
export PATH LANG=C

PROCESS_RTMP_LOG_APP=$(dirname $0)/rtmplog2html.py
RTMP_SERVER=crtmpserver

rotate(){
	local oldName=$1
	local logPath=$(dirname "${oldName}")
	local newName="${logPath}/$(date "+%Y-%m-%d-%H-%M")-speed.log"
	mv -f ${oldName} ${newName}
	echo ${newName}
}	

analyse_all(){
	local speedFile=$1
    local monitorFile=$2
    local confFile=$3
    local resultFile=$4
    python ${PROCESS_RTMP_LOG_APP} -V -M -P -U -S "${speedFile}" -C "${monitorFile}" -O "${resultFile}" -F "${confFile}"
}

analyse_detail(){
	local speedFile=$1
    local monitorFile=$2
    local confFile=$3
    local resultFile=$4
    python ${PROCESS_RTMP_LOG_APP} -M -P -U -S "${speedFile}" -C "${monitorFile}" -O "${resultFile}" -F "${confFile}"
}

analyse_overview(){
	local speedFile=$1
    local monitorFile=$2
    local resultFile=$3
    local linkFile=$4
    if [ -f "${resultFile}" -a -s "${resultFile}" ]; then
        python ${PROCESS_RTMP_LOG_APP} -T -V -S "${speedFile}" -C "${monitorFile}" -O "${resultFile}" -L "${linkFile}"
    else
        python ${PROCESS_RTMP_LOG_APP} -V -S "${speedFile}" -C "${monitorFile}" -O "${resultFile}" -L "${linkFile}"
    fi
}

make_index(){
    local indexFile=$1
    python ${PROCESS_RTMP_LOG_APP} -I "${indexFile}"
}

main(){
    local action=$1
    local confFile=$2
    local resultPath=$3
    local bakPath="${resultPath}/$(date +%Y%m%d)"
    if [ ! -d "${bakPath}" ];then
        mkdir -p "${bakPath}"
    fi
    local mainIndex="${resultPath}/rtmp.html"
    local resultFile="${bakPath}/index_rtmp.html"
    local resultFile2="${bakPath}/$(date +%H-%M).rtmp.html"
    if [ -f "${confFile}" ]; then
        #get speed filename 
        local speedFile=$(grep  -E '[[:space:]]*speedPath=' "${confFile}" | grep -Po '"\S+"' | tr -d '"' )
        #get monitor filename
        local monitorFile=$(grep  -E '[[:space:]]*monitorPath=' "${confFile}" | grep -Po '"\S+"' | tr -d '"')
        if [ -f "${speedFile}" -o -f "${monitorFile}" ]; then
            #rotate speed log
            speedFile=$(rotate ${speedFile})
            if [ -n "${resultFile}" ]; then
                case "${action}" in 
                    overview)
                        analyse_overview "${speedFile}" "${monitorFile}" "${resultFile}" 
                        ;;
                    detail)
                        analyse_detail "${speedFile}" "${monitorFile}" "${confFile}" "${resultFile}"
                        ;;
                    all)
                        if [ -n "${resultFile2}" ]; then
                            analyse_detail "${speedFile}" "${monitorFile}" "${confFile}" "${resultFile2}"
                            analyse_overview "${speedFile}" "${monitorFile}" "${resultFile}" "${resultFile2}"
                            make_index "${mainIndex}"
                        else
                            #test only
                            analyse_all "${speedFile}" "${monitorFile}" "${confFile}" "${resultFile}"
                        fi
                        ;;
                esac
            fi
        fi
    fi
    #reset stream error count
    ps -C "${RTMP_SERVER}" -o pid= | xargs kill -USR2
}

main $*

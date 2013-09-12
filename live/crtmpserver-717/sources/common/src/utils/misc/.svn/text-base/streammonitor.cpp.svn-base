/* 
 *  Copyright (c) 2010,
 *  Gavriloaie Eugen-Andrei (shiretu@gmail.com)
 *
 *  This file is part of crtmpserver.
 *  crtmpserver is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  crtmpserver is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with crtmpserver.  If not, see <http://www.gnu.org/licenses/>.
 */


#include "utils/misc/streammonitor.h"

string StreamMonitor::_speedPath ;
string StreamMonitor::_monitorPath ;
ofstream StreamMonitor::_speedStream;
ofstream StreamMonitor::_monitorStream;
map<string,StreamMonitor::StreamInfo> StreamMonitor::_streams;

bool StreamMonitor::Init(Variant &configuration) {
	if (configuration.HasKeyChain(V_STRING, false, 1,
			"monitorPath")){
		_monitorPath = (string) configuration.GetValue("monitorPath", false);
	}
	if (configuration.HasKeyChain(V_STRING, false, 1,
			"speedPath")){
		_speedPath = (string) configuration.GetValue("speedPath", false);
	}
	return OpenMonitorFile() && OpenSpeedFile();
}

void StreamMonitor::final(){
	if( _monitorStream) 
		_monitorStream.close();
	if(_speedStream)
		_speedStream.close();
}

void StreamMonitor::OnStatus(const string& streamName,const string& streamType,StreamStatus streamStatus){
	if( streamStatus == StreamMaxIdle){
		_streams.erase(streamName);
	}
	else{
		if(_streams.find(streamName) == _streams.end()){
			_streams[streamName] = StreamInfo(streamStatus,streamType,streamStatus == StreamBroken?1:0);
		}
		else{
			_streams[streamName].type = streamType;
			_streams[streamName].status = streamStatus;
			if(streamStatus == StreamBroken){
				_streams[streamName].errorCount ++;
			}
			
		}
	}
	if(OpenMonitorFile()){
		FOR_MAP(_streams,string,StreamInfo,i){
			INFO("stream:%s,type:%sstatus:%d,error:%d",STR(MAP_KEY(i)),STR(MAP_VAL(i).type),(int)MAP_VAL(i).status,MAP_VAL(i).errorCount);
			_monitorStream<<MAP_KEY(i)<<":"<<(MAP_VAL(i).status == StreamAvailable?1:0)<<":"<<MAP_VAL(i).errorCount<<endl;
		}
		_monitorStream.close();
	}
}

void StreamMonitor::OnSpeed(const string& streamName,const string& streamType,StreamDirection direction,const double& speed,const string& remoteIP,const string& localIP,const uint32_t totalByte){
	if(OpenSpeedFile()){
		char outstr[200] = {0};
		time_t t =  time(NULL);
		struct tm *tmp = localtime(&t);
		if (tmp) {
			strftime(outstr,sizeof(outstr),"%Y-%m-%d %H:%M:%S",tmp);	
		}

		_speedStream<<"time:"<<outstr<<",stream:"<<streamName<<",type:"<<streamType<<",direction:"<<(direction == UpStream ?"up":"down")<<",speed:"
		<<speed<<",totalBytes:"<<totalByte<<",remote:"<<remoteIP<<",local:"<<localIP<<endl;

		_speedStream.close();
	}	
}

bool StreamMonitor::OpenMonitorFile() {
	if(_monitorPath.empty()) return false;
	if(_monitorStream) {
		_monitorStream.close();
	}
	ios_base::openmode openMode = ios_base::out | ios_base::binary | ios_base::trunc;
	_monitorStream.open(STR(_monitorPath), openMode);
	if (_monitorStream.fail()) {
		return false;
	}
	return true;
}

bool StreamMonitor::OpenSpeedFile() {
	if(_speedPath.empty()) return false;
	if(_speedStream) {
		_speedStream.close();
	}
	ios_base::openmode openMode = ios_base::out | ios_base::binary | ios_base::app ;
	_speedStream.open(STR(_speedPath), openMode);
	if (_speedStream.fail()) {
		return false;
	}
	return true;
}

void StreamMonitor::ResetError(){
	bool opened = OpenMonitorFile();
    FOR_MAP(_streams,string,StreamMonitor::StreamInfo,i){
        MAP_VAL(i).ResetError();
        INFO("stream:%s,type:%sstatus:%d,error:%d",STR(MAP_KEY(i)),STR(MAP_VAL(i).type),(int)MAP_VAL(i).status,MAP_VAL(i).errorCount);
        if( opened )
            _monitorStream<<MAP_KEY(i)<<":"<<(MAP_VAL(i).status == StreamAvailable?1:0)<<":"<<MAP_VAL(i).errorCount<<endl;
    }
    if( opened )
        _monitorStream.close();
}

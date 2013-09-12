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


#ifndef _STREAMMONITOR_H
#define	_STREAMMONITOR_H

#include "platform/platform.h"
#include "utils/misc/variant.h"

class DLLEXP StreamMonitor{
public:
	enum StreamStatus{StreamNone,StreamBroken,StreamAvailable,StreamMaxIdle};
	enum StreamDirection{UpStream,DownStream};
	struct StreamInfo{
		StreamStatus status;
		string type;
		int errorCount;	
		StreamInfo(){
			errorCount = 0;
		};

		StreamInfo(StreamStatus st,const string& ty,const int errCount){
			status = st;
			type = ty;
			errorCount = errCount;
		};

		StreamInfo(const StreamInfo& other){
			Assign(other);
		};
		
		StreamInfo& operator =(const StreamInfo& other){
			Assign(other);
			return *this;
		}

		void Assign(const StreamInfo& other){
			status = other.status;
			type = other.type;
			errorCount = other.errorCount;
		};
	    
        void ResetError(){
            errorCount = 0;
        };	
	};
private:
	static ofstream _monitorStream,_speedStream;
	static string _monitorPath;
	static string _speedPath;
	static map<string,StreamInfo> _streams;
public:
	static bool Init(Variant &configuration);
	static void OnStatus(const string& streamName,const string& streamType,StreamStatus streamStatus);
	static void OnSpeed(const string& streamName,const string& streamType,StreamDirection direction,const double& speed,const string& remoteIP,const string& localIP,const uint32_t totalByte);
    static void ResetError();
	static void final();
private:
	static bool OpenMonitorFile();
	static bool OpenSpeedFile();
};


#endif	/* _STREAMMONITOR_H */


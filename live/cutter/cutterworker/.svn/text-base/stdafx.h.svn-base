// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once

#include <stdio.h>

#ifdef WIN32

#include "targetver.h"
#include <tchar.h>

#else

#define _tmain main
#define _TCHAR char

#endif

// TODO: reference additional headers your program requires here

#ifdef WIN32
#include "../AsyncLilo.h"
#else 
#include "AsyncLilo.h"
#endif // WIN32

void set_logger(ALilo& lilo);
ALilo& get_logger();

#define ERROR_LOG(b, ...) get_logger().printf(lilogger::kLevelError, ##b);
#define ALARM_LOG(b, ...) get_logger().printf(lilogger::kLevelAlarm, ##b);
#define EVENT_LOG(b, ...) get_logger().printf(lilogger::kLevelEvent, ##b);
#define INFOR_LOG(b, ...) get_logger().printf(lilogger::kLevelInfor, ##b);
#define DEBUG_LOG(b, arg, ...) get_logger().printf(lilogger::kLevelDebug, b, ##arg);
// cutterworker.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

// #include "util/Util.h"
// #include <framework/network/NetName.h>
// #include <framework/logger/Logger.h>
// using namespace framework::configure;
// using namespace framework::logger;
// using namespace framework::network;
// 
// #include <boost/asio/read.hpp>
// #include <boost/asio/io_service.hpp>
// #include <boost/asio/streambuf.hpp>
// #include <boost/thread/thread.hpp>
// #include <boost/asio/deadline_timer.hpp>
// //#include <boost/date_time/posix_time/posix_time_duration.hpp>
// #include <boost/bind.hpp>
// using namespace boost::system;
// using namespace boost::asio;
//using namespace boost::asio::ip;
//#include "framework/Framework.h"
#include "StreamReader.h"
#include <string>
#include "framework/configure/Config.h"
#include "framework/logger/Logger.h"

int _tmain(int argc, _TCHAR* argv[])
{
	printf("Cutter Worker\n");
	printf("ver.4.1 released by Tady, 07052013\n");
	printf("\n");
	
//	std::string strArg1("http://192.168.222.94/channels/cctv2/flv.head"), strArg2;
	std::string strArg1("http://192.168.222.94:8080/stream.flv"), strArg2("c:\\tmp"), strArg3("100"), strArg4(""), strArg5("5");

	switch (argc)
	{
	case 6:
		{
			strArg5 = argv[5];
		}
	case 5:
		{
			strArg4 = argv[4];
		}
	case 4:
		{
			strArg3 = argv[3];
		}
	case 3:
		{
			strArg1 = argv[1];
			strArg2 = argv[2];
		}
		break;
	case 2:
		{
			strArg1 = std::string(argv[1]);
			if (strArg1 == "/?")
			{
				printf("Usage:  %s [src_url] [dst_dir] [cache_time] [header_cache_time] [interval]\n", argv[0]);
				return 0;
			}
		}
	default:
		{
			printf("~ Illegal params! ~\n");
			printf("Usage:  %s [src_url] [dst_dir] [cache_time] [header_cache_time] [interval]\n", argv[0]);
			return 0;
		}

	}

	boost::uint32_t piece_life_span = atoi(strArg3.c_str());
	if (piece_life_span <= 0 || piece_life_span > 2592000) // max 30 days.
		piece_life_span = 30;

	boost::uint32_t head_piece_life_span = atoi(strArg4.c_str());
	if (head_piece_life_span <= 0 || head_piece_life_span > 25920000) // max 300 days.
		head_piece_life_span = 10 * 24 * 3600;

	boost::uint32_t interval = atoi(strArg5.c_str());
	if (interval <= 0)
		interval = 5;

	printf("src url: %s\n", strArg1.c_str());
	printf("dst dir: %s\n", strArg2.c_str());
	printf("piece cache time: %d\n", piece_life_span);
	printf("head  cache time: %d\n", head_piece_life_span);
	printf("interval: %d\n", interval);

	std::string title = strArg2;
	std::string::size_type pos = strArg2.rfind("/");
	if (pos != std::string::npos)
	{
		title = strArg2.substr(pos + 1, std::string::npos);
	}

	// Config logger.
	framework::configure::Config conf("./cutter_base.ini");  
	framework::logger::global_logger().load_config(conf);

	unsigned int print_level = lilogger::kLevelDebug;
	unsigned int file_level = lilogger::kLevelNostr;
	std::string file_path;
	unsigned int file_cut_period = 0;
	unsigned int file_save_period = 0;
	conf.register_module("log")
		<< CONFIG_PARAM_NAME_NOACC("print_level", print_level)
		<< CONFIG_PARAM_NAME_NOACC("file_level", file_level)
		<< CONFIG_PARAM_NAME_NOACC("file_path", file_path)
		<< CONFIG_PARAM_NAME_NOACC("file_cut_period", file_cut_period)
		<< CONFIG_PARAM_NAME_NOACC("file_save_period", file_save_period);

	// Config media.
	bool cut_with_kf = true;
	bool need_modify_ts = true;
	conf.register_module("media")
		<< CONFIG_PARAM_NAME_NOACC("cut_with_key_frame", cut_with_kf)
		<< CONFIG_PARAM_NAME_NOACC("modify_flv_timestamp", need_modify_ts);

	// Config live-cms stuff.
	std::string livecms_host("live-cms.synacast.com");
	unsigned int report_status_period(60);
	conf.register_module("livecms")
		<< CONFIG_PARAM_NAME_NOACC("livecms_host", livecms_host)
		<< CONFIG_PARAM_NAME_NOACC("report_status_period", report_status_period);

	ALilo alilo(title.c_str(), print_level, file_level, 0, file_path.c_str(), file_cut_period, file_save_period);
	set_logger(alilo);
	alilo.printf(lilogger::kLevelInfor, "============================");

	alilo.printf(lilogger::kLevelInfor, "print_log_level: %d", print_level);
	alilo.printf(lilogger::kLevelInfor, "file_log_level: %d", file_level);
	alilo.printf(lilogger::kLevelInfor, "file_log_path: %s", file_path.c_str());
	alilo.printf(lilogger::kLevelInfor, "file_log_cut_period: %d", file_cut_period);
	alilo.printf(lilogger::kLevelInfor, "file_log_save_period: %d", file_save_period);

	alilo.printf(lilogger::kLevelInfor, "cut_with_key_frame: %d", cut_with_kf);
	alilo.printf(lilogger::kLevelInfor, "modify_flv_timestamp: %d", need_modify_ts);

	alilo.printf(lilogger::kLevelInfor, "livecms_host: %s", livecms_host.c_str());
	alilo.printf(lilogger::kLevelInfor, "report_status_period: %d", report_status_period);

	alilo.printf(lilogger::kLevelInfor, "============================");
	alilo.printf(lilogger::kLevelInfor, "Started.");
	
#ifdef WIN32
	// Set the console title	
	::SetConsoleTitle(title.c_str());
#endif

	CStreamReader reader(strArg1, strArg2, 2, cut_with_kf, interval,
		piece_life_span, head_piece_life_span, need_modify_ts,
		livecms_host, report_status_period);
//	CStreamReader reader(strArg1, strArg2, 2, true, interval, piece_life_span, head_piece_life_span);
//	CStreamReader reader(strArg1, strArg2, 1, true, 10, piece_life_span, head_piece_life_span);
//	CStreamReader reader(strArg1, strArg2, 0, false, 1, piece_life_span, head_piece_life_span);
	if (!reader.Start())
		return -1;
	return 0;
}


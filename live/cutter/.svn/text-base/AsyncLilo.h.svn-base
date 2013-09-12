#pragma once

#include <boost/asio/io_service.hpp>
#include <boost/shared_array.hpp>
#include "lightlogger.h"
#include <string>

namespace boost {
	class thread;
}
class lilogger;
class lilo
{
public:
	lilo(const char* module, unsigned int print_level = lilogger::kLevelDebug, unsigned int file_level = lilogger::kLevelNostr, 
		unsigned int trace_level = lilogger::kLevelNostr, const char* file_path = NULL, 
		unsigned int file_cut_period = 0, unsigned int file_save_period = 0);
	~lilo();

	void printf(unsigned int level, const char *format, ...);
	void print(unsigned int level, const char *str_content);
private:
	lilogger* get_lilogger();
	lilogger *lilogger_;

	std::string module_name_;
	unsigned int print_level_;
	unsigned int file_level_;
	unsigned int trace_level_;
	std::string file_path_;
	//int file_path_len_;
	time_t cur_file_tick_;
	unsigned int file_cut_period_;
	unsigned int file_save_period_;
	const char *file_name_format_;
	//const char *log_ts_format_;
};

class ALilo
{
public:
	//ALilo();
	ALilo(const char* module, unsigned int print_level = lilogger::kLevelDebug, unsigned int file_level = lilogger::kLevelNostr, 
		unsigned int trace_level = lilogger::kLevelNostr, const char* file_path = NULL, 
		unsigned int file_cut_period = 0, unsigned int file_save_period = 0);
	~ALilo();
	void printf(unsigned int level, const char *format, ...);

private:
	void run();
	void on_printf(unsigned int level, const boost::shared_array<char>& contents);
	boost::shared_array<char>& va2str(const char *format, ...);
	boost::shared_ptr<boost::asio::io_service::work> work_;
	boost::asio::io_service ios_;
	boost::thread* thread_;
	lilo lilo_;

};

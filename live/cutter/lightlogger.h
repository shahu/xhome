#pragma once
#include <time.h>
#include <stdarg.h>

static const char* cs_level_name_[6] = { "", "[Error]\0", "[Alarm]\0", "[Event]\0", "[Infor]\0", "[Debug]\0" };
class lilogger
{
public:
	enum
	{
		kLevelNostr		= 0,	// 消息级别的定义
		kLevelError,
		kLevelAlarm, 
		kLevelEvent, 
		kLevelInfor, 
		kLevelDebug,
		
	};

	enum
	{
		kOutputNone		= 0,
		kOutputConsole,
		kOutputFile,
		kOutputTrace
	};

	lilogger(unsigned int print_level = kLevelDebug, unsigned int file_level = kLevelNostr, 
		unsigned int trace_level = kLevelNostr, const char* file_path = NULL, 
		unsigned int file_cut_period = 0, unsigned int file_save_period = -1)
		: print_level_(print_level), file_level_(file_level)
		, trace_level_(trace_level)
		, file_cut_period_(file_cut_period), file_save_period_(file_save_period)
		, file_name_format_("[%y-%m-%d %H;%M;%S].log\0")
		, log_ts_format_("%y-%m-%d %X\0")
		, cur_file_tick_(0)
	{
		memset(file_path_, 0, 1024);
		file_path_len_ = file_path ? strlen(file_path) : 0;
		if (file_path  && file_path_len_ < 1024)
			strcpy(file_path_, file_path);
		else
			file_level_ = kLevelNostr;
	}

// 	lilogger(unsigned int print_level = kLevelDebug, unsigned int file_level = kLevelNostr, 
// 		unsigned int trace_level = kLevelNostr, const char* file_path = NULL)
// 		: print_level_(print_level), file_level_(file_level)
// 		, trace_level_(trace_level)
// 		, file_name_format_(NULL)
// 		, log_ts_format_("%y-%m-%d %X\0")
// 		, file_cut_period_(0), file_save_period_(-1)
// 		, file_path_len_(0)
// 		, cur_file_tick_(0)
// 	{
// 
// 	}

	~lilogger()
	{}

	inline void print(unsigned int level, const char *str_content)
	{
		if ((level > print_level_ && level > file_level_ && level > trace_level_)
			|| str_content == NULL)
			return; // Do nothing;

		char str_time[20];
		time_t now = time(NULL);
		strftime(str_time, 20, log_ts_format_, localtime(&now));

		if (level <= print_level_)
		{
			::printf("[%s] %s %s \r\n", str_time, cs_level_name_[level], str_content);
		}
		if (level <= file_level_)
		{
			FILE *Logfp = fopen(get_cur_file_name(), "a+");
			if (Logfp)
			{
				fprintf(Logfp, "%s %s %s \r\n", str_time,cs_level_name_[level], str_content);
				fclose(Logfp);
			}
		}
		if (level <= trace_level_)
		{

		}
	}

	inline void printf(unsigned int level, const char *format, ...)
	{
		if (level > print_level_
			&& level > file_level_
			&& level > trace_level_)
			return; // Do nothing;

		char pc[1024]; memset(pc, 0, 1024);
		va_list args;
		va_start(args, format);
		vsnprintf(pc, 1024 - 1, format, args);

		print(level, pc);
	}


private:

	char* get_file_name(time_t ts)
	{
		strftime(file_path_ + file_path_len_, 1024 - file_path_len_ - 1, file_name_format_, localtime(&ts));
		return file_path_;
	}

	inline char* get_cur_file_name()
	{ 
		if (file_cut_period_ > 0)
		{
			time_t file_tick = time(NULL) / file_cut_period_;
			if (file_tick - cur_file_tick_ > 0)
			{
				{ // Remove old file.
					char cmd[1024];
#ifdef WIN32
					sprintf(cmd, "del /q %s", get_remove_file_name());
#else
					sprintf(cmd , "rm -f %s", get_remove_file_name());
#endif
					system(cmd);
				}

				cur_file_tick_ = file_tick;
				file_tick *= file_cut_period_;
				strftime(file_path_ + file_path_len_, 1024 - file_path_len_ - 1
					, file_name_format_, localtime(&file_tick));
			}

		}

		return file_path_;
	}

	inline char* get_remove_file_name()
	{ return get_file_name((time(NULL) - file_save_period_)/ file_cut_period_ * file_cut_period_ - file_cut_period_); }


	unsigned int print_level_;
	unsigned int file_level_;
	unsigned int trace_level_;
	char file_path_[1024];
	int file_path_len_;
	time_t cur_file_tick_;
//	static const char* cs_level_name_[6];
	unsigned int file_cut_period_;
	unsigned int file_save_period_;
	const char *file_name_format_;
	const char *log_ts_format_;
};

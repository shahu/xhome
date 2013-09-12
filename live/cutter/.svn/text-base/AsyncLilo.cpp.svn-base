#include "AsyncLilo.h"

#include <boost/thread/thread.hpp>
#include <boost/bind.hpp>
#include <boost/filesystem.hpp>

lilo::lilo( const char* module, unsigned int print_level /*= lilogger::kLevelDebug*/, unsigned int file_level /*= lilogger::kLevelNostr*/, unsigned int trace_level /*= lilogger::kLevelNostr*/, const char* file_path /*= NULL*/, unsigned int file_cut_period /*= 0*/, unsigned int file_save_period /*= 0*/ )
: module_name_(module)
, print_level_(print_level), file_level_(file_level)
, trace_level_(trace_level)
, file_path_(file_path)
, file_cut_period_(file_cut_period), file_save_period_(file_save_period)
, file_name_format_("%d-%m-%y_%H-%M-%S\0")
, cur_file_tick_(0)
, lilogger_(NULL)
{
#ifdef WIN32
	module_name_ += ".log";
#endif
}

lilo::~lilo()
{
	if (lilogger_)
		delete lilogger_;
}

void lilo::printf( unsigned int level, const char *format, ... )
{
	char pc[1024]; memset(pc, 0, 1024);
	va_list args;
	va_start(args, format);
	vsnprintf(pc, 1024 - 1, format, args);

	get_lilogger()->print(level, pc);
}


void lilo::print( unsigned int level, const char *str_content )
{
	get_lilogger()->print(level, str_content);
}

lilogger* lilo::get_lilogger()
{
	if (file_level_ <= lilogger::kLevelNostr)
	{
		if (!lilogger_) 
			lilogger_ = new lilogger(print_level_, file_level_, trace_level_);
	}
	else if (file_cut_period_ > 0)
	{
		time_t file_tick = time(NULL) / file_cut_period_;
		if (file_tick - cur_file_tick_ > 0)
		{
			if (lilogger_) delete lilogger_;

			cur_file_tick_ = file_tick;
			file_tick *= file_cut_period_;

			char dir_name[32];
			strftime(dir_name, 32 - 1
				, file_name_format_, localtime(&file_tick));

			boost::filesystem::path cur_path(file_path_);
			cur_path /= std::string(dir_name);

			try
			{
				if (!boost::filesystem::exists(cur_path))
					boost::filesystem::create_directory( cur_path );
			}
			catch (const boost::filesystem::filesystem_error& e)
			{
				::printf("!! ERROR !! Log Dir Exception: %s\n", e.what());
				file_level_ = 0;
			}

			cur_path /= module_name_;
			lilogger_ = new lilogger(print_level_, file_level_, trace_level_, cur_path.string().c_str());

			// Remove old guys.
			time_t old_time = ((cur_file_tick_ * file_cut_period_  - file_save_period_)/ file_cut_period_ * file_cut_period_ - file_cut_period_);
			strftime(dir_name, 32 - 1
				, file_name_format_, localtime(&old_time));
			boost::filesystem::path old_path(file_path_);
			old_path /= std::string(dir_name);
			try 
			{
				if (boost::filesystem::exists(old_path))
					boost::filesystem::remove_all(old_path);
			}
			catch (const boost::filesystem::filesystem_error& e)
			{
				::printf("!! ERROR !! Log Dir Exception: %s\n", e.what());
				file_level_ = 0;
			}
		}
	}
	else
	{
		boost::filesystem::path cur_path(file_path_);
		try 
		{
			if (!boost::filesystem::exists(cur_path))
				boost::filesystem::create_directory( cur_path );
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			::printf("!! ERROR !! Log Dir Exception: %s\n", e.what());
			file_level_ = 0;
		}

		cur_path /= module_name_;
		lilogger_ = new lilogger(print_level_, file_level_, trace_level_, cur_path.string().c_str());
	}

	assert(lilogger_);
	return	lilogger_;
}

ALilo::ALilo( const char* module, unsigned int print_level /*= lilogger::kLevelDebug*/, unsigned int file_level /*= lilogger::kLevelNostr*/, unsigned int trace_level /*= lilogger::kLevelNostr*/, const char* file_path /*= NULL*/, unsigned int file_cut_period /*= 0*/, unsigned int file_save_period /*= 0*/ )
: lilo_(module, print_level, file_level, trace_level, file_path, file_cut_period, file_save_period)
{
	thread_ = new boost::thread( boost::bind( &ALilo::run, this ) );
}

ALilo::~ALilo()
{
	ios_.stop();
	delete thread_;
}

void ALilo::run()
{
	work_.reset(new boost::asio::io_service::work(ios_));
	ios_.run();
}

void ALilo::printf(unsigned int level, const char *format, ...)
{
	boost::shared_array<char> pc(new char[1024]);
	memset(pc.get(), 0, 1024);
	va_list args;
	va_start(args, format);
	vsnprintf(pc.get(), 1024 - 1, format, args);

	ios_.post(boost::bind(&ALilo::on_printf, this, level, pc));
}

void ALilo::on_printf( unsigned int level,const boost::shared_array<char>& contents )
{
	lilo_.print(level, contents.get());
}

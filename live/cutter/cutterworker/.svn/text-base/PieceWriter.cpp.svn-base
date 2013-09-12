#include "stdafx.h"
#include "PieceWriter.h"

#include <boost/thread/thread.hpp>
#include <boost/bind.hpp>
#include <fstream>
#include <sstream>

#include <util/serialization/stl/vector.h>
#include <util/archive/BinaryIArchive.h>
#include <util/archive/BinaryOArchive.h>
#include <util/archive/BigEndianBinaryOArchive.h>

using namespace util::archive;

#include <boost/date_time/posix_time/posix_time_types.hpp>


#include <boost/asio/buffer.hpp>
//#include <array>
//#include <protocol/udpserver_mod/CheckSum.h>
#include <framework/string/Uuid.h>
#include <framework/string/Md5.h>

#include <util/protocol/http/HttpClient.h>

#include "FlvStuff.h"

//#include <boost/iostreams/operations.hpp>

template <typename ConstBufferSequence>
inline boost::uint32_t check_sum_new(
									 ConstBufferSequence const & buffers)
{
	using namespace boost::asio;
	typedef typename ConstBufferSequence::const_iterator iterator;
	iterator iter = buffers.begin();
	iterator end = buffers.end();
	register boost::uint32_t crc = (boost::uint32_t)0x20110312312LL;
	register boost::uint32_t v0 = 0;
	register boost::uint32_t v1 = 0;
	boost::uint32_t const * buf = NULL;
	boost::uint32_t size = 0;
	// 兼容老算法的BUG
	boost::uint8_t last_byte = 0;
	for (; iter != end; ++iter) {
		buf = buffer_cast<boost::uint32_t const *>(buffer(*iter));
		assert(size == 0);
		assert(((boost::uint32_t)(intptr_t)buf & (sizeof(boost::uint32_t) - 1)) == 0);  // 地址必须4字节对齐
		size = buffer_size(buffer(*iter));
		// 兼容老算法的BUG，最后8个字节被拆成8个字节独立计算了
		iterator iter1(iter);
		if (++iter1 == end) {
			--size;
			last_byte = ((boost::uint8_t const *)buf)[size];
		}
		while (size >= sizeof(boost::uint32_t) * 2) {
			v0 = *buf++;
			v1 = *buf++;
			crc ^= ((crc << 14) ^ (crc >> 6))
				^ framework::system::BytesOrder::little_endian_to_host_long(v0 ^ v1);
			size -= sizeof(boost::uint32_t) * 2;
		}
	}
	if (size) {
		boost::uint8_t const * bytes = (boost::uint8_t const *)buf;
		while (size--) {
			crc ^= ((crc >> 13) ^ (*bytes++ & 0xFF) ^ (crc << 7));
		}
	}
	crc ^= ((crc >> 13) ^ (last_byte & 0xFF) ^ (crc << 7));
	return framework::system::BytesOrder::host_to_little_endian_long(crc);
}

// Check-point tag's data. The data is same as start-point tag.
static unsigned char CheckPointTagData[119] = {
	0x12, 0x00, 0x00, 0x68, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x02, 0x00, 0x0A, 0x6F, 0x6E,
	0x43, 0x75, 0x65, 0x50, 0x6F, 0x69, 0x6E, 0x74,
	0x08, 0x00, 0x00, 0x00, 0x04, 0x00, 0x04, 0x6E,
	0x61, 0x6D, 0x65, 0x02, 0x00, 0x09, 0x43, 0x75,
	0x65, 0x20, 0x50, 0x6F, 0x69, 0x6E, 0x74, 0x00,
	0x04, 0x74, 0x69, 0x6D, 0x65, 0x00, 0x40, 0x02,
	0x6A, 0x7E, 0xF9, 0xDB, 0x22, 0xD1, 0x00, 0x04,
	0x74, 0x79, 0x70, 0x65, 0x02, 0x00, 0x05, 0x65,
	0x76, 0x65, 0x6E, 0x74, 0x00, 0x0A, 0x70, 0x61,
	0x72, 0x61, 0x6D, 0x65, 0x74, 0x65, 0x72, 0x73,
	0x08, 0x00, 0x00, 0x00, 0x01, 0x00, 0x05, 0x73,
	0x74, 0x61, 0x72, 0x74, 0x00, 0x40, 0xFE, 0x24,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09,
	0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x73 
};
static boost::uint32_t CheckPointOffset = 101;

// Off-air tag's data.
static unsigned char OffairTagData[112] = {
	0x12, 0x00, 0x00, 0x61, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x02, 0x00, 0x0A, 0x6F, 0x6E,
	0x43, 0x75, 0x65, 0x50, 0x6F, 0x69, 0x6E, 0x74,
	0x08, 0x00, 0x00, 0x00, 0x04, 0x00, 0x04, 0x6E,
	0x61, 0x6D, 0x65, 0x02, 0x00, 0x09, 0x43, 0x75,
	0x65, 0x20, 0x50, 0x6F, 0x69, 0x6E, 0x74, 0x00,
	0x04, 0x74, 0x69, 0x6D, 0x65, 0x00, 0x40, 0x02,
	0x6A, 0x7E, 0xF9, 0xDB, 0x22, 0xD1, 0x00, 0x04,
	0x74, 0x79, 0x70, 0x65, 0x02, 0x00, 0x05, 0x65,
	0x76, 0x65, 0x6E, 0x74, 0x00, 0x0A, 0x70, 0x61,
	0x72, 0x61, 0x6D, 0x65, 0x74, 0x65, 0x72, 0x73,
	0x08, 0x00, 0x00, 0x00, 0x01, 0x00, 0x05, 0x6F,
	0x6E, 0x61, 0x69, 0x72, 0x01, 0x00, 0x00, 0x00,
	0x09, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x6C 
};

// flv header
static unsigned char FlvHeader[382] = {
	0x46, 0x4C, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00,
	0x09, 0x00, 0x00, 0x00, 0x00, 0x12, 0x00, 0x01,
	0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x02, 0x00, 0x0A, 0x6F, 0x6E, 0x4D, 0x65, 0x74,
	0x61, 0x44, 0x61, 0x74, 0x61, 0x08, 0x00, 0x00,
	0x00, 0x0C, 0x00, 0x08, 0x64, 0x75, 0x72, 0x61,
	0x74, 0x69, 0x6F, 0x6E, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x77,
	0x69, 0x64, 0x74, 0x68, 0x00, 0x40, 0x7C, 0x20,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x68,
	0x65, 0x69, 0x67, 0x68, 0x74, 0x00, 0x40, 0x76,
	0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0D,
	0x76, 0x69, 0x64, 0x65, 0x6F, 0x64, 0x61, 0x74,
	0x61, 0x72, 0x61, 0x74, 0x65, 0x00, 0x40, 0x77,
	0x31, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09,
	0x66, 0x72, 0x61, 0x6D, 0x65, 0x72, 0x61, 0x74,
	0x65, 0x00, 0x40, 0x30, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x0C, 0x76, 0x69, 0x64, 0x65,
	0x6F, 0x63, 0x6F, 0x64, 0x65, 0x63, 0x69, 0x64,
	0x00, 0x40, 0x1C, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x0D, 0x61, 0x75, 0x64, 0x69, 0x6F,
	0x64, 0x61, 0x74, 0x61, 0x72, 0x61, 0x74, 0x65,
	0x00, 0x40, 0x3F, 0x40, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x0F, 0x61, 0x75, 0x64, 0x69, 0x6F,
	0x73, 0x61, 0x6D, 0x70, 0x6C, 0x65, 0x72, 0x61,
	0x74, 0x65, 0x00, 0x40, 0xE5, 0x88, 0x80, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x0F, 0x61, 0x75, 0x64,
	0x69, 0x6F, 0x73, 0x61, 0x6D, 0x70, 0x6C, 0x65,
	0x73, 0x69, 0x7A, 0x65, 0x00, 0x40, 0x30, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x73,
	0x74, 0x65, 0x72, 0x65, 0x6F, 0x01, 0x00, 0x00,
	0x0C, 0x61, 0x75, 0x64, 0x69, 0x6F, 0x63, 0x6F,
	0x64, 0x65, 0x63, 0x69, 0x64, 0x00, 0x40, 0x24,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08,
	0x66, 0x69, 0x6C, 0x65, 0x73, 0x69, 0x7A, 0x65,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x01, 0x17,
	0x08, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0xAF, 0x00, 0x12, 0x08, 0x00,
	0x00, 0x00, 0x0F, 0x09, 0x00, 0x00, 0x34, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x17, 0x00,
	0x00, 0x00, 0x00, 0x01, 0x64, 0x00, 0x33, 0xFF,
	0xE1, 0x00, 0x1F, 0x67, 0x64, 0x00, 0x33, 0xAC,
	0xD9, 0x41, 0xD0, 0xBF, 0x88, 0x97, 0xFF, 0x00,
	0x19, 0x00, 0x18, 0x10, 0x00, 0x00, 0x03, 0x00,
	0x10, 0x00, 0x00, 0x03, 0x02, 0x08, 0xF1, 0x83,
	0x19, 0x60, 0x01, 0x00, 0x05, 0x68, 0xEB, 0xEC,
	0xB2, 0x2C, 0x00, 0x00, 0x00, 0x3F 
};

static unsigned char OnMetaData[112] = {
	0x12, 0x00, 0x00, 0x61, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x02, 0x00, 0x0A, 0x6F, 0x6E,
	0x4D, 0x65, 0x74, 0x61, 0x44, 0x61, 0x74, 0x61,
	0x08, 0x00, 0x00, 0x00, 0x04, 0x00, 0x04, 0x6E,
	0x61, 0x6D, 0x65, 0x02, 0x00, 0x09, 0x43, 0x75,
	0x65, 0x20, 0x50, 0x6F, 0x69, 0x6E, 0x74, 0x00,
	0x04, 0x74, 0x69, 0x6D, 0x65, 0x00, 0x40, 0x02,
	0x6A, 0x7E, 0xF9, 0xDB, 0x22, 0xD1, 0x00, 0x04,
	0x74, 0x79, 0x70, 0x65, 0x02, 0x00, 0x05, 0x65,
	0x76, 0x65, 0x6E, 0x74, 0x00, 0x0A, 0x70, 0x61,
	0x72, 0x61, 0x6D, 0x65, 0x74, 0x65, 0x72, 0x73,
	0x08, 0x00, 0x00, 0x00, 0x01, 0x00, 0x05, 0x6F,
	0x6E, 0x61, 0x69, 0x72, 0x01, 0x00, 0x00, 0x00,
	0x09, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x6C 
};

static unsigned char VideoHeaderTag[63] = {
	0x09, 0x00, 0x00, 0x30, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x17, 0x00, 0x00, 0x00, 0x00,
	0x01, 0x64, 0x00, 0x15, 0xFF, 0xE1, 0x00, 0x1B,
	0x67, 0x64, 0x00, 0x15, 0xAC, 0xD9, 0x41, 0xD0,
	0xBF, 0x88, 0x97, 0x01, 0x10, 0x00, 0x00, 0x03,
	0x00, 0x10, 0x00, 0x00, 0x03, 0x02, 0x88, 0xF1,
	0x62, 0xD9, 0x60, 0x01, 0x00, 0x05, 0x68, 0xEB,
	0xEC, 0xB2, 0x2C, 0x00, 0x00, 0x00, 0x3B 
};

static unsigned char AudioHeaderTag[22] = {
	0x08, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0xAF, 0x00, 0x13, 0x88, 0x56,
	0xE5, 0x00, 0x00, 0x00, 0x00, 0x12 
};


//////////////////////////////////////////////////////////////////////////
// Class CPieceWriter

CPieceWriter::CPieceWriter(const std::string& path, const boost::uint32_t& piece_scale,
						   const boost::uint32_t& piece_life_span, 
						   const std::string& livecms_host, const boost::uint32_t& status_report_period)
: file_path_(path), piece_scale_(piece_scale), piece_life_span_(piece_life_span)
, stat_(STAT_FIRST_PIECE)
, clock_manager_(ios_, boost::posix_time::seconds(1))
, clock_(clock_manager_, 1000, boost::bind(&CPieceWriter::OnClockAlarm, this))
, current_piece_id_(0), reader_ok_(false)
, writing_piece_num_(0)
, checkpoint_scale_(CHECK_POINT_TAG_SCALE)
, cur_media_ts_(0)
, report_alarm_(5)
, livecms_host_(livecms_host)
, status_report_period_(status_report_period)
{
    guid_str_ = file_path_.filename();
    clock_manager_.start();
}

CPieceWriter::~CPieceWriter(void)
{
    clock_manager_.stop();
	ios_.stop();
}

bool CPieceWriter::Start()
{
	boost::thread* run_thread = new boost::thread( boost::bind( &CPieceWriter::Run, this ) );
	if ( 0 == run_thread ) {
		return false;
	}
	return true;
}
void CPieceWriter::AsyncWritePiece( const piece_ptr& piece, 
								   const boost::uint32_t& seek_point_pos, const boost::uint32_t& media_ts)
{
	if (writing_piece_num_ < 50)
	{
		++writing_piece_num_;
		ios_.post(boost::bind(&CPieceWriter::HandleWritePiece, this, piece, seek_point_pos, media_ts));
	}
}

void CPieceWriter::HandleWritePiece( const piece_ptr& piece, 
									const boost::uint32_t& seek_point_pos, const boost::uint32_t& media_ts)
{
	if (reader_ok_ == false)
	{
		ReportReaderStatus(true);
		reader_ok_ = true;
		get_logger().printf(lilogger::kLevelEvent, "!! ReaderOK !!");

	}
	--writing_piece_num_;

	if (stat_ == STAT_NORMAL)
	{
		cur_media_ts_ = media_ts;
		get_logger().printf(lilogger::kLevelDebug, "cur_media_ts_ < %d >", cur_media_ts_);
	}
}

void CPieceWriter::ReaderError()
{
	ios_.post(boost::bind(&CPieceWriter::HandleReaderError, this));
}

void CPieceWriter::HandleReaderError()
{
	get_logger().printf(lilogger::kLevelEvent, "!! ReaderError !!");

	ReportReaderStatus(false);
	reader_ok_ = false;

	stat_ =  STAT_FIRST_PIECE;

	boost::uint32_t now_id = GetUTC();
	while (current_piece_id_ != 0 && ((int)(now_id - current_piece_id_)) / (int)piece_scale_> 0)
	{
		WriteOneSpecialPiece();
	}
}

std::size_t CPieceWriter::DecreasePiece( const std::size_t& remove_size )
{
	std::size_t removed_size(0);

	while (remove_size > removed_size && !piece_file_queue_.empty())
	{
		try
		{
			if (boost::filesystem::exists(piece_file_queue_[0].path_))
			{
				boost::filesystem::remove(piece_file_queue_[0].path_);
				removed_size +=piece_file_queue_[0].size_;
			}
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
#ifdef WIN32
			switch (e.code().value())
			{
			case ERROR_ALREADY_EXISTS:
				break;
			default:
				break;
			}
#endif // WIN32
		}

		piece_file_queue_.pop_front();
	}

	return removed_size;
}

boost::uint32_t CPieceWriter::GetUTC()
{
// 	tm tm1 = to_tm(boost::posix_time::second_clock::universal_time());
// 	return (boost::uint32_t)mktime( &tm1 );
// 	time_t tt;
// 	tm *tm2;
// 	time(&tt);
// 	tm2 = localtime(&tt);
// 	boost::uint32_t time1 = mktime(tm2);
// 	boost::uint32_t time2 = time(NULL);
// 	printf("timestamp %d < == > %d\n", time1, time2);

	return (boost::uint32_t)time(NULL);
}

void CPieceWriter::OnClockAlarm()
{
	boost::uint32_t now_id = GetUTC();

	// Remove piece.
	while (!piece_file_queue_.empty()
		&& ((int)now_id - (int)(piece_file_queue_.front().id_) - (int)piece_scale_) > (int)piece_life_span_)
	{
		try
		{
			boost::filesystem::remove(piece_file_queue_.front().path_);
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
		}
		piece_file_queue_.pop_front();
	}

	// Add special piece.
	while (!reader_ok_)
	{
		if (current_piece_id_ == 0 || ((int)(now_id - current_piece_id_) / (int)piece_scale_) <= 0)
			break;
		WriteOneSpecialPiece();
	}

	if (--report_alarm_ <= 0)
	{
		ReportReaderStatus(reader_ok_);
	}
		
}

void on_fetch(boost::system::error_code const & ec, boost::shared_ptr<util::protocol::HttpClient> client_ptr)
{
	if (ec)
		get_logger().printf(lilogger::kLevelDebug, "Report failed: [%d] %s", ec.value(), ec.message().c_str());
	else
		get_logger().printf(lilogger::kLevelDebug, "Report HTTP stat: [%d] %s", client_ptr->response_head().err_code, client_ptr->response_head().err_msg.c_str());
	client_ptr.reset();
}

void CPieceWriter::ReportReaderStatus( bool status )
{
	boost::shared_ptr<util::protocol::HttpClient> reporter_ptr(new util::protocol::HttpClient(ios_));
	std::string status_report_url = "http://" + livecms_host_ + "/api_stream_status?GUID=" + guid_str_;
	if (status)
		status_report_url += "&Status=1";
	else
		status_report_url += "&Status=2";

	reporter_ptr->async_fetch_get(status_report_url, boost::bind(&on_fetch, _1, reporter_ptr));

	report_alarm_ = status_report_period_;
}

void CPieceWriter::AsyncUpdateHeader( const piece_ptr& piece )
{
	ios_.post(boost::bind(&CPieceWriter::HandleUpdateHeader, this, piece));
}

boost::uint32_t GetLastTsInPiece(const std::string& piece_path)
{
	boost::uint32_t ts(0);
	boost::uint32_t tag_size;
	size_t tag_size_pos;
	FLVTagHeader_t tag_header;
	size_t tag_header_pos;

	try
	{
		tag_size_pos = (size_t)boost::filesystem::file_size(piece_path) - 4;
		std::ifstream piece(piece_path.c_str(), std::ios::binary);
		while (ts == 0 && tag_size_pos > 1400)
		{
// 			boost::iostreams::seek(piece, (boost::iostreams::stream_offset)tag_size_pos, std::ios_base::beg, std::ios_base::in);
// 			boost::iostreams::read(piece, (char*)&tag_size, 4);
			piece.seekg(tag_size_pos, std::ios_base::beg);
			piece.read((char*)&tag_size, 4);
			tag_size = FLV_UI32(((unsigned char*)&(tag_size)));
			if (tag_size <= 0)
				break;
			tag_header_pos = tag_size_pos - tag_size;
			if (tag_header_pos < 1413)
				break;
			tag_size_pos =  tag_header_pos - 4;
// 			boost::iostreams::seek(piece, (boost::iostreams::stream_offset)tag_header_pos, std::ios_base::beg, std::ios_base::in);
// 			boost::iostreams::read(piece, (char*)&tag_header, sizeof(FLVTagHeader_t));
			piece.seekg(tag_header_pos, std::ios_base::beg);
			piece.read((char*)&tag_header, sizeof(FLVTagHeader_t));
			ts = FLVTagHeader_t::get_tag_ts(&tag_header);
		}
	}
	catch (const boost::filesystem::filesystem_error& e)
	{
		get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
	}

	return ts;
}
//////////////////////////////////////////////////////////////////////////
// Class CPPLPieceWriter

CPPLPieceWriter::CPPLPieceWriter( const std::string& path, const boost::uint32_t& piece_scale, 
								 const boost::uint32_t& piece_life_span, const boost::uint32_t& head_piece_life_span)
: CPieceWriter(path, piece_scale, piece_life_span)
, head_piece_life_span_(head_piece_life_span)
, header_piece_id_(0)
{
}

void CPPLPieceWriter::Run()
{
	boost::filesystem::directory_iterator end_itr;
	boost::uint32_t id;
	std::size_t size;
	std::map<boost::uint32_t, piece_file_info> pieces;
	boost::uint32_t now_id = GetUTC();
	
	// Load special-tags.
	if (boost::filesystem::exists("c:\\onair.tag") 
		&& boost::filesystem::file_size("c:\\onair.tag") > 0)
	{
		std::ifstream tag_file("c:\\onair.tag", std::ios::binary);
		if (tag_file)
		{
			tag_file.seekg (0, std::ios::end);
			offair_tag_.reset(new lite_buffer(tag_file.tellg()));
			tag_file.seekg (0, std::ios::beg);
			BinaryIArchive<> is(tag_file);
			while(is)
			{
				is >> offair_tag_;
			}
			tag_file.close();
		}
	}
	else
	{
		offair_tag_.reset(new lite_buffer(sizeof(OffairTagData)));
		memcpy(offair_tag_->buf_.get(), OffairTagData, sizeof(OffairTagData));
	}

	checkpoint_tag_.reset(new lite_buffer(sizeof(CheckPointTagData)));
	memcpy(checkpoint_tag_->buf_.get(), CheckPointTagData, sizeof(CheckPointTagData));
// 
// 	std::ofstream tag2_file("c:\\onair2.tag", std::ios::binary);
// 	BinaryOArchive<> os(tag2_file);
// 	os << offair_tag_buffer.get();
// 	tag2_file.close();

	// Remove xxx.piece.tmp
	try
	{
		for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
		{
			if ( !boost::filesystem::is_directory(iter->status()) 
				&& iter->path().extension() == std::string(".tmp"))
			{
				boost::filesystem::remove(iter->path());
			}
		}

		// Scan pieces from disk.
		for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
		{
			size = 0;
			if ( !boost::filesystem::is_directory(iter->status()) 
				&& iter->path().extension() == std::string(".piece"))
			{
				id = atoi(iter->path().stem().c_str());
				size = (size_t)boost::filesystem::file_size(iter->path());
				if (id != 0 && size != 0)
				{
					pieces.insert(std::make_pair(id, piece_file_info(id, iter->path().string(), size)));
				}
			}

		}
	}
	catch (const boost::filesystem::filesystem_error& e)
	{
		get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
	}

	for (std::map<boost::uint32_t, piece_file_info>::iterator iter = pieces.begin();
		iter != pieces.end(); ++iter)
	{
		piece_file_queue_.push_back(iter->second);
	}

	// Remove piece.
	while (!piece_file_queue_.empty()
		&& (int)(now_id - piece_file_queue_.front().id_)  - (int)piece_scale_ > (int)piece_life_span_)
	{
		try
		{
			boost::filesystem::remove(piece_file_queue_.front().path_);
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
		}
		piece_file_queue_.pop_front();
	}
	if (!piece_file_queue_.empty())
	{
		current_piece_id_ = piece_file_queue_.back().id_ + piece_scale_;
	}

	// Scan head piece from disk.
	pieces.clear();
	for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
	{
		size = 0;
		if ( !boost::filesystem::is_directory(iter->status()) 
			&& iter->path().extension() == std::string(".head"))
		{
			id = atoi(iter->path().stem().c_str());
			size = (size_t)boost::filesystem::file_size(iter->path());
			if (id != 0 && size != 0)
			{
				pieces.insert(std::make_pair(id, piece_file_info(id, iter->path().string(), size)));

				try
				{
					// xxx.piece
					boost::filesystem::path piece_path = iter->path();
					piece_path.replace_extension("");
					if (boost::filesystem::exists(piece_path))
						boost::filesystem::remove(piece_path);
					boost::filesystem::copy_file(iter->path(), piece_path);

				}
				catch (const boost::filesystem::filesystem_error& e)
				{
					get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
#ifdef WIN32
					switch (e.code().value())
					{
					case ERROR_ALREADY_EXISTS:
						break;
					default:
						break;
					}
#endif // WIN32				
				}
			}
		}
	}
	for (std::map<boost::uint32_t, piece_file_info>::iterator iter = pieces.begin();
		iter != pieces.end(); ++iter)
	{
		head_piece_file_queue_.push_back(iter->second);
	}

	// Remove head piece.
	while (!head_piece_file_queue_.empty()
		&& (int)(now_id - head_piece_file_queue_.front().id_)  - (int)piece_scale_ > (int)head_piece_life_span_)
	{
		try
		{
			// xxx.piece.head
			boost::filesystem::remove(head_piece_file_queue_.front().path_);
			// xxx.piece
			boost::filesystem::path piece_path = head_piece_file_queue_.front().path_;
			piece_path.replace_extension("");
			boost::filesystem::remove(piece_path);
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
		}
		head_piece_file_queue_.pop_front();
	}
	if (!head_piece_file_queue_.empty())
	{
		header_piece_id_ = head_piece_file_queue_.back().id_;
	}

	// Add absent pieces.
//	assert(now_id >=  current_piece_id_);
	while (current_piece_id_ > 0 && (int)(now_id - current_piece_id_) > 0)
	{
		WriteOneSpecialPiece();
	}

	// Run.
	clock_.start();
	work_.reset(new boost::asio::io_service::work(ios_));
	ios_.run();
}

void CPPLPieceWriter::HandleWritePiece(const piece_ptr& piece, 
									   const boost::uint32_t& seek_point_pos, const boost::uint32_t& media_ts)
{
	CPieceWriter::HandleWritePiece(piece, seek_point_pos, media_ts);

	boost::uint32_t piece_length(0);
	int error_value;
	std::size_t removed_size(0);
	boost::uint32_t seek_point_offset = seek_point_pos;

	do 
	{
		error_value = 0;
		switch (stat_)
		{
		case STAT_FIRST_PIECE:
			{
				header_piece_ = piece;
				stat_ = STAT_SECOND_PIECE;
			}
			return;
		case STAT_SECOND_PIECE:
			{
				boost::uint32_t now = GetUTC();
				if (current_piece_id_ > now)
				{ // now-id is already exist.
					get_logger().printf(lilogger::kLevelAlarm, ">Existent[%d] < ", now);
					return;
				}

				if (current_piece_id_ > 0)
				{
					while ((int)(GetUTC() - current_piece_id_) / (int)piece_scale_ > 0)
					{	// Padding empty pieces.
						WriteOneSpecialPiece();
					}
				}
				else
					current_piece_id_ = GetUTC() / piece_scale_ * piece_scale_;

				header_piece_id_ = current_piece_id_;

				// Calc Piece header values.
				for (int i = header_piece_->size() - 1; i >= 0; --i)
					piece_length += (*header_piece_)[i]->size_;
				piece_header_.DATA_HEADER_LENGTH = (boost::uint16_t)piece_length;

				for (int i = piece->size() - 1; i >= 0; --i)
					piece_length += (*piece)[i]->size_;
				piece_header_.PIECE_HEADER_LENGTH = PIECE_HEADER_T_SIZE + PIECE_DATA_HEADER_T_SIZE;
				piece_header_.PIECE_LENGTH = piece_header_.PIECE_HEADER_LENGTH + piece_length;

				if (NeedCheckPointTag() && checkpoint_tag_)
				{
					piece_header_.PIECE_LENGTH += checkpoint_tag_->size_;
					seek_point_offset += checkpoint_tag_->size_;
				}

				piece_header_.PIECE_ID = current_piece_id_;
				piece_header_.DATA_HEADER_PIECE_ID = header_piece_id_;

				piece_header_.CHECK_SUM = 0;

				piece_header_.HAS_SEEK_POINT = true;
				if (seek_point_pos < piece_length - piece_header_.DATA_HEADER_LENGTH)
					piece_header_.SEEK_POINT = seek_point_offset;
				else
					piece_header_.SEEK_POINT = 0;
				last_seek_point_piece_id_ = header_piece_id_;

				do {
					do {
						// Write file
						std::stringstream file_name;
						file_name << current_piece_id_ << ".piece.tmp";
						file_path_ /= file_name.str();
						std::string file_path_str = file_path_.string();
						std::ofstream piece_file(file_path_str.c_str(), std::ios::binary);
						if (!piece_file)
						{
							error_value = 1;
							break;
						}
						BinaryOArchive<> os(piece_file);
						if (NeedCheckPointTag() && checkpoint_tag_)
						{
							os << piece_header_;
							os << framework::container::make_array(&(header_piece_->front()), header_piece_->size());
							//							os << framework::container::make_array(&(checkpoint_tag_->buf_), checkpoint_tag_->size_);

							boost::uint64_t* check_id = (boost::uint64_t*)(checkpoint_tag_->buf_.get() + CheckPointOffset);
							double db_check_id = current_piece_id_;
							*check_id = framework::system::BytesOrder::host_to_big_endian_longlong(*(boost::uint64_t*)&db_check_id);
							os << checkpoint_tag_.get();
						}					
						else
						{
							os << piece_header_;
							os << framework::container::make_array(&(header_piece_->front()), header_piece_->size());
						}
//						os << piece_header_;//framework::container::make_array((unsigned char*)&piece_header_, PIECE_HEADER_T_SIZE);
//						os << framework::container::make_array((unsigned char*)&piece_data_header_, PIECE_DATA_HEADER_T_SIZE);
						os << framework::container::make_array(&(piece->front()), piece->size());
						if (!os)
						{
							error_value = 2;
							break;
						}
						piece_file.close();

						// Rename file
						try
						{ // 2 copies.
							// xxx.piece.head
							file_path_.replace_extension(".head");
							if (boost::filesystem::exists(file_path_))
								boost::filesystem::remove(file_path_);
							boost::filesystem::rename(file_path_str, file_path_);

							// xxx.piece
							file_path_str = file_path_.string();
							file_path_.replace_extension("");
							if (boost::filesystem::exists(file_path_))
								boost::filesystem::remove(file_path_);
							boost::filesystem::copy_file(file_path_str, file_path_);

						}
						catch (const boost::filesystem::filesystem_error& e)
						{
							get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
#ifdef WIN32
							switch (e.code().value())
							{
							case ERROR_ALREADY_EXISTS:
								break;
							default:
								break;
							}
#endif // WIN32
							error_value = e.code().value();
						}

					} while (false);

					if (error_value != 0)
					{
						// Maybe disk is full, remove some piece.
						if (removed_size < (2 * piece_header_.PIECE_LENGTH))
						{
							DecreasePiece(piece_header_.PIECE_LENGTH);
							removed_size += piece_header_.PIECE_LENGTH;
						}
						else
						{ // Removed enough, make a break.
#ifdef WIN32
							::Sleep(1000);
#else
							sleep(1);
#endif
						}
					}
					else
						break; // Goooood.

				} while (true);

				get_logger().printf(lilogger::kLevelEvent, "Written [%d.piece.head], size [%d]", current_piece_id_, piece_header_.PIECE_LENGTH);
				head_piece_file_queue_.push_back(
					piece_file_info(current_piece_id_, file_path_.string() + ".head", piece_header_.PIECE_LENGTH));
				file_path_.remove_filename();
				current_piece_id_ += piece_scale_;
				stat_ = STAT_NORMAL;

			}
			break;
		case STAT_NORMAL:
			{
				boost::uint32_t now = GetUTC();
				if (current_piece_id_ > now + piece_scale_)
				{ // now-id is already exist.
					get_logger().printf(lilogger::kLevelAlarm, ">Existent[%d] < ", now);
					return;
				}

// 				if (NeedCheckID() && (int)(now - current_piece_id_) > 2)
// 				{
// 					while (GetUTC() - current_piece_id_ > 0)
// 					{	// Padding empty pieces.
// 						WriteOneSpecialPiece(1);
// 					}
// 				}

				if (piece_scale_ > 3)
				{
					if (NeedCheckID() && (int)(now - current_piece_id_) / (int)piece_scale_ > 0)
					{
						while ((int)(GetUTC() - current_piece_id_) / (int)piece_scale_ > 0)
						{	// Padding empty pieces.
							WriteOneSpecialPiece(1);
						}
					}
				}
				else
				{
					if (NeedCheckID() && (int)(now - current_piece_id_) / (int)piece_scale_ > 2)
					{
						while ((int)(GetUTC() - current_piece_id_) /(int) piece_scale_ > 0)
						{	// Padding empty pieces.
							WriteOneSpecialPiece(1);
						}
					}
				}

				// Calc Piece header values.
				for (int i = piece->size() - 1; i >= 0; --i)
					piece_length += (*piece)[i]->size_;
				piece_header_.PIECE_HEADER_LENGTH = PIECE_HEADER_T_SIZE;
				piece_header_.PIECE_LENGTH = piece_header_.PIECE_HEADER_LENGTH + piece_length;

				if (NeedCheckPointTag() && checkpoint_tag_)
				{
					piece_header_.PIECE_LENGTH += checkpoint_tag_->size_;
					seek_point_offset += checkpoint_tag_->size_;
				}

				piece_header_.PIECE_ID = current_piece_id_;
				piece_header_.DATA_HEADER_PIECE_ID = header_piece_id_;

				piece_header_.CHECK_SUM = 0;

				if ((int)seek_point_pos < (int)(piece_length - piece_header_.DATA_HEADER_LENGTH))
				{
					piece_header_.HAS_SEEK_POINT = true;
					piece_header_.SEEK_POINT = seek_point_offset;
					last_seek_point_piece_id_ = current_piece_id_;
				}
				else
				{
					piece_header_.HAS_SEEK_POINT = false;
					piece_header_.SEEK_POINT = last_seek_point_piece_id_;
				}

				do 
				{
					do{
						// Write file
						std::stringstream file_name;
						file_name << current_piece_id_ << ".piece.tmp";
						file_path_ /= file_name.str();
						std::string file_path_str = file_path_.string();
						std::ofstream piece_file(file_path_str.c_str(), std::ios::binary);
						if (!piece_file)
						{
							error_value = 1;
							break;
						}

						BinaryOArchive<> os(piece_file);
						if (NeedCheckPointTag() && checkpoint_tag_)
						{
							os << piece_header_;
//							os << framework::container::make_array(&(checkpoint_tag_->buf_), checkpoint_tag_->size_);

							boost::uint64_t* check_id = (boost::uint64_t*)(checkpoint_tag_->buf_.get() + CheckPointOffset);
							double db_check_id = current_piece_id_;
							*check_id = framework::system::BytesOrder::host_to_big_endian_longlong(*(boost::uint64_t*)&db_check_id);
							os << checkpoint_tag_;
						}
						else
						{
							os << piece_header_;//framework::container::make_array((unsigned char*)&piece_header_, PIECE_HEADER_T_SIZE);
						}
						os << framework::container::make_array(&(piece->front()), piece->size());
						if (!os)
						{
							error_value = 2;
							break;
						}

						piece_file.close();

						// Rename file
						file_path_.replace_extension("");
						try
						{
							if (boost::filesystem::exists(file_path_))
								boost::filesystem::remove(file_path_);
							boost::filesystem::rename(file_path_str, file_path_);
						}
						catch (const boost::filesystem::filesystem_error& e)
						{
							get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
#ifdef WIN32
							switch (e.code().value())
							{
							case ERROR_ALREADY_EXISTS:
								break;
							default:
								break;
							}
#endif // WIN32
							error_value = e.code().value();
						}
					} while(false);

					if (error_value != 0)
					{
						// Maybe disk is full, remove some piece.
						if (removed_size < (2 * piece_header_.PIECE_LENGTH))
						{
							DecreasePiece(piece_header_.PIECE_LENGTH);
							removed_size += piece_header_.PIECE_LENGTH;
						}
						else
						{ // Removed enough, make a break.
#ifdef WIN32
							::Sleep(1000);
#else
							sleep(1);
#endif
						}
					}
					else
						break; // Gooooooooooood!

				} while (true);
				
				get_logger().printf(lilogger::kLevelEvent, "Written [%d.piece], size [%d]", current_piece_id_, piece_header_.PIECE_LENGTH);
				piece_file_queue_.push_back(piece_file_info(current_piece_id_, file_path_.string(), piece_header_.PIECE_LENGTH));
				file_path_.remove_filename();
				current_piece_id_ += piece_scale_;
			}
			break;
		default:
			assert(0);
		}

	} while (error_value);

}

void CPPLPieceWriter::OnClockAlarm()
{
	CPieceWriter::OnClockAlarm();

	boost::uint32_t now_id = GetUTC();
	// Remove head piece.
	while (!head_piece_file_queue_.empty()
		&& (int)(now_id - head_piece_file_queue_.front().id_) - (int)piece_scale_ > (int)head_piece_life_span_)
	{
		try
		{
			// xxx.piece.head
			boost::filesystem::remove(head_piece_file_queue_.front().path_);
			// xxx.piece
			boost::filesystem::path piece_path = head_piece_file_queue_.front().path_;
			piece_path.replace_extension("");
			boost::filesystem::remove(piece_path);
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
		}
		head_piece_file_queue_.pop_front();
	}

}

// Actually there are 2 types: 0, with an offair-tag, when reader is error; 1, empty piece, when id is not exact.
void CPPLPieceWriter::WriteOneSpecialPiece( int special_type )
{
	assert(special_type <= 1);
	int error_value(0);
	std::size_t removed_size;

	if (special_type <= 1)
	{
		piece_header_.PIECE_LENGTH = 
			piece_header_.PIECE_HEADER_LENGTH = PIECE_HEADER_T_SIZE;
		if (special_type == 0 && offair_tag_)
			piece_header_.PIECE_LENGTH += offair_tag_->size_;
		if (NeedCheckPointTag() && checkpoint_tag_)
			piece_header_.PIECE_LENGTH += checkpoint_tag_->size_;

		piece_header_.PIECE_ID = current_piece_id_;
		piece_header_.DATA_HEADER_PIECE_ID = header_piece_id_;
		piece_header_.CHECK_SUM = 0;

		piece_header_.HAS_SEEK_POINT = true;
		piece_header_.SEEK_POINT = 0;
		last_seek_point_piece_id_ = current_piece_id_;

		do{
			// Write file
			std::stringstream file_name;
			file_name << current_piece_id_ << ".piece.tmp";
			file_path_ /= file_name.str();
			std::string file_path_str = file_path_.string();
			std::ofstream piece_file(file_path_str.c_str(), std::ios::binary);
			if (!piece_file)
			{
				error_value = 1;
				break;
			}

			BinaryOArchive<> os(piece_file);
			// Pedding checkpoint-tag
			if (NeedCheckPointTag() && checkpoint_tag_)
			{
				os << piece_header_;
				boost::uint64_t* check_id = (boost::uint64_t*)(checkpoint_tag_->buf_.get() + CheckPointOffset);
				double db_check_id = current_piece_id_;
				*check_id = framework::system::BytesOrder::host_to_big_endian_longlong(*(boost::uint64_t*)&db_check_id);
				//				os << framework::container::make_array(&(checkpoint_tag_->buf_), checkpoint_tag_->size_);
				os << checkpoint_tag_.get();
			}
			else
				os << piece_header_;
			// pedding offair-tag
			if (special_type == 0 && offair_tag_)
			{
				os << offair_tag_;
			}

			if (!os)
			{
				error_value = 2;
				break;
			}

			piece_file.close();

			// Rename file
			file_path_.replace_extension("");
			try
			{
				if (boost::filesystem::exists(file_path_))
					boost::filesystem::remove(file_path_);
				boost::filesystem::rename(file_path_str, file_path_);
			}
			catch (const boost::filesystem::filesystem_error& e)
			{
				get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
#ifdef WIN32
				switch (e.code().value())
				{
				case ERROR_ALREADY_EXISTS:
					break;
				default:
					break;
				}
#endif // WIN32
				error_value = e.code().value();
			}

			if (error_value != 0)
			{
				// Maybe disk is full, remove some piece.
				if (removed_size < (2 * piece_header_.PIECE_LENGTH))
				{
					DecreasePiece(piece_header_.PIECE_LENGTH);
					removed_size += piece_header_.PIECE_LENGTH;
				}
				else
				{ // Removed enough, make a break.
#ifdef WIN32
					::Sleep(1000);
#else
					sleep(1);
#endif
				}
			}
			else
				break; // Success.

		} while(true);

		get_logger().printf(lilogger::kLevelEvent, "Written [%d.piece], size [%d]", current_piece_id_, piece_header_.PIECE_LENGTH);
		piece_file_queue_.push_back(piece_file_info(current_piece_id_, file_path_.string(), piece_header_.PIECE_LENGTH));
		file_path_.remove_filename();
		current_piece_id_ += piece_scale_;
	}
	
}



//////////////////////////////////////////////////////////////////////////
// Class CFLVPieceWriter

void CFLVPieceWriter::Run()
{
	boost::filesystem::directory_iterator end_itr;
	boost::uint32_t id;
	std::size_t size;
	std::map<boost::uint32_t, piece_file_info> pieces;
	boost::uint32_t now_id = GetUTC();

	// Load special-tags.
	if (boost::filesystem::exists("c:\\onair.tag") 
		&& boost::filesystem::file_size("c:\\onair.tag") > 0)
	{
		std::ifstream tag_file("c:\\onair.tag", std::ios::binary);
		if (tag_file)
		{
			tag_file.seekg (0, std::ios::end);
			offair_tag_.reset(new lite_buffer(tag_file.tellg()));
			tag_file.seekg (0, std::ios::beg);
			BinaryIArchive<> is(tag_file);
			while(is)
			{
				is >> offair_tag_;
			}
			tag_file.close();
		}
	}
	else
	{
		offair_tag_.reset(new lite_buffer(sizeof(OffairTagData)));
		memcpy(offair_tag_->buf_.get(), OffairTagData, sizeof(OffairTagData));
	}

	checkpoint_tag_.reset(new lite_buffer(sizeof(CheckPointTagData)));
	memcpy(checkpoint_tag_->buf_.get(), CheckPointTagData, sizeof(CheckPointTagData));

	header_piece_len_ = sizeof(FlvHeader);
	liter_buffer_ptr flv_header(new lite_buffer(header_piece_len_));
	header_piece_.reset(new std::vector<liter_buffer_ptr>());
	memcpy(flv_header->buf_.get(), FlvHeader, header_piece_len_);
	header_piece_->push_back(flv_header);
	// 
	// 	std::ofstream tag2_file("c:\\onair2.tag", std::ios::binary);
	// 	BinaryOArchive<> os(tag2_file);
	// 	os << offair_tag_buffer.get();
	// 	tag2_file.close();

	// Remove xxx.flv.tmp
	for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
	{
		if ( !boost::filesystem::is_directory(iter->status()) 
			&& iter->path().extension() == std::string(".tmp"))
		{
			try
			{
				boost::filesystem::remove(iter->path());
			}
			catch (const boost::filesystem::filesystem_error& e)
			{
				get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
			}
		}
	}

	// Scan pieces from disk.
	for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
	{
		size = 0;
		if ( !boost::filesystem::is_directory(iter->status()) 
			&& iter->path().extension() == std::string(".flv"))
		{
			id = atoi(iter->path().stem().c_str());
			size = (size_t)boost::filesystem::file_size(iter->path());
			if (id != 0 && size != 0)
			{
				pieces.insert(std::make_pair(id, piece_file_info(id, iter->path().string(), size)));
			}
		}

	}
	for (std::map<boost::uint32_t, piece_file_info>::iterator iter = pieces.begin();
		iter != pieces.end(); ++iter)
	{
		piece_file_queue_.push_back(iter->second);
	}

	// Remove piece.
	while (!piece_file_queue_.empty()
		&& (int)(now_id - piece_file_queue_.front().id_) - (int)piece_scale_ > (int)piece_life_span_)
	{
		try
		{
			boost::filesystem::remove(piece_file_queue_.front().path_);
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
		}
		piece_file_queue_.pop_front();
	}
	if (!piece_file_queue_.empty())
	{
		current_piece_id_ = piece_file_queue_.back().id_ + piece_scale_;
	}

	// Add absent pieces.
// 	assert(now_id >=  current_piece_id_);
// 	while (current_piece_id_ > 0 && (int)(now_id - current_piece_id_) / (int)piece_scale_ > 0)
// 	{
// 		WriteOneSpecialPiece();
// 	}

	// Run.
	clock_.start();
	work_.reset(new boost::asio::io_service::work(ios_));
	ios_.run();
}

void CFLVPieceWriter::HandleWritePiece( const piece_ptr& piece, 
									   const boost::uint32_t& seek_point_pos, const boost::uint32_t& media_ts)
{
	CPieceWriter::HandleWritePiece(piece, seek_point_pos, media_ts);

	boost::uint32_t piece_length(0);
	int error_value;
	std::size_t removed_size(0);
	do 
	{
		error_value = 0;
		switch (stat_)
		{
		case STAT_FIRST_PIECE:
			{
				header_piece_ = piece;
				for (int i = header_piece_->size() - 1; i >= 0; --i)
					piece_length += (*header_piece_)[i]->size_;
				header_piece_len_ = piece_length;
				stat_ = STAT_SECOND_PIECE;
			}
			return;
		case STAT_SECOND_PIECE:
			{
				boost::uint32_t now = GetUTC();
				if (current_piece_id_ > now)
				{ // now-id is already exist.
					get_logger().printf(lilogger::kLevelAlarm, ">Existent[%d] <", now);
					return;
				}

				if (current_piece_id_ > 0)
				{
					while ((int)(GetUTC() - current_piece_id_) / (int)piece_scale_ > 0)
					{	// Padding empty pieces.
						WriteOneSpecialPiece();
					}
				}
				else
					current_piece_id_ = GetUTC() / piece_scale_ * piece_scale_;
		
				stat_ = STAT_NORMAL;
			}		
		case STAT_NORMAL:
			{
				boost::uint32_t now = GetUTC();
				if (current_piece_id_ > now + piece_scale_)
				{ // now-id is already exist.
					get_logger().printf(lilogger::kLevelAlarm, ">Existent[%d] <", now);
					return;
				}

				if (piece_scale_ > 3)
				{
					if (NeedCheckID() && (int)(now - current_piece_id_) / (int)piece_scale_ > 0)
					{
						while ((int)(GetUTC() - current_piece_id_) / (int)piece_scale_ > 0)
						{	// Padding empty pieces.
							WriteOneSpecialPiece(1);
						}
					}
				}
				else
				{
					if (NeedCheckID() && (int)(now - current_piece_id_) / (int)piece_scale_ > 2)
					{
						while ((int)(GetUTC() - current_piece_id_) / (int)piece_scale_ > 0)
						{	// Padding empty pieces.
							WriteOneSpecialPiece(1);
						}
					}
				}
			
				// Calc Piece header values.
				piece_length = header_piece_len_;
				for (int i = piece->size() - 1; i >= 0; --i)
					piece_length += (*piece)[i]->size_;
				if (NeedCheckPointTag() && checkpoint_tag_)
					piece_length += checkpoint_tag_->size_;

				do {
					do {
						// Write file
						std::stringstream file_name;
						file_name << current_piece_id_ << ".flv.tmp";
						file_path_ /= file_name.str();
						std::string file_path_str = file_path_.string();
						std::ofstream piece_file(file_path_str.c_str(), std::ios::binary);
						if (!piece_file)
						{
							error_value = 1;
							break;
						}

						BinaryOArchive<> os(piece_file);
						os << framework::container::make_array(&(header_piece_->front()), header_piece_->size());
						if (NeedCheckPointTag() && checkpoint_tag_)
						{
							boost::uint64_t* check_id = (boost::uint64_t*)(checkpoint_tag_->buf_.get() + CheckPointOffset);
							double db_check_id = current_piece_id_;
							*check_id = framework::system::BytesOrder::host_to_big_endian_longlong(*(boost::uint64_t*)&db_check_id);
							os << checkpoint_tag_.get();
						}	
						os << framework::container::make_array(&(piece->front()), piece->size());

						if (!os)
						{
							error_value = 2;
							break;
						}
						piece_file.close();

						// Rename file
						try
						{ 
							// xxx.flv
							file_path_str = file_path_.string();
							file_path_.replace_extension("");
							if (boost::filesystem::exists(file_path_))
								boost::filesystem::remove(file_path_);
							boost::filesystem::rename(file_path_str, file_path_);

						}
						catch (const boost::filesystem::filesystem_error& e)
						{
							get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
#ifdef WIN32
							switch (e.code().value())
							{
							case ERROR_ALREADY_EXISTS:
								break;
							default:
								break;
							}
#endif // WIN32
							error_value = e.code().value();
						}

					} while (false);

					if (error_value != 0)
					{
						// Maybe disk is full, remove some piece.
						if (removed_size < (2 * piece_length))
						{
							DecreasePiece(piece_length);
							removed_size += piece_length;
						}
						else
						{ // Removed enough, make a break.
#ifdef WIN32
							::Sleep(1000);
#else
							sleep(1);
#endif
						}
					}
					else
						break; // Goooood.

				} while (true);

				get_logger().printf(lilogger::kLevelEvent, "Written [%d.flv], size [%d]", current_piece_id_, piece_length);

				piece_file_queue_.push_back(piece_file_info(current_piece_id_, file_path_.string(), piece_length));
				file_path_.remove_filename();
				current_piece_id_ += piece_scale_;
			}
			break;
		default:
			assert(0);
		}

	} while (error_value);

}

void CFLVPieceWriter::WriteOneSpecialPiece( int special_type /*= 0*/ )
{
	assert(special_type <= 1);
	int error_value(0);
	std::size_t removed_size, piece_length;

	if (special_type <= 1 && header_piece_)
	{
		piece_length = header_piece_len_;
		if (special_type == 0 && offair_tag_)
			piece_length += offair_tag_->size_;
		if (NeedCheckPointTag() && checkpoint_tag_)
			piece_length += checkpoint_tag_->size_;

		do{
			// Write file
			std::stringstream file_name;
			file_name << current_piece_id_ << ".flv.tmp";
			file_path_ /= file_name.str();
			std::string file_path_str = file_path_.string();
			std::ofstream piece_file(file_path_str.c_str(), std::ios::binary);
			if (!piece_file)
			{
				error_value = 1;
				break;
			}

			BinaryOArchive<> os(piece_file);
			// flv header
			os << framework::container::make_array(&(header_piece_->front()), header_piece_->size());
			// Pedding checkpoint-tag
			if (NeedCheckPointTag() && checkpoint_tag_)
			{
				boost::uint64_t* check_id = (boost::uint64_t*)(checkpoint_tag_->buf_.get() + CheckPointOffset);
				double db_check_id = current_piece_id_;
				*check_id = framework::system::BytesOrder::host_to_big_endian_longlong(*(boost::uint64_t*)&db_check_id);
				os << checkpoint_tag_.get();
			}
			// Pedding offair-tag
			if (special_type == 0 && offair_tag_)
			{
				os << offair_tag_;
			}

			if (!os)
			{
				error_value = 2;
				break;
			}

			piece_file.close();

			// Rename file
			file_path_.replace_extension("");
			try
			{
				if (boost::filesystem::exists(file_path_))
					boost::filesystem::remove(file_path_);
				boost::filesystem::rename(file_path_str, file_path_);
			}
			catch (const boost::filesystem::filesystem_error& e)
			{
				get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
#ifdef WIN32
				switch (e.code().value())
				{
				case ERROR_ALREADY_EXISTS:
					break;
				default:
					break;
				}
#endif // WIN32
				error_value = e.code().value();
			}

			if (error_value != 0)
			{
				// Maybe disk is full, remove some piece.
				if (removed_size < (2 * piece_length))
				{
					DecreasePiece(piece_length);
					removed_size += piece_length;
				}
				else
				{ // Removed enough, make a break.
#ifdef WIN32
					::Sleep(1000);
#else
					sleep(1);
#endif
				}
			}
			else
				break; // Success.

		} while(true);

		get_logger().printf(lilogger::kLevelEvent, "Written [%d.flv], size [%d]", current_piece_id_, piece_length);
		piece_file_queue_.push_back(piece_file_info(current_piece_id_, file_path_.string(), piece_length));
		file_path_.remove_filename();
		current_piece_id_ += piece_scale_;
	}

}



//////////////////////////////////////////////////////////////////////////
// Class CPPLBlockWriter

CPPLBlockWriter::CPPLBlockWriter( const std::string& path, const boost::uint32_t& piece_scale, const boost::uint32_t& piece_life_span,
								 const std::string& livecms_host, const boost::uint32_t& status_report_period) 
: CPieceWriter(path, piece_scale, ((2 * DIR_SCALE) > piece_life_span ? (2 * DIR_SCALE) : piece_life_span),
			   livecms_host, status_report_period)
{
	framework::string::Uuid rid;
//	guid_str_ = file_path_.filename();
	rid.from_string(guid_str_);
	memcpy(block_header_.RID, rid.to_bytes().data(), 16);
}

/*
void CPPLBlockWriter::Init(boost::uint32_t& media_ts)
{
	boost::filesystem::directory_iterator end_itr;
	boost::uint32_t id;
	std::size_t size;
	std::map<boost::uint32_t, piece_file_info> pieces;
	boost::uint32_t now_id = GetUTC();

	// Load special-tags.
	if (boost::filesystem::exists("c:\\onair.tag") 
		&& boost::filesystem::file_size("c:\\onair.tag") > 0)
	{
		std::ifstream tag_file("c:\\onair.tag", std::ios::binary);
		if (tag_file)
		{
			tag_file.seekg (0, std::ios::end);
			offair_tag_.reset(new lite_buffer(tag_file.tellg()));
			tag_file.seekg (0, std::ios::beg);
			BinaryIArchive<> is(tag_file);
			while(is)
			{
				is >> offair_tag_;
			}
			tag_file.close();
		}
	}
	else
	{
		offair_tag_.reset(new lite_buffer(sizeof(OffairTagData)));
		memcpy(offair_tag_->buf_.get(), OffairTagData, sizeof(OffairTagData));
	}

	checkpoint_tag_.reset(new lite_buffer(sizeof(CheckPointTagData)));
	memcpy(checkpoint_tag_->buf_.get(), CheckPointTagData, sizeof(CheckPointTagData));

	header_piece_len_ = 13;
	liter_buffer_ptr flv_header(new lite_buffer(header_piece_len_));
	header_piece_.reset(new std::vector<liter_buffer_ptr>());
	memcpy(flv_header->buf_.get(), FlvHeader, header_piece_len_);
	header_piece_->push_back(flv_header);

	onmetadata_tag_.reset(new lite_buffer(sizeof(OnMetaData)));
	memcpy(onmetadata_tag_->buf_.get(), OnMetaData, sizeof(OnMetaData));

	videoheader_tag_.reset(new lite_buffer(sizeof(VideoHeaderTag)));
	memcpy(videoheader_tag_->buf_.get(), VideoHeaderTag, sizeof(VideoHeaderTag));

	audioheader_tag_.reset(new lite_buffer(sizeof(AudioHeaderTag)));
	memcpy(audioheader_tag_->buf_.get(), AudioHeaderTag, sizeof(AudioHeaderTag));
	// 
	// 	std::ofstream tag2_file("c:\\onair2.tag", std::ios::binary);
	// 	BinaryOArchive<> os(tag2_file);
	// 	os << offair_tag_buffer.get();
	// 	tag2_file.close();

	// Scan dirs.
	dir_path_ = file_path_;
	file_path_="";
	std::map<boost::uint32_t, dir_info> dirs;
	try
	{
		for (boost::filesystem::directory_iterator iter(dir_path_); iter != end_itr; ++iter)
		{
			if ( boost::filesystem::is_directory(iter->status()) )
			{
				id = atoi(iter->path().stem().c_str());
				//size = (size_t)boost::filesystem::file_size(iter->path());
				if (id != 0)//&& size != 0)
				{
					dirs.insert(std::make_pair(id, dir_info(id, iter->path().string())));
				}
			}
		}

	}
	catch (const boost::filesystem::filesystem_error& e)
	{
		get_logger().printf(lilogger::kLevelError, "Scan dirs exception: %s", e.what());
	}

	for (std::map<boost::uint32_t, dir_info>::iterator iter = dirs.begin();
	iter != dirs.end(); ++iter)
	{
		dir_queue_.push(iter->second);
	}
	while (!dir_queue_.empty()
		&& (int)(now_id - dir_queue_.front().id_ * DIR_SCALE) > (int)piece_life_span_)
	{
		try
		{
			boost::filesystem::remove_all(dir_queue_.front().path_);
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Remove dir exception: %s", e.what());
		}
		dir_queue_.pop();
	}
	if (!dir_queue_.empty())
	{
		file_path_ = dir_queue_.back().path_;
// 		current_piece_id_ = current_piece_id_ / piece_scale_ * piece_scale_;
// 
// 		media_ts = GetLastTsInPiece(piece_file_queue_.back().path_);
// 		cur_media_ts_ = media_ts;
	}
// 	else
// 	{
// 		file_path_ = dir
// 	}

	if (file_path_ != "")
	{
		try
		{
			// Remove xxx.flv.tmp
			for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
			{
				if ( !boost::filesystem::is_directory(iter->status()) 
					&& iter->path().extension() == std::string(".tmp"))
				{
					boost::filesystem::remove(iter->path());
				}
			}

			// Scan pieces from disk.
			for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
			{
				size = 0;
				if ( !boost::filesystem::is_directory(iter->status()) 
					&& iter->path().extension() == std::string(".block"))
				{
					id = atoi(iter->path().stem().c_str());
					size = (size_t)boost::filesystem::file_size(iter->path());
					if (id != 0 && size != 0)
					{
						pieces.insert(std::make_pair(id, piece_file_info(id, iter->path().string(), size)));
					}
				}

			}
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
		}
		for (std::map<boost::uint32_t, piece_file_info>::iterator iter = pieces.begin();
			iter != pieces.end(); ++iter)
		{
			piece_file_queue_.push_back(iter->second);
		}

		// Remove piece.
		while (!piece_file_queue_.empty()
			&& (int)(now_id - piece_file_queue_.front().id_) - (int)piece_scale_ > (int)piece_life_span_)
		{
			try
			{
				boost::filesystem::remove(piece_file_queue_.front().path_);
			}
			catch (const boost::filesystem::filesystem_error& e)
			{
				get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
			}
			piece_file_queue_.pop_front();
		}
		if (!piece_file_queue_.empty())
		{
			current_piece_id_ = piece_file_queue_.back().id_ + piece_scale_;
			current_piece_id_ = current_piece_id_ / piece_scale_ * piece_scale_;

			media_ts = GetLastTsInPiece(piece_file_queue_.back().path_);
			cur_media_ts_ = media_ts;
		}
	}

	// Add absent pieces.
	//	assert(now_id >=  current_piece_id_);
	while (current_piece_id_ > 0 && (int)(now_id - current_piece_id_) / (int)piece_scale_ > 0)
	{
		WriteOneSpecialPiece();
	}
}

*/
void CPPLBlockWriter::Init(boost::uint32_t& media_ts)
{
	boost::filesystem::directory_iterator end_itr;
	std::size_t size;
	std::map<boost::uint32_t, piece_file_info> pieces;
	boost::uint32_t now_id = GetUTC();

	// Load special-tags.
	if (boost::filesystem::exists("c:\\onair.tag") 
		&& boost::filesystem::file_size("c:\\onair.tag") > 0)
	{
		std::ifstream tag_file("c:\\onair.tag", std::ios::binary);
		if (tag_file)
		{
			tag_file.seekg (0, std::ios::end);
			offair_tag_.reset(new lite_buffer(tag_file.tellg()));
			tag_file.seekg (0, std::ios::beg);
			BinaryIArchive<> is(tag_file);
			while(is)
			{
				is >> offair_tag_;
			}
			tag_file.close();
		}
	}
	else
	{
		offair_tag_.reset(new lite_buffer(sizeof(OffairTagData)));
		memcpy(offair_tag_->buf_.get(), OffairTagData, sizeof(OffairTagData));
	}

	checkpoint_tag_.reset(new lite_buffer(sizeof(CheckPointTagData)));
	memcpy(checkpoint_tag_->buf_.get(), CheckPointTagData, sizeof(CheckPointTagData));

	header_piece_len_ = 13;
	liter_buffer_ptr flv_header(new lite_buffer(header_piece_len_));
	header_piece_.reset(new std::vector<liter_buffer_ptr>());
	memcpy(flv_header->buf_.get(), FlvHeader, header_piece_len_);
	header_piece_->push_back(flv_header);

	onmetadata_tag_.reset(new lite_buffer(sizeof(OnMetaData)));
	memcpy(onmetadata_tag_->buf_.get(), OnMetaData, sizeof(OnMetaData));

	videoheader_tag_.reset(new lite_buffer(sizeof(VideoHeaderTag)));
	memcpy(videoheader_tag_->buf_.get(), VideoHeaderTag, sizeof(VideoHeaderTag));

	audioheader_tag_.reset(new lite_buffer(sizeof(AudioHeaderTag)));
	memcpy(audioheader_tag_->buf_.get(), AudioHeaderTag, sizeof(AudioHeaderTag));
	// 
	// 	std::ofstream tag2_file("c:\\onair2.tag", std::ios::binary);
	// 	BinaryOArchive<> os(tag2_file);
	// 	os << offair_tag_buffer.get();
	// 	tag2_file.close();

	// Scan dirs.
	dir_path_ = file_path_.remove_filename();
	file_path_="";
	std::map<boost::uint32_t, dir_info> dirs;
	boost::filesystem::path file_dir;
	boost::uint32_t id;
	try
	{
		for (boost::filesystem::directory_iterator iter(dir_path_); iter != end_itr; ++iter)
		{
			if ( boost::filesystem::is_directory(iter->status()) )
			{
				id = atoi(iter->path().stem().c_str());
				file_dir = iter->path();
				file_dir /= guid_str_;
				if (id != 0
					&& boost::filesystem::exists(file_dir))
				{
					dirs.insert(std::make_pair(id, dir_info(id, file_dir.string())));
				}
			}
		}

	}
	catch (const boost::filesystem::filesystem_error& e)
	{
		get_logger().printf(lilogger::kLevelError, "Scan dirs exception: %s", e.what());
	}

	for (std::map<boost::uint32_t, dir_info>::iterator iter = dirs.begin();
		iter != dirs.end(); ++iter)
	{
		dir_queue_.push(iter->second); //Tady, 08132012: Actually, Dir-queue was useless.
	}
	
	if (!dir_queue_.empty())
	{
		file_path_ = dir_queue_.back().path_;
	}

	if (file_path_ != "")
	{
		try
		{
			// Remove xxx.flv.tmp
			for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
			{
				if ( !boost::filesystem::is_directory(iter->status()) 
					&& iter->path().extension() == std::string(".tmp"))
				{
					boost::filesystem::remove(iter->path());
				}
			}

			// Scan pieces from disk.
			for (boost::filesystem::directory_iterator iter(file_path_); iter != end_itr; ++iter)
			{
				size = 0;
				if ( !boost::filesystem::is_directory(iter->status()) 
					&& iter->path().extension() == std::string(".block"))
				{
					id = atoi(iter->path().stem().c_str());
					size = (size_t)boost::filesystem::file_size(iter->path());
					if (id != 0 && size != 0)
					{
						pieces.insert(std::make_pair(id, piece_file_info(id, iter->path().string(), size)));
					}
				}

			}
		}
		catch (const boost::filesystem::filesystem_error& e)
		{
			get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
		}
		for (std::map<boost::uint32_t, piece_file_info>::iterator iter = pieces.begin();
			iter != pieces.end(); ++iter)
		{
			piece_file_queue_.push_back(iter->second);
		}

		// Remove piece.
		while (!piece_file_queue_.empty()
			&& (int)(now_id - piece_file_queue_.front().id_) - (int)piece_scale_ > (int)piece_life_span_)
		{
			try
			{
				boost::filesystem::remove(piece_file_queue_.front().path_);
			}
			catch (const boost::filesystem::filesystem_error& e)
			{
				get_logger().printf(lilogger::kLevelError, "Excpetion: %s", e.what());
			}
			piece_file_queue_.pop_front();
		}
		if (!piece_file_queue_.empty())
		{
			current_piece_id_ = piece_file_queue_.back().id_ + piece_scale_;
			current_piece_id_ = current_piece_id_ / piece_scale_ * piece_scale_;

			media_ts = GetLastTsInPiece(piece_file_queue_.back().path_);
			cur_media_ts_ = media_ts;
		}
	}

	// Add absent pieces.
	//	assert(now_id >=  current_piece_id_);
	while (current_piece_id_ > 0 && (int)(now_id - current_piece_id_) / (int)piece_scale_ > 0)
	{
		WriteOneSpecialPiece();
	}
}

void CPPLBlockWriter::Run()
{	
	// Run.
	clock_.start();
	work_.reset(new boost::asio::io_service::work(ios_));
	ios_.run();
}

void CPPLBlockWriter::HandleWritePiece( const piece_ptr& piece, 
									   const boost::uint32_t& seek_point_pos, const boost::uint32_t& media_ts)
{
	CPieceWriter::HandleWritePiece(piece, seek_point_pos, media_ts);

	boost::uint32_t piece_length(0);
	int error_value;
	std::size_t removed_size(0);
	do 
	{
		error_value = 0;
		switch (stat_)
		{
		case STAT_FIRST_PIECE:
			{
				header_piece_ = piece;
				for (int i = header_piece_->size() - 1; i >= 0; --i)
					piece_length += (*header_piece_)[i]->size_;
				header_piece_len_ = piece_length;
				stat_ = STAT_SECOND_PIECE;
			}
			return;
		case STAT_SECOND_PIECE:
			{
				boost::uint32_t now = GetUTC();
				if (current_piece_id_ > now)
				{ // now-id is already exist.
					get_logger().printf(lilogger::kLevelAlarm, ">Existent[%d] < ", now);
					return;
				}

				if (current_piece_id_ > 0)
				{
					while ((int)(GetUTC() - current_piece_id_) / (int)piece_scale_ > 0)
					{	// Padding empty pieces.
						WriteOneSpecialPiece();
					}
				}
				else
				{
					current_piece_id_ = GetUTC() / piece_scale_ * piece_scale_;
				}

				stat_ = STAT_NORMAL;
			}		
		case STAT_NORMAL:
			{
				boost::uint32_t now = GetUTC();
				if (current_piece_id_ > now + piece_scale_)
				{ // now-id is already exist.
					get_logger().printf(lilogger::kLevelAlarm, ">Existent[%d] < ", now);
					return;
				}

				if (piece_scale_ > 3)
				{
					if (NeedCheckID() && (int)(now - current_piece_id_) / (int)piece_scale_ > 0)
					{
						while ((int)(GetUTC() - current_piece_id_) / (int)piece_scale_ > 0)
						{	// Padding empty pieces.
							WriteOneSpecialPiece(1);
						}
					}
				}
				else
				{
					if (NeedCheckID() && (int)(now - current_piece_id_) / (int)piece_scale_ > 2)
					{
						while ((int)(GetUTC() - current_piece_id_) / (int)piece_scale_ > 0)
						{	// Padding empty pieces.
							WriteOneSpecialPiece(1);
						}
					}
				}

				// Calc Piece header values.
				std::vector<liter_buffer_ptr> data_collection;
				// FLV header
				data_collection = *header_piece_;
				piece_length = header_piece_len_;
				// CuePoint
				if (NeedCheckPointTag() && checkpoint_tag_)
				{
					boost::uint64_t* check_id = (boost::uint64_t*)(checkpoint_tag_->buf_.get() + CheckPointOffset);
					double db_check_id = current_piece_id_;
					*check_id = framework::system::BytesOrder::host_to_big_endian_longlong(*(boost::uint64_t*)&db_check_id);

					data_collection.push_back(checkpoint_tag_);
					piece_length += checkpoint_tag_->size_;

				}
				// FLV tags
				data_collection.insert(data_collection.end(), piece->begin(), piece->end());
				for (int i = piece->size() - 1; i >= 0; --i)
					piece_length += (*piece)[i]->size_;

				// Assign values of block_header.
				{	
					block_header_.BLOCK_HEADER_LENGTH = FLV_BLOCK_ROOT_HEADER_T_SIZE
						+ 4 * (piece_length / FLV_BLOCK_SIZE + bool(piece_length % FLV_BLOCK_SIZE));
					block_header_.DATA_LENGTH = piece_length;
					block_header_.BLOCK_ID = current_piece_id_;

					// Calc check-sum value of piece.
					piece_len_ = 0;
					boost::uint32_t copy_len, data_len, src_offset(0);
					boost::uint32_t* check_sum_val = (boost::uint32_t*)(block_header_.CHECKSUM_LIST);
					for (int i(0); i < (int)data_collection.size(); ++i)
					{
						data_len = data_collection[i]->size_;
						src_offset = 0;

						while(src_offset != data_len)
						{
							copy_len = FLV_BLOCK_SIZE - piece_len_;
							if (copy_len > data_len - src_offset) 
								copy_len = data_len - src_offset;

							memcpy(piece_buf_ + piece_len_, data_collection[i]->buf_.get() + src_offset, copy_len);
							piece_len_ += copy_len;
							src_offset += copy_len;

							if (piece_len_ == FLV_BLOCK_SIZE)
							{
								*check_sum_val = check_sum_new(boost::asio::buffer(piece_buf_, piece_len_));
								++check_sum_val;

								piece_len_ = 0;
							}
						}
							
					}
					if (piece_len_ != 0)
					{
						*check_sum_val = check_sum_new(boost::asio::buffer(piece_buf_, piece_len_));
					}

					// Calc MD5 of block_header.
					framework::string::Md5 block_header_md5;
					block_header_md5.update((boost::uint8_t*)(&block_header_) + 16, block_header_.BLOCK_HEADER_LENGTH -  16);
					block_header_md5.final();
					memcpy(block_header_.HASH_VALUE, block_header_md5.to_bytes().data(), 16);

					piece_length += FLV_BLOCK_HEADER_T_SIZE;
				}

				// Write file
				std::string file_path_str;
				do {
					do {
// 						std::stringstream file_name;
// 						file_name << current_piece_id_ << ".block.tmp";
// 						file_path_ /= file_name.str();
// 						std::string file_path_str = file_path_.string();

						file_path_str = GetFilePathName();
						std::string file_path_tmp = file_path_str + ".tmp";
						std::ofstream piece_file(file_path_tmp.c_str(), std::ios::binary);
						if (!piece_file)
						{
							error_value = 1;
							break;
						}

						BinaryOArchive<> os(piece_file);
						// Block header.
						os << block_header_;
						// flv
// 						os << framework::container::make_array(&(header_piece_->front()), header_piece_->size());
// 						if (NeedCheckPointTag() && checkpoint_tag_)
// 						{
// 							os << checkpoint_tag_.get();
// 						}	
						os << framework::container::make_array(&(data_collection.front()), data_collection.size());

						if (!os)
						{
							error_value = 2;
							piece_file.close();
							get_logger().printf(lilogger::kLevelError, "File Writing Error: %d", error_value);
							break;
						}
						piece_file.close();

						// Rename file
						try
						{ 
							// xxx.piece
// 							file_path_str = file_path_.string();
// 							file_path_.replace_extension("");
							if (boost::filesystem::exists(file_path_str))
								boost::filesystem::remove(file_path_str);
							boost::filesystem::rename(file_path_tmp, file_path_str);

						}
						catch (const boost::filesystem::filesystem_error& e)
						{
							get_logger().printf(lilogger::kLevelError, "File Renaming Excpetion: %s", e.what());
#ifdef WIN32
							switch (e.code().value())
							{
							case ERROR_ALREADY_EXISTS:
								break;
							default:
								break;
							}
#endif // WIN32							
							error_value = e.code().value();
						}

					} while (false);

					if (error_value != 0)
					{
						// Maybe disk is full, remove some piece.
						if (removed_size < (2 * piece_length))
						{
							DecreasePiece(piece_length);
							removed_size += piece_length;
						}
						else
						{ // Removed enough, make a break.
#ifdef WIN32
							::Sleep(1000);
#else
							sleep(1);
#endif
						}
					}
					else
						break; // Goooood.

				} while (true);

				get_logger().printf(lilogger::kLevelEvent, "Written [%d.block], size [%d]", current_piece_id_, piece_length);

				piece_file_queue_.push_back(piece_file_info(current_piece_id_, file_path_str, piece_length));
//				file_path_.remove_filename();
				current_piece_id_ += piece_scale_;
			}
			break;
		default:
			assert(0);
		}

	} while (error_value);

}

void CPPLBlockWriter::WriteOneSpecialPiece( int special_type /*= 0*/ )
{
	assert(special_type <= 1);
	int error_value(0);
	std::size_t removed_size, piece_length(0);

	if (special_type <= 1/* && header_piece_*/)
	{
		// Calc Piece header values.
		std::vector<liter_buffer_ptr> data_collection;
		// FLV header
// 		data_collection = *header_piece_;
 		piece_length = FLV_FILEHEADER_T_SIZE;
		liter_buffer_ptr flv_header(new lite_buffer(piece_length));
		memcpy(flv_header->buf_.get(), FlvHeader, piece_length);
		data_collection.push_back(flv_header);

		// Add cue points.
		if (NeedCheckPointTag() && checkpoint_tag_)
		{
			boost::uint64_t* check_id = (boost::uint64_t*)(checkpoint_tag_->buf_.get() + CheckPointOffset);
			double db_check_id = current_piece_id_;
			*check_id = framework::system::BytesOrder::host_to_big_endian_longlong(*(boost::uint64_t*)&db_check_id);

			data_collection.push_back(checkpoint_tag_);
			piece_length += checkpoint_tag_->size_;

		}

		if (special_type == 0)
		{
			UINT32 ts = cur_media_ts_;
			if (offair_tag_)
			{
// 				printf("Writing cur_media_ts_ < %d > \n", cur_media_ts_);
// 				FLVTagHeader_t::set_tag_ts((FLVTagHeader_t *)offair_tag_->buf_.get(), cur_media_ts_);
//				flv_util::change_tags_ts(offair_tag_->buf_.get(), offair_tag_->size_, ts);
				data_collection.push_back(offair_tag_);
				piece_length += offair_tag_->size_;
			}
/*
			if (onmetadata_tag_)
			{
				data_collection.push_back(onmetadata_tag_);
				piece_length += onmetadata_tag_->size_;
			}

			if (videoheader_tag_)
			{
				FLVTagHeader_t::set_tag_ts((FLVTagHeader_t *)videoheader_tag_->buf_.get(), cur_media_ts_ + 10);
				data_collection.push_back(videoheader_tag_);
				piece_length += videoheader_tag_->size_;
			}
			if (audioheader_tag_)
			{
				FLVTagHeader_t::set_tag_ts((FLVTagHeader_t *)audioheader_tag_->buf_.get(), cur_media_ts_ + 20);
				data_collection.push_back(audioheader_tag_);
				piece_length += audioheader_tag_->size_;
			}
*/
			cur_media_ts_ += piece_scale_ * 1000;
			cur_media_ts_ = cur_media_ts_ >= ts ? cur_media_ts_ : ts;
		}

		// Assign values of block_header.
		{	
			block_header_.BLOCK_HEADER_LENGTH = FLV_BLOCK_ROOT_HEADER_T_SIZE
				+ 4 * (piece_length / FLV_BLOCK_SIZE + bool(piece_length % FLV_BLOCK_SIZE));
			block_header_.DATA_LENGTH = piece_length;
			block_header_.BLOCK_ID = current_piece_id_;

			// Calc check-sum value of piece.
			piece_len_ = 0;
			boost::uint32_t copy_len, data_len, src_offset(0);
			boost::uint32_t* check_sum_val = (boost::uint32_t*)(block_header_.CHECKSUM_LIST);
			for (int i(0); i < (int)data_collection.size(); ++i)
			{
				data_len = data_collection[i]->size_;
				src_offset = 0;

				while(src_offset != data_len)
				{
					copy_len = FLV_BLOCK_SIZE - piece_len_;
					if (copy_len > data_len - src_offset) 
						copy_len = data_len - src_offset;

					memcpy(piece_buf_ + piece_len_, data_collection[i]->buf_.get() + src_offset, copy_len);
					piece_len_ += copy_len;
					src_offset += copy_len;

					if (piece_len_ == FLV_BLOCK_SIZE)
					{
						*check_sum_val = check_sum_new(boost::asio::buffer(piece_buf_, piece_len_));
						++check_sum_val;

						piece_len_ = 0;
					}
				}

			}
			if (piece_len_ != 0)
			{
				*check_sum_val = check_sum_new(boost::asio::buffer(piece_buf_, piece_len_));
			}

			// Calc MD5 of block_header.
			framework::string::Md5 block_header_md5;
			block_header_md5.update((boost::uint8_t*)(&block_header_) + 16, block_header_.BLOCK_HEADER_LENGTH -  16);
			block_header_md5.final();
			memcpy(block_header_.HASH_VALUE, block_header_md5.to_bytes().data(), 16);

			piece_length += FLV_BLOCK_HEADER_T_SIZE;
		}

		// Write file
		std::string file_path_str;
		do{
			do 
			{
// 				std::stringstream file_name;
// 				file_name << current_piece_id_ << ".block.tmp";
// 				file_path_ /= file_name.str();
// 				std::string file_path_str = file_path_.string();
				file_path_str = GetFilePathName();
				std::string file_path_tmp = file_path_str + ".tmp";
				std::ofstream piece_file(file_path_tmp.c_str(), std::ios::binary);
				if (!piece_file)
				{
					error_value = 1;
					break;
				}

				BinaryOArchive<> os(piece_file);
				// block header
				os << block_header_;
				// flv 
				os << framework::container::make_array(&(data_collection.front()), data_collection.size());
				// Pedding checkpoint-tag
	// 			if (NeedCheckPointTag() && checkpoint_tag_)
	// 			{
	// 				os << checkpoint_tag_.get();
	// 			}
	// 			// Pedding offair-tag
	// 			if (special_type == 0 && offair_tag_)
	// 			{
	// 				os << offair_tag_;
	// 			}

				if (!os)
				{
					error_value = 2;
					piece_file.close();
					get_logger().printf(lilogger::kLevelError, "File Writing Error: %d", error_value);
					break;
				}
				piece_file.close();

				// Rename file
//				file_path_.replace_extension("");
				try
				{
					if (boost::filesystem::exists(file_path_str))
						boost::filesystem::remove(file_path_str);
					boost::filesystem::rename(file_path_tmp, file_path_str);
				}
				catch (const boost::filesystem::filesystem_error& e)
				{
					get_logger().printf(lilogger::kLevelError, "File Renaming Excpetion: %s", e.what());
	#ifdef WIN32
					switch (e.code().value())
					{
					case ERROR_ALREADY_EXISTS:
						break;
					default:
						break;
					}
	#endif // WIN32			
					error_value = e.code().value();
				}
			} while (false);

			if (error_value != 0)
			{
				get_logger().printf(lilogger::kLevelAlarm, "Write failed(%d). [%d.block], size [%d]", error_value, current_piece_id_, piece_length);
				// Maybe disk is full, remove some piece.
				if (removed_size < (2 * piece_length))
				{
					DecreasePiece(piece_length);
					removed_size += piece_length;
				}
				else
				{ // Removed enough, make a break.
#ifdef WIN32
					::Sleep(1000);
#else
					sleep(1);
#endif
				}
			}
			else
				break; // Success.

		} while(true);

		get_logger().printf(lilogger::kLevelEvent, "Written [%d.block], size [%d]", current_piece_id_, piece_length);
		piece_file_queue_.push_back(piece_file_info(current_piece_id_, file_path_str, piece_length));
		//file_path_.remove_filename();
		current_piece_id_ += piece_scale_;
	}

}

void CPPLBlockWriter::OnClockAlarm()
{
	boost::uint32_t now_id = GetUTC();

	// Remove dir.
	while (!dir_queue_.empty()
		&& ((int)now_id - (int)(dir_queue_.front().id_ * DIR_SCALE)) > (int)piece_life_span_)
	{
		// Removed by Tady, 08032012: All removing work left to Cutterprocessor.
// 		try
// 		{
// 			boost::filesystem::remove_all(dir_queue_.front().path_);
// 		}
// 		catch (const boost::filesystem::filesystem_error& e)
// 		{
// 			get_logger().printf(lilogger::kLevelError, "Remove dir exception: %s", e.what());
// 		}
		dir_queue_.pop();
	}

	CPieceWriter::OnClockAlarm();
}

void CPPLBlockWriter::HandleUpdateHeader( const piece_ptr& header_tag)
{
	assert(header_tag->size() == 2);
	FLVTagHeader_t* flvTagHeader = (FLVTagHeader_t*)header_tag->front()->buf_.get();
	FLVTagHeader_t::set_tag_ts(flvTagHeader, 0);
	int type = flvTagHeader->type;
	boost::uint32_t add_size =  header_tag->front()->size_ + header_tag->back()->size_;
	int remove_size = 0;

	for(std::vector<liter_buffer_ptr>::iterator iter = ++(header_piece_->begin()); iter != header_piece_->end(); iter += 2)
	{
		FLVTagHeader_t* flvTagHeader = (FLVTagHeader_t*)((*iter)->buf_.get());
		if ((int)(flvTagHeader->type) == type)
		{
			remove_size = (*iter)->size_;
			(*iter) = header_tag->front();
			++iter;			
			remove_size += (*iter)->size_;
			(*iter) = header_tag->back();

			//header_piece_->insert(header_piece_->end(), header_tag->begin(), header_tag->end());
			assert(header_piece_len_ > remove_size);
			header_piece_len_ -= remove_size;
			header_piece_len_ += add_size;
			break;
		}
	}
	if (remove_size == 0)
	{
		header_piece_->insert(header_piece_->end(), header_tag->begin(), header_tag->end());
		header_piece_len_ += add_size;
	}
}

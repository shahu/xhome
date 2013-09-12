#pragma once

// #include "util/Util.h"
// #include <boost/asio/io_service.hpp>
//#include "util/protocol/http/HttpClient.h"
#include "PieceWriter.h"

namespace util{
	namespace protocol{
		class HttpClient;
}
}

class CStreamReader
{
public:
	CStreamReader(const std::string& url, const std::string& path,
		const boost::uint32_t& piece_type, const bool& cut_with_kf, const boost::uint32_t& piece_scale,
		const boost::uint32_t& piece_life_span, const boost::uint32_t& head_piece_life_span,
		const bool& need_modify_ts = true,
		const std::string& livecms_host = "live-cms.synacast.com", const boost::uint32_t& status_report_period = 60);
	~CStreamReader(void);

	bool Start();
	void Stop();
	
private:
	void on_http_open(const boost::system::error_code& ec);
	void on_content(const boost::system::error_code& ec, const std::size_t& length);
	void async_begin();
	bool do_begin();

	void OnClockAlarm();

	enum {
		STAT_NEED_HEAD,
		STAT_NEED_FLV_DATA
	};
	enum {
		READING_HEAD,
		READING_FLV_TAGHEAD,
		READING_FLV_TAGBODY
	};

	const static int cs_pedding_cut_down_time = 10;
	const static int cs_start_cut_down_time = 3;

	std::string url_;
	boost::shared_ptr<util::protocol::HttpClient> http_client_;
	boost::asio::io_service ios_;
	int stat_;
	int reading_stat_;

	liter_buffer_ptr	reading_buffer_ptr_;
	std::size_t			reading_size_;

	piece_ptr piece_ptr_;
	
	int cur_flv_tag_type_;
	boost::uint32_t	flv_tag_beg_timestamp_;	// Added by Tady, 06262013: The first non-zero timestamp in flv file.
	boost::uint32_t flv_tag_timestamp_; // unit is ms
	boost::uint32_t cur_flv_tag_ts_;	// unit is piece-count
	boost::uint32_t cur_piece_ts_;
	bool got_audio_head_;
	bool got_video_head_;
	// Added by Tady, 06152011: For audio or video only stream.
	bool is_header_over_;

	CPieceWriter* writer_;

	bool got_seek_point_;
	boost::uint32_t seek_point_pos_;

	bool need_cut_with_kf_;
	bool cut_with_kf_;
	boost::uint32_t cut_scale_;	 // Unit is sec.
//	bool can_cut_;
	int	 should_cut_count_;

	framework::timer::AsioTimerManager clock_manager_;
	framework::timer::PeriodicTimer clock_;
	boost::asio::io_service::work work_;
	bool is_reader_ready_;
	int  pedding_cut_down_time_;
	int	 start_cut_down_time_;
	
	// Added by Tady, 06182011: When cut_with_kf is true
	// , the very first piece is usually starting with a non-kf. 
	// So we drop it!
	bool got_first_data_piece_; 

	boost::uint32_t flv_tag_ts_delta_; // unit is ms.
	bool got_first_data_tag_;

	// Added by Tady, 05232012: A switcher to modify time stamp
	bool need_modify_ts_;

	piece_ptr aac_sequence_header_;
	piece_ptr avc_sequence_header_;
};

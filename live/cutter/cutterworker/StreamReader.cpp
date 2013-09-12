#include "stdafx.h"
#include "StreamReader.h"
#include "util/protocol/http/HttpClient.h"
#include <boost/bind.hpp>
// #include "util//binder/Bind.h"
// #include "util/binder/PtrFun.h"
#include <boost/asio/buffer.hpp>
#include "FlvStuff.h"

CStreamReader::CStreamReader(const std::string& url, const std::string& path, 
    const boost::uint32_t& piece_type, const bool& cut_with_kf, const boost::uint32_t& piece_scale, 
	const boost::uint32_t& piece_life_span, const boost::uint32_t& head_piece_life_span,
	const bool& need_modify_ts,
	const std::string& livecms_host, const boost::uint32_t& status_report_period) 
: url_(url), cut_scale_(piece_scale * 1000)
, need_cut_with_kf_(cut_with_kf), cut_with_kf_(cut_with_kf)/*, can_cut_(false)*/
, should_cut_count_(0)
, flv_tag_timestamp_(0)
, clock_manager_(ios_, boost::posix_time::seconds(1))
, clock_(clock_manager_, 1000, boost::bind(&CStreamReader::OnClockAlarm, this))
, work_(ios_)
, need_modify_ts_(need_modify_ts)
{
    if (piece_type == 0)
    {
        writer_ = new CPPLPieceWriter(path, piece_scale, piece_life_span, head_piece_life_span);
    }
    else if (piece_type == 1)
    {
        writer_ = new CFLVPieceWriter(path, piece_scale, piece_life_span);
        writer_->SetCheckpointScale(piece_scale);
    }
    else if (piece_type == 2)
    {
        writer_ = new CPPLBlockWriter(path, piece_scale, piece_life_span, livecms_host, status_report_period);
        writer_->SetCheckpointScale(piece_scale);
		((CPPLBlockWriter*)writer_)->Init(flv_tag_timestamp_);
		cur_piece_ts_ = flv_tag_timestamp_ / cut_scale_;
//		flv_tag_timestamp_ = 0x7ffff000;
    }
    else
    {
        writer_ = NULL;
    }

    clock_manager_.start();
}

CStreamReader::~CStreamReader(void)
{
    clock_manager_.stop();

    if (writer_ != NULL)
    {
        delete writer_;
    }
}

bool CStreamReader::Start()
{
	if (writer_)
	{
		if (!writer_->Start())
			return false; 

// 		if (!do_begin())
// 			return false;
		async_begin();

		clock_.start();
		ios_.run();
		return true;
	}

	return false;
}

void CStreamReader::on_http_open( const boost::system::error_code& ec )
{
	if (!ec 
		&& http_client_->response_head().err_code == util::protocol::http_error::ok)
	{
		is_reader_ready_ = true;
		pedding_cut_down_time_ = cs_pedding_cut_down_time;

		reading_size_ = FLV_FILEHEADER_T_SIZE/* + FLV_TAGHEADER_T_SIZE*/;
		reading_buffer_ptr_.reset(new lite_buffer(reading_size_));
		http_client_->async_receive(boost::asio::buffer(reading_buffer_ptr_->buf_.get(), reading_size_),
			boost::bind(&CStreamReader::on_content, this, _1, _2));
	}
	else
	{ // Restart
		async_begin();
	}
}

void CStreamReader::on_content( const boost::system::error_code& ec, const std::size_t& length )
{
	if (!ec && length > 0 )
	{
//		printf("on_content:: length[%d], time[%d]\n", length, 10 - pedding_cut_down_time_);
		pedding_cut_down_time_ = cs_pedding_cut_down_time;

		if (length < reading_size_)
		{
			std::size_t write_pos = reading_buffer_ptr_->size_ - reading_size_ + length;
			reading_size_ = reading_size_ - length;
			http_client_->async_receive(boost::asio::buffer(reading_buffer_ptr_->buf_.get() + write_pos, reading_size_),
				boost::bind(&CStreamReader::on_content, this, _1, _2));
			return;
		}

		boost::uint32_t tag_ts = 0;
		switch (stat_)
		{
		case STAT_NEED_HEAD:
			{
				switch (reading_stat_)
				{
				case READING_HEAD:
					{
						FLVFileHeader_t* flvHeader = (FLVFileHeader_t*)(reading_buffer_ptr_->buf_.get());
						if (!strncmp((char*)(flvHeader->signature), "FLV", 3))	// FLV
						{
							got_audio_head_ = got_video_head_ = false;
							got_seek_point_ = false;
							seek_point_pos_ = 0;
							cut_with_kf_ = need_cut_with_kf_;
							should_cut_count_ = 0;

							is_header_over_ = false;
							got_first_data_piece_ = false;
							got_first_data_tag_ = false;
							flv_tag_ts_delta_ = 0;
							flv_tag_beg_timestamp_ = 0;

							piece_ptr_->push_back(reading_buffer_ptr_);	

							reading_size_ = FLV_TAGHEADER_T_SIZE;
							reading_stat_ = READING_FLV_TAGHEAD;
						}
						else
						{
							get_logger().printf(lilogger::kLevelAlarm, "It is NOT a flv data!");
							writer_->ReaderError();
							async_begin();
							return;
						}
					}
					break;
				case READING_FLV_TAGHEAD:
					{
						FLVTagHeader_t* flvTagHeader = (FLVTagHeader_t*)(reading_buffer_ptr_->buf_.get());
						cur_flv_tag_type_ = (int)flvTagHeader->type;
						tag_ts = FLVTagHeader_t::get_tag_ts(flvTagHeader);
						if (flv_tag_beg_timestamp_ == 0) flv_tag_beg_timestamp_ = tag_ts;
						// Added by Tady, 06152011: For audio or video only stream.
						if (/*(int)(tag_ts -  flv_tag_beg_timestamp_) > 10000 */
							tag_ts > 0
							&& (got_audio_head_ || got_video_head_))
						{
							is_header_over_ = true;

							if (got_first_data_tag_ == false)
							{
								if (tag_ts < flv_tag_timestamp_)
								{
									flv_tag_timestamp_ += 500;
									flv_tag_ts_delta_ = flv_tag_timestamp_ - tag_ts;

									if ((boost::int32_t)flv_tag_timestamp_ < 0)
									{
										flv_tag_timestamp_ &= 0x7fffffff;
										flv_tag_ts_delta_ = flv_tag_timestamp_ - tag_ts;
									}

									if (need_modify_ts_) 
										FLVTagHeader_t::set_tag_ts(flvTagHeader, flv_tag_timestamp_);
								}
								else
									flv_tag_timestamp_ = tag_ts;
								got_first_data_tag_ = true;
							}

							cur_flv_tag_ts_ = flv_tag_timestamp_ / cut_scale_;
						}
						piece_ptr_->push_back(reading_buffer_ptr_);	

						reading_size_ = FLV_UI24(flvTagHeader->datasize) + FLV_PREVIOUSTAGSIZE_SIZE;
						reading_stat_ = READING_FLV_TAGBODY;
					}
					break;
				case READING_FLV_TAGBODY:
					{
						if (is_header_over_)
						{	
							assert(piece_ptr_->size() > 1);
							piece_ptr tmp_piece_ptr(new std::vector<liter_buffer_ptr>());

							tmp_piece_ptr->push_back(piece_ptr_->back());
							tmp_piece_ptr->push_back(reading_buffer_ptr_);
							piece_ptr_->pop_back();

							cur_piece_ts_ = cur_flv_tag_ts_;
							stat_ = STAT_NEED_FLV_DATA;
							// post a piece.
							writer_->AsyncWritePiece(piece_ptr_, 0);

							piece_ptr_ = tmp_piece_ptr;

							if (!got_video_head_)
								cut_with_kf_ = false;
						}
						else
						{
							bool need_drop_tag = false;
							switch (cur_flv_tag_type_)
							{
							case FLV_AUDIODATA:
								{
									AudioTagDataHeader_t* aHeader = (AudioTagDataHeader_t*)reading_buffer_ptr_->buf_.get();
									if ((aHeader->audioformat & 0xf0)  == 0xa0)	// aac
									{
										if (aHeader->aacpackettype == 0)		// sequence header.
										{
											got_audio_head_ = true;
											FLVTagHeader_t* flvTagHeader = (FLVTagHeader_t*)(piece_ptr_->back()->buf_.get());
											FLVTagHeader_t::set_tag_ts(flvTagHeader, 0);

										}
										else 
											need_drop_tag = true;// drop it.
									}
									else 
									{
										// Do not cache it in header.
										got_audio_head_ = true;
										need_drop_tag = true;// drop it.
									}
								}
								break;
							case FLV_VIDEODATA:
								{
									VideoTagDataHeader_t* vHeader = (VideoTagDataHeader_t*)reading_buffer_ptr_->buf_.get();
									if ((vHeader->frameandcodetype & 0xf) == 0x07)	// avc.
									{
										if (vHeader->avcpackettype == 0)			// sequence header.
										{
											got_video_head_ = true;
											FLVTagHeader_t* flvTagHeader = (FLVTagHeader_t*)(piece_ptr_->back()->buf_.get());
											FLVTagHeader_t::set_tag_ts(flvTagHeader, 0);
										}
										else
											need_drop_tag = true;// drop it.
									}
									else
									{
										// Do not cache it in header.
										got_video_head_ = true;
										need_drop_tag = true;// drop it.

										cut_with_kf_ = false;
									}
								}
								break;
							case FLV_SCRIPTDATAOBJECT:
								{
								}
								break;
							default:
								{
								}
							}

							if (need_drop_tag == true)
								piece_ptr_->pop_back();
							else
								piece_ptr_->push_back(reading_buffer_ptr_);

							if (got_audio_head_ && got_video_head_)
							{
								stat_ = STAT_NEED_FLV_DATA;
								// post a piece.
								writer_->AsyncWritePiece(piece_ptr_, 0);
								piece_ptr_.reset(new std::vector<liter_buffer_ptr>());
							}
						}

						reading_size_ = FLV_TAGHEADER_T_SIZE;
						reading_stat_ = READING_FLV_TAGHEAD;
					}
					break;
				}
			}
			break;
		case STAT_NEED_FLV_DATA:
			{
				switch (reading_stat_)
				{
				case READING_FLV_TAGHEAD:
					{
						FLVTagHeader_t* flvTagHeader = (FLVTagHeader_t*)(reading_buffer_ptr_->buf_.get());
						cur_flv_tag_type_ = (int)flvTagHeader->type;
						tag_ts = FLVTagHeader_t::get_tag_ts(flvTagHeader);
						if (cur_flv_tag_type_ == FLV_AUDIODATA || cur_flv_tag_type_ == FLV_VIDEODATA)
						{
							if (got_first_data_tag_ == false)
							{
								if (tag_ts < flv_tag_timestamp_)
								{
									flv_tag_timestamp_ += 500;
									flv_tag_ts_delta_ = flv_tag_timestamp_ - tag_ts;
								}
								got_first_data_tag_ = true;
							}
							if (flv_tag_ts_delta_ > 0)
							{
								if (tag_ts + flv_tag_ts_delta_ ==  flv_tag_timestamp_)
									flv_tag_timestamp_ += 1;
								else
									flv_tag_timestamp_ = tag_ts + flv_tag_ts_delta_;

								if ((boost::int32_t)flv_tag_timestamp_ < 0)
								{
									flv_tag_timestamp_ &= 0x7fffffff;
									flv_tag_ts_delta_ = flv_tag_timestamp_ - tag_ts;
								}

								if (need_modify_ts_)
									FLVTagHeader_t::set_tag_ts(flvTagHeader, flv_tag_timestamp_);
							}
							else
							{
								if (tag_ts == flv_tag_timestamp_)
								{
									tag_ts += 1;
									if (need_modify_ts_)
										FLVTagHeader_t::set_tag_ts(flvTagHeader, tag_ts);
								}
								if (tag_ts - flv_tag_timestamp_ > 100)
									get_logger().printf(lilogger::kLevelDebug, "flv_ts_delta: === %d === ", tag_ts - flv_tag_timestamp_);
								flv_tag_timestamp_ = tag_ts;
							}
							cur_flv_tag_ts_ = flv_tag_timestamp_ / cut_scale_;
							if (cur_piece_ts_ == 0xffffffff) 
								cur_piece_ts_ = cur_flv_tag_ts_;		

							int ts_delta = (int)(cur_flv_tag_ts_ - cur_piece_ts_);
							if (ts_delta > 0)
							{ // post a piece
								if (!cut_with_kf_)
								{
									writer_->AsyncWritePiece(piece_ptr_, seek_point_pos_, flv_tag_timestamp_);
									piece_ptr_.reset(new std::vector<liter_buffer_ptr>());

									cur_piece_ts_ = cur_flv_tag_ts_;
									got_seek_point_ = false;
									seek_point_pos_ = 0;
								}
								else
								{
									//								if (!can_cut_)
									cur_piece_ts_ = cur_flv_tag_ts_;
									//								can_cut_ = true;
									//								should_cut_count_ += ts_delta;
									should_cut_count_++;
								}
							}
							else if (ts_delta < 0)
							{
								if (cur_piece_ts_ > 429490 && cur_flv_tag_ts_ < 10)
								{
									// 								boost::uint32_t cur_tag_ts = 0x80000000;
									// 								cur_tag_ts += cur_flv_tag_ts_ * cut_scale_;
									// 								ts_delta = (int)(cur_tag_ts - cur_piece_ts_ * cut_scale_) / cut_scale_;

									ts_delta = (flv_tag_timestamp_ + 0x80000000) / cut_scale_ - cur_piece_ts_;
									if (ts_delta > 0)
									{ // post a piece 
										if (!cut_with_kf_)
										{
											writer_->AsyncWritePiece(piece_ptr_, seek_point_pos_, flv_tag_timestamp_);
											piece_ptr_.reset(new std::vector<liter_buffer_ptr>());

											cur_piece_ts_ = cur_flv_tag_ts_;
											got_seek_point_ = false;
											seek_point_pos_ = 0;
										}
										else
										{
											//									if (!can_cut_)
											cur_piece_ts_ = cur_flv_tag_ts_;
											//									can_cut_ = true;
											should_cut_count_++;
										}
									}
								}
								else if (cur_flv_tag_ts_ != 0 && ts_delta < -2)
								{	// Big gap!
									// Restart
									cur_piece_ts_ = 0xffffffff;
									writer_->ReaderError();
									async_begin();
									return;
								}
							}
						}

						if (got_seek_point_ == false)
						{
							seek_point_pos_ += reading_buffer_ptr_->size_;
						}
						piece_ptr_->push_back(reading_buffer_ptr_);
						reading_size_ = FLV_UI24(flvTagHeader->datasize) + FLV_PREVIOUSTAGSIZE_SIZE;
						reading_stat_ = READING_FLV_TAGBODY;
					}
					break;
				case READING_FLV_TAGBODY:
					{

						if (cur_flv_tag_type_ == FLV_AUDIODATA)
						{
							AudioTagDataHeader_t* aHeader = (AudioTagDataHeader_t*)reading_buffer_ptr_->buf_.get();
							if ((aHeader->audioformat & 0xf0)  == 0xa0	// aac
								&& aHeader->aacpackettype == 0)			// sequence header.
							{
								get_logger().printf(lilogger::kLevelEvent, "Update aac sequence header.");
//								writer_->ReaderError();
//								async_begin();
//								return;

// 								liter_buffer_ptr last_buf = piece_ptr_->back();
// 								FLVTagHeader_t* flvTagHeader = (FLVTagHeader_t*)(last_buf->buf_.get());
// 								FLVTagHeader_t::set_tag_ts(flvTagHeader, 0);

								aac_sequence_header_.reset(new std::vector<liter_buffer_ptr>());
								aac_sequence_header_->push_back(piece_ptr_->back());
								aac_sequence_header_->push_back(reading_buffer_ptr_);
							}
						}
						else if (cur_flv_tag_type_ == FLV_VIDEODATA)
						{
							VideoTagDataHeader_t* vHeader = (VideoTagDataHeader_t*)reading_buffer_ptr_->buf_.get();
							if ((vHeader->frameandcodetype & 0xf) == 0x7	// avc
								&& vHeader->avcpackettype == 0)			// sequence header.
							{
								get_logger().printf(lilogger::kLevelEvent, "Update avc sequence header.");
// 								writer_->ReaderError();
// 								async_begin();
// 								return;

								avc_sequence_header_.reset(new std::vector<liter_buffer_ptr>());
								avc_sequence_header_->push_back(piece_ptr_->back());
								avc_sequence_header_->push_back(reading_buffer_ptr_);
								cut_with_kf_ = need_cut_with_kf_;
							}
							else if ((vHeader->frameandcodetype & 0xf0) == 0x10)
							{
								if (!cut_with_kf_)
								{
									got_seek_point_ = true;
									seek_point_pos_ -= FLV_TAGHEADER_T_SIZE;
								}
								else
								{
//									if (can_cut_)
									if (should_cut_count_ > 0)
									{
										got_seek_point_ = true;
										seek_point_pos_ = 0;

										liter_buffer_ptr last_buf = piece_ptr_->back();
										piece_ptr_->pop_back();
//										if (piece_ptr_->size() > 0)
										{
											// Added by Tady, 06182011: When cut_with_kf is true
											// , the very first piece is usually starting with a non-kf. 
											// So we drop it!
											//if (got_first_data_piece_)
												writer_->AsyncWritePiece(piece_ptr_, seek_point_pos_, flv_tag_timestamp_);
// 											else
// 												got_first_data_piece_ = true;

											piece_ptr_.reset(new std::vector<liter_buffer_ptr>());
											--should_cut_count_;
										}
// 										else
// 											assert(0);

										piece_ptr_->push_back(last_buf);
//										cur_piece_ts_ = cur_flv_tag_ts_;
										get_logger().printf(lilogger::kLevelDebug, "Cut point: < %d >, should cut count[ %d ]", cur_flv_tag_ts_, should_cut_count_);
//										can_cut_ = false;
/*
										if (should_cut_count_ > 2)
										{
											// Insert empty piece
											while (should_cut_count_ > 0)
											{
												--should_cut_count_;
												writer_->AsyncWritePiece(piece_ptr(new std::vector<liter_buffer_ptr>()), 0);
											}

										}
*/
										
										// Update sequence header.
										if (aac_sequence_header_)
										{
											writer_->AsyncUpdateHeader(aac_sequence_header_);
											aac_sequence_header_.reset();

										}
										if (avc_sequence_header_)
										{
											writer_->AsyncUpdateHeader(avc_sequence_header_);
											avc_sequence_header_.reset();
										}
									}
								}
							}
						}

						if (should_cut_count_ > 2)
						{
							// Insert empty piece
							while (should_cut_count_ > 1)
							{
								--should_cut_count_;
								writer_->AsyncWritePiece(piece_ptr(new std::vector<liter_buffer_ptr>()), 0);
							}
						}

						if (got_seek_point_ == false)
						{
							seek_point_pos_ += reading_buffer_ptr_->size_;
						}
						piece_ptr_->push_back(reading_buffer_ptr_);
						reading_size_ = FLV_TAGHEADER_T_SIZE;
						reading_stat_ = READING_FLV_TAGHEAD;
					}
					break;
				default:
					{
						assert(0);
					}
				}

			}
			break;
		default:
			{
				assert(0);
			}
		}
		
		if (reading_size_ > 1024 * 1024 * 10)
		{
			writer_->ReaderError();
			async_begin();
		}
		else
		{
			reading_buffer_ptr_.reset(new lite_buffer(reading_size_));
			http_client_->async_receive(boost::asio::buffer(reading_buffer_ptr_->buf_.get(), reading_size_),
				boost::bind(&CStreamReader::on_content, this, _1, _2));
		}
	}
	else
	{ // Restart
		writer_->ReaderError();
		async_begin();
	}
}

void CStreamReader::async_begin()
{
	is_reader_ready_ = false;
//	start_cut_down_time_ = (cut_scale_ / 1000) > cs_start_cut_down_time ? (cut_scale_ / 1000) : cs_start_cut_down_time;
	start_cut_down_time_ = (cut_scale_ / 1000) > cs_start_cut_down_time ? 1 : cs_start_cut_down_time;
}

bool CStreamReader::do_begin()
{
	if (http_client_)
	{
		boost::system::error_code close_ec;
		http_client_->close(close_ec);
	}
	http_client_.reset(new util::protocol::HttpClient(ios_));

	if (!url_.empty())
	{
//		cur_piece_ts_ = 0xffffffff;
		piece_ptr_.reset(new std::vector<liter_buffer_ptr>());
		stat_ = STAT_NEED_HEAD;
		reading_stat_ = READING_HEAD;

		http_client_->async_open(url_,
			boost::bind(&CStreamReader::on_http_open, this, _1));
		return true;
	}
	else
		return false;
}

void CStreamReader::OnClockAlarm()
{
	if (is_reader_ready_ == true)
	{
		--pedding_cut_down_time_;
		if (pedding_cut_down_time_ == 0)
		{
			get_logger().printf(lilogger::kLevelEvent, "OnClockAlarm:: Time out!");
			assert (http_client_);
			boost::system::error_code close_ec;
			http_client_->cancel(close_ec);
		}
	}
	else
	{
		--start_cut_down_time_;
		if (start_cut_down_time_ == 0)
		{
			do_begin();
		}
		else if (start_cut_down_time_ < -30)
		{
			boost::system::error_code close_ec;
			http_client_->cancel(close_ec);
		}
	}
}

#ifndef _TINY_VLC_
#define _TINY_VLC_

#include "elements.h"

//! Bitstream
struct bit_stream
{
	// CABAC Decoding
	int           read_len;           //!< actual position in the codebuffer, CABAC only
	int           code_len;           //!< overall codebuffer length, CABAC only
	// CAVLC Decoding
	int           frame_bitoffset;    //!< actual position in the codebuffer, bit-oriented, CAVLC only
	int           bitstream_length;   //!< over codebuffer lnegth, byte oriented, CAVLC only
	// ErrorConcealment
	byte          *streamBuffer;      //!< actual codebuffer for read bytes
	int           ei_flag;            //!< error indication, 0: no error, else unspecified error
};
typedef struct bit_stream Bitstream;

class NaluParser
{
public:
	NaluParser()
		: is_ready_(false), got_sps_(false), got_pps_(false)
		, pic_order_cnt_lsb(0)
	{
	}

	void parse(Bitstream* bs);
	void parse_with_startcode(Bitstream *bs);
	void parse_without_startcode(Bitstream* bs);
	void parse_AVCDecoderConfigurationRecord(Bitstream*bs);

	void parse_NALU(Bitstream* bs);

	struct Nal_header
	{
		char nal_unit_type : 5;		// 5 bit
		char nal_ref_idc : 2;	    // 2 bit 	
		char forbidden_zero_bit : 1;  // 1 bit
	};

	struct AVCDecoderConfigurationRecord
	{
		int configuration_version_;
		int avc_profile_indication_;
		int profile_compatibility;
		int avc_level_indication_;
		int length_size_minus_one;
		int num_of_sps;
		int num_of_pps;
	};

	AVCDecoderConfigurationRecord avc_decoder_configuration_record_;
	bool is_ready_;
	bool got_sps_;
	bool got_pps_;
	Nal_header cur_nal_header_;

	// SPS struct
	char profile_idc;   // u(8) 
	char useless1;		// u(8)
// 		constraint_set0_flag  u(1) 
// 		constraint_set1_flag  u(1) 
// 		constraint_set2_flag  u(1) 
// 		reserved_zero_5bits /* equal to 0 */  0 u(5) 
	char level_idc;					// u(8)
	int sps_seq_parameter_set_id;		// ue_v
	int chroma_format_idc;			// ue_v
	char residual_colour_transform_flag; // u(1)
	int bit_depth_luma_minus8;		// ue_v
	int bit_depth_chroma_minus8;	
	int qpprime_y_zero_transform_bypass_flag;
	int seq_scaling_matrix_present_flag;
	char seq_scaling_list_present_flag[8]; // u(1)
	int	delta_scale;
	int log2_max_frame_num_minus4;	// ue_v
	int pic_order_cnt_type;			// ue_v

	int log2_max_pic_order_cnt_lsb_minus4;	// ue_v
	char delta_pic_order_always_zero_flag;	// u(1)
	int offset_for_non_ref_pic;				// se_v
	int offset_for_top_to_bottom_field;		
	int num_ref_frames_in_pic_order_cnt_cycle;	// ue_v
	int offset_for_ref_frame[100];			// se_v
	int num_ref_frames;						// ue_v
	char gaps_in_frame_num_value_allowed_flag;	// u(1)
	int pic_width_in_mbs_minus1;			// ue_v
	int pic_height_in_map_units_minus1;	
	char frame_mbs_only_flag;				// u(1)


	// pic_parameter_set_rbsp
	int pps_pic_parameter_set_id;		// ue_v
	int pps_seq_parameter_set_id;		
	char entropy_coding_mode_flag;	// u(1)
	char pic_order_present_flag; 
	int num_slice_groups_minus1;	// ue_v
	int slice_group_map_type;
	int run_length_minus1;			// ue_v[]
	int top_left;					
	int bottom_right;				
	char slice_group_change_direction_flag;	// u(1)
	int slice_group_change_rate_minus1;		// ue_v
	int pic_size_in_map_units_minus1;		// ue_v
	int slice_group_id;						// u_v[]
	int num_ref_idx_l0_active_minus1;		// ue_v
	int num_ref_idx_l1_active_minus1;
	char weighted_pred_flag;				// u(1)
	char weighted_bipred_idc;				// u(2)
	int pic_init_qp_minus26;				// se_v
	int pic_init_qs_minus26;
	int chroma_qp_index_offset;
	char deblocking_filter_control_present_flag; // u(1)
	char constrained_intra_pred_flag;
	char redundant_pic_cnt_present_flag;

	// slice_layer_without_partitioning_rbsp
	// slice_header
	int first_mb_in_slice;	// ue_v
	int	slice_type;
	int sh_pic_parameter_set_id;
	int frame_num;			// u_v
	char field_pic_flag;	// u(1)
	char bottom_field_flag;
	int idr_pic_id;			// ue_v
	int pic_order_cnt_lsb;	// u_v
	int delta_pic_order_cnt_bottom;	// se_v
	int delta_pic_order_cnt[2];	
	int redundant_pic_cnt;	// ue_v

};


#endif // _TINY_VLC_

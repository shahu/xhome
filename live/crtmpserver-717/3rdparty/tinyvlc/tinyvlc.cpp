
#include <math.h>
#include "tinyvlc.h"
//#include <stdio.h>
//! Syntaxelement
typedef struct syntaxelement
{
	int           type;                  //!< type of syntax element for data part.
	int           value1;                //!< numerical value of syntax element
	int           value2;                //!< for blocked symbols, e.g. run/level
	int           len;                   //!< length of code
	int           inf;                   //!< info part of CAVLC code
	unsigned int  bitpattern;            //!< CAVLC bitpattern
	int           context;               //!< CABAC context
	int           k;                     //!< CABAC context for coeff_count,uv

#if TRACE
#define       TRACESTRING_SIZE 100           //!< size of trace string
	char          tracestring[TRACESTRING_SIZE]; //!< trace string
#endif

	//! for mapping of CAVLC to syntaxElement
	void  (*mapping)(int len, int info, int *value1, int *value2);
	//! used for CABAC: refers to actual coding method of each individual syntax element type
	//	void  (*reading)(Macroblock *currMB, struct syntaxelement *, DecodingEnvironmentPtr);
} SyntaxElement;


#define SYMTRACESTRING (void)

/*!
************************************************************************
* \brief
*    mapping rule for ue(v) syntax elements
* \par Input:
*    lenght and info
* \par Output:
*    number in the code table
************************************************************************
*/
void linfo_ue(int len, int info, int *value1, int *dummy)
{
	//assert ((len >> 1) < 32);
	*value1 = (int) (((unsigned int) 1 << (len >> 1)) + (unsigned int) (info) - 1);
}

/*!
************************************************************************
* \brief
*    mapping rule for se(v) syntax elements
* \par Input:
*    lenght and info
* \par Output:
*    signed mvd
************************************************************************
*/
void linfo_se(int len,  int info, int *value1, int *dummy)
{
	//assert ((len >> 1) < 32);
	unsigned int n = ((unsigned int) 1 << (len >> 1)) + (unsigned int) info - 1;
	*value1 = (n + 1) >> 1;
	if((n & 0x01) == 0)                           // lsb is signed bit
		*value1 = -*value1;
}

/*!
************************************************************************
* \brief
*  read one exp-golomb VLC symbol
*
* \param buffer
*    containing VLC-coded data bits
* \param totbitoffset
*    bit offset from start of partition
* \param  info
*    returns the value of the symbol
* \param bytecount
*    buffer length
* \return
*    bits read
************************************************************************
*/
int GetVLCSymbol (byte buffer[],int totbitoffset,int *info, int bytecount)
{
	long byteoffset = (totbitoffset >> 3);         // byte from start of buffer
	int  bitoffset  = (7 - (totbitoffset & 0x07)); // bit from start of byte
	int  bitcounter = 1;
	int  len        = 0;
	byte *cur_byte  = &(buffer[byteoffset]);
	int  ctr_bit    = ((*cur_byte) >> (bitoffset)) & 0x01;  // control bit for current bit posision

	while (ctr_bit == 0)
	{                 // find leading 1 bit
		len++;
		bitcounter++;
		bitoffset--;
		bitoffset &= 0x07;
		cur_byte  += (bitoffset == 7);
		byteoffset+= (bitoffset == 7);      
		ctr_bit    = ((*cur_byte) >> (bitoffset)) & 0x01;
	}

	if (byteoffset + ((len + 7) >> 3) > bytecount)
		return -1;
	else
	{
		// make infoword
		int inf = 0;                          // shortest possible code is 1, then info is always 0    

		while (len--)
		{
			bitoffset --;    
			bitoffset &= 0x07;
			cur_byte  += (bitoffset == 7);
			bitcounter++;
			inf <<= 1;    
			inf |= ((*cur_byte) >> (bitoffset)) & 0x01;
		}

		*info = inf;
		return bitcounter;           // return absolute offset in bit from start of frame
	}
}

/*!
************************************************************************
* \brief
*  Reads bits from the bitstream buffer
*
* \param buffer
*    containing VLC-coded data bits
* \param totbitoffset
*    bit offset from start of partition
* \param info
*    returns value of the read bits
* \param bitcount
*    total bytes in bitstream
* \param numbits
*    number of bits to read
*
************************************************************************
*/
int GetBits (byte buffer[],int totbitoffset,int *info, int bitcount,
			 int numbits)
{
	if ((totbitoffset + numbits ) > bitcount) 
	{
		return -1;
	}
	else
	{
		int bitoffset  = 7 - (totbitoffset & 0x07); // bit from start of byte
		int byteoffset = (totbitoffset >> 3); // byte from start of buffer
		int bitcounter = numbits;
		byte *curbyte  = &(buffer[byteoffset]);
		int inf = 0;

		while (numbits--)
		{
			inf <<=1;    
			inf |= ((*curbyte)>> (bitoffset--)) & 0x01;    
			if (bitoffset == -1 ) 
			{ //Move onto next byte to get all of numbits
				curbyte++;
				bitoffset = 7;
			}
			// Above conditional could also be avoided using the following:
			// curbyte   -= (bitoffset >> 3);
			// bitoffset &= 0x07;
		}
		*info = inf;

		return bitcounter;           // return absolute offset in bit from start of frame
	}
}

/*!
************************************************************************
* \brief
*    read next UVLC codeword from UVLC-partition and
*    map it to the corresponding syntax element
************************************************************************
*/
int readSyntaxElement_VLC(SyntaxElement *sym, Bitstream *currStream)
{

	sym->len =  GetVLCSymbol (currStream->streamBuffer, currStream->frame_bitoffset, &(sym->inf), currStream->bitstream_length);
	if (sym->len == -1)
		return -1;

	currStream->frame_bitoffset += sym->len;
	sym->mapping(sym->len, sym->inf, &(sym->value1), &(sym->value2));

#if TRACE
	tracebits(sym->tracestring, sym->len, sym->inf, sym->value1);
#endif

	return 1;
}

/*!
************************************************************************
* \brief
*    read FLC codeword from UVLC-partition
************************************************************************
*/
int readSyntaxElement_FLC(SyntaxElement *sym, Bitstream *currStream)
{
	int BitstreamLengthInBits  = (currStream->bitstream_length << 3) + 7;

	if ((GetBits(currStream->streamBuffer, currStream->frame_bitoffset, &(sym->inf), BitstreamLengthInBits, sym->len)) < 0)
		return -1;

	sym->value1 = sym->inf;
	currStream->frame_bitoffset += sym->len; // move bitstream pointer

#if TRACE
	tracebits2(sym->tracestring, sym->len, sym->inf);
#endif

	return 1;
}

/*!
*************************************************************************************
* \brief
*    ue_v, reads an ue(v) syntax element, the length in bits is stored in
*    the global p_Dec->UsedBits variable
*
* \param tracestring
*    the string for the trace file
*
* \param bitstream
*    the stream to be read from
*
* \return
*    the value of the coded syntax element
*
*************************************************************************************
*/
int ue_v (char *tracestring, Bitstream *bitstream)
{
	SyntaxElement symbol;

	//assert (bitstream->streamBuffer != NULL);
	symbol.type = SE_HEADER;
	symbol.mapping = linfo_ue;   // Mapping rule
	SYMTRACESTRING(tracestring);
	readSyntaxElement_VLC (&symbol, bitstream);
	//	p_Dec->UsedBits+=symbol.len;
	return symbol.value1;
}

/*!
*************************************************************************************
* \brief
*    ue_v, reads an se(v) syntax element, the length in bits is stored in
*    the global p_Dec->UsedBits variable
*
* \param tracestring
*    the string for the trace file
*
* \param bitstream
*    the stream to be read from
*
* \return
*    the value of the coded syntax element
*
*************************************************************************************
*/
int se_v (char *tracestring, Bitstream *bitstream)
{
	SyntaxElement symbol;

	//assert (bitstream->streamBuffer != NULL);
	symbol.type = SE_HEADER;
	symbol.mapping = linfo_se;   // Mapping rule: signed integer
	SYMTRACESTRING(tracestring);
	readSyntaxElement_VLC (&symbol, bitstream);
	//	p_Dec->UsedBits+=symbol.len;
	return symbol.value1;
}

/*!
*************************************************************************************
* \brief
*    ue_v, reads an u(v) syntax element, the length in bits is stored in
*    the global p_Dec->UsedBits variable
*
* \param LenInBits
*    length of the syntax element
*
* \param tracestring
*    the string for the trace file
*
* \param bitstream
*    the stream to be read from
*
* \return
*    the value of the coded syntax element
*
*************************************************************************************
*/
int u_v (int LenInBits, char*tracestring, Bitstream *bitstream)
{
	SyntaxElement symbol;
	symbol.inf = 0;

	//assert (bitstream->streamBuffer != NULL);
	symbol.type = SE_HEADER;
	symbol.mapping = linfo_ue;   // Mapping rule
	symbol.len = LenInBits;
	SYMTRACESTRING(tracestring);
	readSyntaxElement_FLC (&symbol, bitstream);
	//	p_Dec->UsedBits+=symbol.len;

	return symbol.inf;
}

int next_bits(Bitstream* bitstream, int LenInBits, int* info)
{
	return GetBits(bitstream->streamBuffer, bitstream->frame_bitoffset, info, (bitstream->bitstream_length << 3) + 7, LenInBits);
}


//#define LengthInBits(x) ((x << 3) + 7)
#define LengthInBits(x) (x << 3)

inline bool CheckDataEnoughInBits(Bitstream* bs, int lenInBits)
{
	return ((bs->frame_bitoffset + lenInBits)  <= LengthInBits(bs->bitstream_length));
};

#define pass_in_bits(x) bs->frame_bitoffset += x;

#define parse_ue_v(x) x = ue_v(0, bs)
#define parse_se_v(x) x = se_v(0, bs)
#define parse_u_v(x, y) x = u_v(y, 0, bs)
#define parse_u_1(x) next_bits(bs, 1, (int*)&x); pass_in_bits(1);

void NaluParser::parse( Bitstream* bs )
{
	is_ready_ = false;
	do 
	{
		// parse start_code_prefix
		bool got_start_code(false);
		int start_code(0);
		bs->frame_bitoffset = (bs->frame_bitoffset & 0xfffffff8) + (bool(bs->frame_bitoffset & 0x07) << 3);
		do 
		{
			if (next_bits(bs, 24, &start_code) != 24)
				break;
			if (start_code == 0x000001)
			{
				bs->frame_bitoffset += 24;
				got_start_code = true;
				break;
			}
			if (next_bits(bs, 32, &start_code) != 32)
				break;
			if (start_code == 0x00000001)
			{
				bs->frame_bitoffset += 32;
				got_start_code = true;
				break;
			}
			//////////////////////////////////////////////////////////////////////////
			bs->frame_bitoffset += 32;
			got_start_code = true;
			break;
			//////////////////////////////////////////////////////////////////////////
			bs->frame_bitoffset += 8;
		} while (bs->frame_bitoffset < LengthInBits(bs->bitstream_length));

		if (got_start_code == false)
			return;

		// parse nal_header
		if ((bs->frame_bitoffset + 8 ) > LengthInBits(bs->bitstream_length)) 
			return;

		cur_nal_header_ = *(Nal_header*)(bs->streamBuffer + (bs->frame_bitoffset >> 3));
		bs->frame_bitoffset += 8;

		switch (cur_nal_header_.nal_unit_type)
		{
		case 7: // SPS
			{
				if (!CheckDataEnoughInBits(bs, 24)) 
					return;
				profile_idc = *(char*)(bs->streamBuffer + (bs->frame_bitoffset >> 3));
				useless1 = *(char*)(bs->streamBuffer + (bs->frame_bitoffset >> 3) + 1);
				level_idc = *(char*)(bs->streamBuffer + (bs->frame_bitoffset >> 3) + 2);
				pass_in_bits(24);

				parse_ue_v(sps_seq_parameter_set_id);

				if (profile_idc == (char)100 || profile_idc == (char)110
					|| profile_idc == (char)122 || profile_idc == (char)144)
				{
					parse_ue_v(chroma_format_idc);
					if (chroma_format_idc == 3)
					{
						//residual_colour_transform_flag
						//bs->frame_bitoffset += 1;
						pass_in_bits(1);
					}
					parse_ue_v(bit_depth_luma_minus8);
					parse_ue_v(bit_depth_chroma_minus8);
					parse_u_1(qpprime_y_zero_transform_bypass_flag);
					parse_u_1(seq_scaling_matrix_present_flag);
					if (seq_scaling_matrix_present_flag)
					{
						//							seq_scaling_list_present_flag // u(8)
						//							pass_in_bits(8); // passed.
						for (int i(0); i < 8; ++i)
						{
							parse_u_1(seq_scaling_list_present_flag);
							if (seq_scaling_list_present_flag)
							{
								if (i < 6)
								{
									int lastScale(8), nextScale(8);
									for (int j(0); j < 16; ++j)
									{
										if (nextScale != 0)
										{
											parse_se_v(delta_scale);
											nextScale = ( lastScale + delta_scale + 256 ) % 256;
										}
										lastScale = ( nextScale  ==  0 ) ? lastScale : nextScale;
									}
								}
								else
								{

									int lastScale(8), nextScale(8);
									for (int j(0); j < 64; ++j)
									{
										if (nextScale != 0)
										{
											parse_se_v(delta_scale);
											nextScale = ( lastScale + delta_scale + 256 ) % 256;
										}
										lastScale = ( nextScale  ==  0 ) ? lastScale : nextScale;
									}
								}
							}
						}
					}

				}

				parse_ue_v(log2_max_frame_num_minus4);
				parse_ue_v(pic_order_cnt_type);

				if (pic_order_cnt_type == 0)
				{
					parse_ue_v(log2_max_pic_order_cnt_lsb_minus4);
				}
				else if (pic_order_cnt_type == 1)
				{
					parse_u_1(delta_pic_order_always_zero_flag);
					parse_se_v(offset_for_non_ref_pic);
					parse_se_v(offset_for_top_to_bottom_field);
					parse_ue_v(num_ref_frames_in_pic_order_cnt_cycle); 
					for( int i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; ++i )
					{
						parse_se_v(offset_for_ref_frame[i]);
					}

				}
				parse_ue_v(num_ref_frames);
				parse_u_1(gaps_in_frame_num_value_allowed_flag);
				parse_ue_v(pic_width_in_mbs_minus1);
				parse_ue_v(pic_height_in_map_units_minus1);
				parse_u_1(frame_mbs_only_flag);

				got_sps_ = true;
			}
			break;
		case 8: // pps
			{
				parse_ue_v(pps_pic_parameter_set_id);
				parse_ue_v(pps_seq_parameter_set_id);
				parse_u_1(entropy_coding_mode_flag);
				parse_u_1(pic_order_present_flag);
				parse_ue_v(num_slice_groups_minus1);
				if( num_slice_groups_minus1 > 0 ) 
				{   
					parse_ue_v(slice_group_map_type);
					if( slice_group_map_type  ==  0 )  
					{
						for( int iGroup = 0; iGroup <= num_slice_groups_minus1; ++iGroup )
							parse_ue_v(run_length_minus1);
					}
					else if( slice_group_map_type  ==  2 )
					{
						for( int iGroup = 0; iGroup < num_slice_groups_minus1; ++iGroup ) 
						{   
							parse_ue_v(top_left/*[iGroup]*/); 
							parse_ue_v(bottom_right/*[iGroup]*/);
						}
					}
					else if(  slice_group_map_type == 3 || 
						slice_group_map_type == 4 || 
						slice_group_map_type == 5 )
					{
						parse_u_1(slice_group_change_direction_flag);
						parse_ue_v(slice_group_change_rate_minus1);
					}
					else if( slice_group_map_type == 6 ) 
					{   
						parse_ue_v(pic_size_in_map_units_minus1);
						for(int i = 0; i <= pic_size_in_map_units_minus1; i++)
						{
							//parse_u_v(slice_group_id, ceil( log2( num_slice_groups_minus1 + 1 ) )/*[ i ]*/);
							parse_u_v(slice_group_id, (int)ceil( log( (double)num_slice_groups_minus1 + 1 ) / log((double)2) )/*[ i ]*/);
						}
					}
				}

				parse_ue_v(num_ref_idx_l0_active_minus1);
				parse_ue_v(num_ref_idx_l1_active_minus1);
				// 					parse_u_1(weighted_pred_flag);
				// 					parse_u_2(weighted_bipred_idc);
				pass_in_bits(3);
				parse_se_v(pic_init_qp_minus26);
				parse_se_v(pic_init_qs_minus26);
				parse_se_v(chroma_qp_index_offset);
				parse_u_1(deblocking_filter_control_present_flag);
				parse_u_1(constrained_intra_pred_flag);
				parse_u_1(redundant_pic_cnt_present_flag);

				got_pps_ = true;
			}
			break;
		case 1: // non-IDR
		case 5: // IDR
			{
				if (!got_sps_
					|| (pic_order_cnt_type != 0 && !got_sps_))
				{
					return;
				}

				parse_ue_v(first_mb_in_slice);
				parse_ue_v(slice_type);
				parse_ue_v(sh_pic_parameter_set_id);
				parse_u_v(frame_num, log2_max_frame_num_minus4 + 4);
				if (!frame_mbs_only_flag) 
				{
					parse_u_1(field_pic_flag);
					if (field_pic_flag)
					{
						parse_u_1(bottom_field_flag);
					}
				}

				if( cur_nal_header_.nal_unit_type  ==  5 )   
					parse_ue_v(idr_pic_id);
				if( pic_order_cnt_type  ==  0 )
				{
					parse_u_v(pic_order_cnt_lsb, log2_max_pic_order_cnt_lsb_minus4 + 4);
// 					if (slice_type == 7)
// 					{
// 						printf("\nI[%d] ", pic_order_cnt_lsb);
// 					}
// 					else if (slice_type == 5)
// 					{
// 						printf("P[%d] ", pic_order_cnt_lsb);
// 					}
// 					else
// 					{
// 
// 						printf("B[%d] ", pic_order_cnt_lsb);
// 					}
					if( pic_order_present_flag &&  !field_pic_flag )   
						parse_se_v(delta_pic_order_cnt_bottom);
				}
				if( pic_order_cnt_type == 1 && !delta_pic_order_always_zero_flag )
				{
					parse_se_v(delta_pic_order_cnt[0]);
					if( pic_order_present_flag  &&  !field_pic_flag )   
						parse_se_v(delta_pic_order_cnt[1]);
				}
				if( redundant_pic_cnt_present_flag )   
					parse_ue_v(redundant_pic_cnt);
				is_ready_ = true;
			}
			break;
		default:
			break;
		}

	} while (!is_ready_);
}

void NaluParser::parse_NALU( Bitstream* bs )
{
	if ((bs->frame_bitoffset + 8 ) > LengthInBits(bs->bitstream_length)) 
		return;

	cur_nal_header_ = *(Nal_header*)(bs->streamBuffer + (bs->frame_bitoffset >> 3));
	bs->frame_bitoffset += 8;

	switch (cur_nal_header_.nal_unit_type)
	{
	case 7: // SPS
		{
			if (!CheckDataEnoughInBits(bs, 24)) 
				return;
			profile_idc = *(char*)(bs->streamBuffer + (bs->frame_bitoffset >> 3));
			useless1 = *(char*)(bs->streamBuffer + (bs->frame_bitoffset >> 3) + 1);
			level_idc = *(char*)(bs->streamBuffer + (bs->frame_bitoffset >> 3) + 2);
			pass_in_bits(24);

			parse_ue_v(sps_seq_parameter_set_id);

			if (profile_idc == (char)100 || profile_idc == (char)110
				|| profile_idc == (char)122 || profile_idc == (char)144)
			{
				parse_ue_v(chroma_format_idc);
				if (chroma_format_idc == 3)
				{
					//residual_colour_transform_flag
					//bs->frame_bitoffset += 1;
					pass_in_bits(1);
				}
				parse_ue_v(bit_depth_luma_minus8);
				parse_ue_v(bit_depth_chroma_minus8);
				parse_u_1(qpprime_y_zero_transform_bypass_flag);
				parse_u_1(seq_scaling_matrix_present_flag);
				if (seq_scaling_matrix_present_flag)
				{
					//							seq_scaling_list_present_flag // u(8)
					//							pass_in_bits(8); // passed.
					for (int i(0); i < 8; ++i)
					{
						parse_u_1(seq_scaling_list_present_flag);
						if (seq_scaling_list_present_flag)
						{
							if (i < 6)
							{
								int lastScale(8), nextScale(8);
								for (int j(0); j < 16; ++j)
								{
									if (nextScale != 0)
									{
										parse_se_v(delta_scale);
										nextScale = ( lastScale + delta_scale + 256 ) % 256;
									}
									lastScale = ( nextScale  ==  0 ) ? lastScale : nextScale;
								}
							}
							else
							{

								int lastScale(8), nextScale(8);
								for (int j(0); j < 64; ++j)
								{
									if (nextScale != 0)
									{
										parse_se_v(delta_scale);
										nextScale = ( lastScale + delta_scale + 256 ) % 256;
									}
									lastScale = ( nextScale  ==  0 ) ? lastScale : nextScale;
								}
							}
						}
					}
				}

			}

			parse_ue_v(log2_max_frame_num_minus4);
			parse_ue_v(pic_order_cnt_type);

			if (pic_order_cnt_type == 0)
			{
				parse_ue_v(log2_max_pic_order_cnt_lsb_minus4);
			}
			else if (pic_order_cnt_type == 1)
			{
				parse_u_1(delta_pic_order_always_zero_flag);
				parse_se_v(offset_for_non_ref_pic);
				parse_se_v(offset_for_top_to_bottom_field);
				parse_ue_v(num_ref_frames_in_pic_order_cnt_cycle); 
				for( int i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; ++i )
				{
					parse_se_v(offset_for_ref_frame[i]);
				}

			}
			parse_ue_v(num_ref_frames);
			parse_u_1(gaps_in_frame_num_value_allowed_flag);
			parse_ue_v(pic_width_in_mbs_minus1);
			parse_ue_v(pic_height_in_map_units_minus1);
			parse_u_1(frame_mbs_only_flag);

			got_sps_ = true;
		}
		break;
	case 8: // pps
		{
			parse_ue_v(pps_pic_parameter_set_id);
			parse_ue_v(pps_seq_parameter_set_id);
			parse_u_1(entropy_coding_mode_flag);
			parse_u_1(pic_order_present_flag);
			parse_ue_v(num_slice_groups_minus1);
			if( num_slice_groups_minus1 > 0 ) 
			{   
				parse_ue_v(slice_group_map_type);
				if( slice_group_map_type  ==  0 )  
				{
					for( int iGroup = 0; iGroup <= num_slice_groups_minus1; ++iGroup )
						parse_ue_v(run_length_minus1);
				}
				else if( slice_group_map_type  ==  2 )
				{
					for( int iGroup = 0; iGroup < num_slice_groups_minus1; ++iGroup ) 
					{   
						parse_ue_v(top_left/*[iGroup]*/); 
						parse_ue_v(bottom_right/*[iGroup]*/);
					}
				}
				else if(  slice_group_map_type == 3 || 
					slice_group_map_type == 4 || 
					slice_group_map_type == 5 )
				{
					parse_u_1(slice_group_change_direction_flag);
					parse_ue_v(slice_group_change_rate_minus1);
				}
				else if( slice_group_map_type == 6 ) 
				{   
					parse_ue_v(pic_size_in_map_units_minus1);
					for(int i = 0; i <= pic_size_in_map_units_minus1; i++)
					{
						//parse_u_v(slice_group_id, ceil( log2( num_slice_groups_minus1 + 1 ) )/*[ i ]*/);
						parse_u_v(slice_group_id, (int)ceil( log( (double)num_slice_groups_minus1 + 1 ) / log((double)2) )/*[ i ]*/);
					}
				}
			}

			parse_ue_v(num_ref_idx_l0_active_minus1);
			parse_ue_v(num_ref_idx_l1_active_minus1);
			// 					parse_u_1(weighted_pred_flag);
			// 					parse_u_2(weighted_bipred_idc);
			pass_in_bits(3);
			parse_se_v(pic_init_qp_minus26);
			parse_se_v(pic_init_qs_minus26);
			parse_se_v(chroma_qp_index_offset);
			parse_u_1(deblocking_filter_control_present_flag);
			parse_u_1(constrained_intra_pred_flag);
			parse_u_1(redundant_pic_cnt_present_flag);

			got_pps_ = true;
		}
		break;
	case 1: // non-IDR
	case 5: // IDR
		{
			if (!got_sps_
				|| (pic_order_cnt_type != 0 && !got_sps_))
			{
				return;
			}

			parse_ue_v(first_mb_in_slice);
			parse_ue_v(slice_type);
			parse_ue_v(sh_pic_parameter_set_id);
			parse_u_v(frame_num, log2_max_frame_num_minus4 + 4);
			if (!frame_mbs_only_flag) 
			{
				parse_u_1(field_pic_flag);
				if (field_pic_flag)
				{
					parse_u_1(bottom_field_flag);
				}
			}

			if( cur_nal_header_.nal_unit_type  ==  5 )   
				parse_ue_v(idr_pic_id);
			if( pic_order_cnt_type  ==  0 )
			{
				parse_u_v(pic_order_cnt_lsb, log2_max_pic_order_cnt_lsb_minus4 + 4);
				// 					if (slice_type == 7)
				// 					{
				// 						printf("\nI[%d] ", pic_order_cnt_lsb);
				// 					}
				// 					else if (slice_type == 5)
				// 					{
				// 						printf("P[%d] ", pic_order_cnt_lsb);
				// 					}
				// 					else
				// 					{
				// 
				// 						printf("B[%d] ", pic_order_cnt_lsb);
				// 					}
				if( pic_order_present_flag &&  !field_pic_flag )   
					parse_se_v(delta_pic_order_cnt_bottom);
			}
			if( pic_order_cnt_type == 1 && !delta_pic_order_always_zero_flag )
			{
				parse_se_v(delta_pic_order_cnt[0]);
				if( pic_order_present_flag  &&  !field_pic_flag )   
					parse_se_v(delta_pic_order_cnt[1]);
			}
			if( redundant_pic_cnt_present_flag )   
				parse_ue_v(redundant_pic_cnt);
			is_ready_ = true;
		}
		break;
	default:
		break;
	}
}

void NaluParser::parse_AVCDecoderConfigurationRecord( Bitstream*bs )
{
	avc_decoder_configuration_record_.configuration_version_ = (int)*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3));
	avc_decoder_configuration_record_.avc_profile_indication_ = (int)*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3) + 1);
	avc_decoder_configuration_record_.profile_compatibility = (int)*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3) + 2);
	avc_decoder_configuration_record_.avc_level_indication_ = (int)*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3) + 3);
	avc_decoder_configuration_record_.length_size_minus_one = (int)(*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3) + 4) & 0x3);

	avc_decoder_configuration_record_.num_of_sps = (int)(*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3) + 5) & 0x1f);
	bs->frame_bitoffset += (6 << 3);
	int sps_length(0);
	int sps_beg_offset = bs->frame_bitoffset;
	for (int i = 0; i < avc_decoder_configuration_record_.num_of_sps; ++i)
	{
		sps_length += (int)*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3)) << 8; 
		sps_length += (int)*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3) + 1);
		sps_length += 2;
		bs->frame_bitoffset += 16;
		parse_NALU(bs);
	}
	
	bs->frame_bitoffset = (sps_length << 3) + sps_beg_offset;
	avc_decoder_configuration_record_.num_of_pps = (int)*((unsigned char*)bs->streamBuffer + (bs->frame_bitoffset >> 3));
	bs->frame_bitoffset += 8;
	for (int j = 0; j < avc_decoder_configuration_record_.num_of_pps; ++j)
	{
		bs->frame_bitoffset += 16;
		parse_NALU(bs);
	}
}

void NaluParser::parse_with_startcode( Bitstream *bs )
{
	is_ready_ = false;
	do 
	{
		// parse start_code_prefix
		bool got_start_code(false);
		int start_code(0);
		bs->frame_bitoffset = (bs->frame_bitoffset & 0xfffffff8) + (bool(bs->frame_bitoffset & 0x07) << 3);
		do 
		{
			if (next_bits(bs, 24, &start_code) != 24)
				break;
			if (start_code == 0x000001)
			{
				bs->frame_bitoffset += 24;
				got_start_code = true;
				break;
			}
			if (next_bits(bs, 32, &start_code) != 32)
				break;
			if (start_code == 0x00000001)
			{
				bs->frame_bitoffset += 32;
				got_start_code = true;
				break;
			}
			bs->frame_bitoffset += 8;
		} while (bs->frame_bitoffset < LengthInBits(bs->bitstream_length));

		if (got_start_code == false)
			return;

		parse_NALU(bs);

	} while(!is_ready_);
}

void NaluParser::parse_without_startcode( Bitstream* bs )
{
	is_ready_ = false;
	do 
	{
		// parse start_code_prefix
// 		bool got_start_code(false);
// 		int start_code(0);
		bs->frame_bitoffset = (bs->frame_bitoffset & 0xfffffff8) + (bool(bs->frame_bitoffset & 0x07) << 3);
	
		int nalu_len(0);
		int next_nalu_bitoffset(0);
		if (next_bits(bs, 32, &nalu_len) != 32 || nalu_len == 0)
			break;
		bs->frame_bitoffset += 32;
		next_nalu_bitoffset = bs->frame_bitoffset + (nalu_len << 3);
		parse_NALU(bs);
	
		//assert(bs->frame_bitoffset <= next_nalu_bitoffset);
		bs->frame_bitoffset = next_nalu_bitoffset;
	} while (bs->frame_bitoffset < LengthInBits(bs->bitstream_length));
}


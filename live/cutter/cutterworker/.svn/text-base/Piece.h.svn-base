#pragma once

#include "framework/container/Array.h"

//////////////////////////////////////////////////////////////////////////
// Original Piece.

#pragma pack (1)
struct PIECE_HEADER
{
	boost::uint32_t PIECE_LENGTH; //整个这一秒的PIECE的长度，包括自己的所有长度
	boost::uint32_t CHECK_SUM;
	boost::uint8_t	VERSION;
	boost::uint16_t PIECE_HEADER_LENGTH; //包括上两项整个PIECE_HEADER的长度
	boost::uint32_t PIECE_ID;
	boost::uint32_t DATA_HEADER_PIECE_ID;

	boost::uint16_t DATA_HEADER_LENGTH;
	boost::uint32_t DATA_HEADER_CHECKSUM;

	boost::uint8_t	HAS_SEEK_POINT;
	boost::uint32_t SEEK_POINT;

	PIECE_HEADER()
		: PIECE_LENGTH(0), CHECK_SUM(0), VERSION(1), PIECE_HEADER_LENGTH(0)
		, PIECE_ID(0), DATA_HEADER_PIECE_ID(0)
		, DATA_HEADER_LENGTH(0), DATA_HEADER_CHECKSUM(0)
		, HAS_SEEK_POINT(0), SEEK_POINT(0)
	{
	}

	template <typename Archive>
	void serialize(Archive & ar)
	{
		ar & PIECE_LENGTH;
		ar & CHECK_SUM;
		ar & VERSION;
		ar & PIECE_HEADER_LENGTH;
		ar & PIECE_ID;
		ar & DATA_HEADER_PIECE_ID;
		if (PIECE_ID == DATA_HEADER_PIECE_ID)
		{
			ar & DATA_HEADER_LENGTH;
			ar & DATA_HEADER_CHECKSUM;
		}
		ar & HAS_SEEK_POINT;
		ar & SEEK_POINT;
	}

};


#pragma pack ()

const boost::uint32_t PIECE_HEADER_T_SIZE = 24;
const boost::uint32_t PIECE_DATA_HEADER_T_SIZE = 6;


//////////////////////////////////////////////////////////////////////////
// FLV Block.

struct FLV_BLOCK_HEADER
{
	boost::uint8_t	HASH_VALUE[16];
	boost::uint8_t  RID[16];
	boost::uint32_t BLOCK_HEADER_LENGTH; //包括上两项整个PIECE_HEADER的长度
	boost::uint32_t DATA_LENGTH;
	boost::uint32_t BLOCK_ID;
	boost::uint32_t VERSION;
	boost::uint8_t	CHECKSUM_LIST[1352];

	FLV_BLOCK_HEADER()
		: BLOCK_HEADER_LENGTH(0), DATA_LENGTH(0), BLOCK_ID(0), VERSION(1)
	{
	}

	template <typename Archive>
	void serialize(Archive & ar)
	{
		ar & framework::container::make_array(HASH_VALUE);
		ar & framework::container::make_array(RID);
		ar & BLOCK_HEADER_LENGTH;
		ar & DATA_LENGTH;
		ar & BLOCK_ID;
		ar & VERSION;
		ar & framework::container::make_array(CHECKSUM_LIST);
	}

};

const boost::uint32_t FLV_BLOCK_HEADER_T_SIZE = 1400; // Unit is byte.
const boost::uint32_t FLV_BLOCK_ROOT_HEADER_T_SIZE = 48;
const boost::uint32_t FLV_BLOCK_SIZE = 16 * FLV_BLOCK_HEADER_T_SIZE;
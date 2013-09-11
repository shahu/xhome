#include "commonData.h"

//return -1 on error.
int init_xhome_service(CallbackFunc* cb,
	char* url, //target address. for example: "tcp://ip:port" or "/home/filename.flv"
	int audioCodec, // aac
	int audioFormatSize, // 2byte
	int audioSampleRate, // 44100
	int audioChannels, // 2
	int audioBitrate, // 256 kbps
	int videoCodec, // h264
	int videoColorFormat, // yuv420p
	int videoWidth, //720
	int videoHeight, //480
	int videoBitrate); // 512kbps

//return -1 on error.
int pushFrame(FrameInfo_t* fram, char* data);

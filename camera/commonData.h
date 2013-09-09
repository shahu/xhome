typedef enum{
    DUMMY_FRAME = (-2),
    EMPTY_FRAME = (-1),
    AUDIO_FRAME = 0,
    I_FRAME,
    P_FRAME,
    B_FRAME,
    END_FRAME_TYPE
}FrameType;

typedef enum {
	FMT_MJPEG = 0,
	FMT_MPEG4,
	FMT_MPEG4_EXT,//H264
	FMT_AUDIO,
	FMT_MAX_NUM
}FrameFormat_t;

#pragma pack(1)
typedef struct _FrameInfo{
	int serial_no;
	int width;//width
	int height;//height
	int format;//defined in FrameFormat_t,the encode formate
	int frameType;//defined in enum FrameType
//	unsigned int quality;//don't know what's the usage
//	unsigned int flags;//don't know what's the usage
	unsigned int timestamp;//the time stamp
    unsigned int frameRate_samplingRate;//video frame rate or audio samplingRate
    unsigned int video_cap;//video's capability
	unsigned long size;  //the size of the video/audio data
	char data[0];
}FrameInfo_t;
#pragma pack()

typedef struct _CallbackFunc {
    //一些设置命令的callback函数，目前还不许要，但是应该包括查询，设置系统配置，无线网络，控制摄像头参数等回调函数
    int (*setFSFunc) (unsigned int fs) ;//set the frame frequence of the camera
    int (*setAngleFunc) (unsigned int angle); //0<angle<360；调整到一个绝对值角度。一开始的位置设置为０；
    unsigned int (*queryAngleFunc) ();//查询当前的绝对角度
}CallbackFunc;


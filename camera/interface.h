#include "commonData.h"
//提供给大帅调用的，在pushFram前使用。用来把你实现的回调函数注册给上层应用。我们会在这个函数里面做一些初始化的事情并启动我们的线程。
int init_xhome_service(CallbackFunc *cb);

//提供给大帅调用的，把一帧的信息和内容传递给上层，注意把数据紧接在size的后面，用size来指名数据的长度。
int pushFrame(FrameInfo_t * frame, char* data);

#include <netinet/in.h>    // for sockaddr_in
#include <sys/types.h>    // for socket
#include <arpa/inet.h>
#include <sys/socket.h>    // for socket
#include <stdio.h>        // for printf
#include <stdlib.h>        // for exit
#include <string.h>        // for bzero
#include <pthread.h>      //


#define SERVER "192.168.0.103"
#define SERVER_REQUEST_PORT 6666
typedef unsigned char UINT8;
typedef unsigned short UINT16;
typedef unsigned int UINT32;

#pragma pack(1)
typedef struct _MsgHeader{
    UINT16 ID;
    UINT16 size;
    UINT16 lrc;
    UINT16 reserved;
    UINT32 referid;
    char*  data;
}MsgHeader;
#pragma pack(0)

/*
   typedef struct _request{
   MsgHeader header;
   char *data;
   }Request;
   */
enum MSGID {
    SERVER_MESSAGE_BEGIN = 0,
    HANDSHAKE ,
    GET_CAMERA_INFO ,
    ROTATE_CAMERA ,
    CAMERICA_MESSAGE_BEGIN = 1000,
    GET_DATA_SERVER_INFO
};



void* MessageHandler(void*arg);
MsgHeader* getMsg();
void pollMsg();
int sendMsg(MsgHeader*header,unsigned size,char*data);

#define MYFREEMSG(x) do {\
    if (NULL!=x){ \
        if (x->size>0){\
            free(x->data);\
            x->data=NULL;\
        }\
        free(x);\
        x=NULL;\
    }\
} while(0)


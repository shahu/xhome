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
typedef signed char INT8;
typedef unsigned char UINT8;
typedef short INT16;
typedef unsigned short UINT16;
typedef int INT32;
typedef unsigned int UINT32;

#pragma pack(1)
typedef struct _MsgHeader{
    UINT16 ID;
    UINT16 size;
    UINT16 lrc;
    UINT16 reserved;
    UINT32 referid;
    char  data[0];
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

typedef struct _Data_Server_Info{
    UINT32  port;
    char ip[17];
}Data_Server_Info;

typedef enum _ROTATE_DIRECTION{
    LEFT=0x1,
    RIGHT=0x2,
    TOP=0x4,
    DOWN=0x8
}ROTATE_DIRECTION;

typedef struct _Data_Rotate{
    ROTATE_DIRECTION direction;
    INT32 degree;
}Data_Rotate;


void* MessageHandler(void*arg);
MsgHeader* getMsg();
int pollMsg();
int sendMsg(MsgHeader*header,unsigned size,char*data);

#define MYFREEMSG(x) do {\
       free(x);\
       x=NULL;\
} while(0)


#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include "MessageHandler.h"
#define MSG_VERSION 1
extern int send_data;
extern unsigned data_server_port;
extern char data_server[50];
int client_socket;

void* MessageHandler(void*arg){

    //1. initial the socket to connect to the  the server

    //设置一个socket地址结构client_addr,代表客户机internet地址, 端口
    struct sockaddr_in client_addr;
    bzero(&client_addr,sizeof(client_addr)); //把一段内存区的内容全部设置为0
    client_addr.sin_family = AF_INET;    //internet协议族
    client_addr.sin_addr.s_addr = htons(INADDR_ANY);//INADDR_ANY表示自动获取本机地址
    client_addr.sin_port = htons(0);    //0表示让系统自动分配一个空闲端口
    //创建用于internet的流协议(TDP)socket,用client_socket代表客户机socket
    int client_socket = socket(AF_INET,SOCK_STREAM,0);
    if( client_socket < 0)
    {
        printf("Create Socket Failed!\n");
        exit(1);
    }
    //把客户机的socket和客户机的socket地址结构联系起来
    if( bind(client_socket,(struct sockaddr*)&client_addr,sizeof(client_addr)))
    {
        printf("Client Bind Port Failed!\n"); 
        exit(1);
    }

    //设置一个socket地址结构server_addr,代表服务器的internet地址, 端口
    struct sockaddr_in server_addr;
    bzero(&server_addr,sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    if(inet_aton(SERVER,&server_addr.sin_addr) == 0) 
    {
        printf("Server IP Address Error!\n");
        exit(1);
    }
    server_addr.sin_port = htons(SERVER_REQUEST_PORT);
    socklen_t server_addr_length = sizeof(server_addr);
    unsigned int try_connect = 0;
    //向服务器发起连接,连接成功后client_socket代表了客户机和服务器的一个socket连接
    while ( try_connect < 10000 ){
        if(connect(client_socket,(struct sockaddr*)&server_addr, server_addr_length) < 0)
        {
            printf("Can Not Connect To %s!\n",SERVER);
            try_connect++;
            sleep(30);
            continue;
        }
        break;
    }
    if ( 10000 == try_connect ){
        printf("Can Not Connect to %s failed after tryied %d times!!\n",SERVER,try_connect);
        exit(1);
    }

    pollMsg(client_socket);

    //关闭socket
    close(client_socket);
    return 0;
}
/*
   void* transfer_data_routine(void* arg)
   {
   char data_server[100];
   unsigned int data_port = *((unsigned int *)arg);
   sprintf(data_server,"%s",(char*)arg+sizeof(unsigned int));



   delete (ARG*)arg;
   pthread_exit(NULL);
   }
   */

MsgHeader * getMsg(){
    MsgHeader* header=malloc(sizeof(MsgHeader));
    if (NULL==header){
        printf ("failed to allocate the memory for MsgHeader");
        return NULL;
    }
    int length = recv(client_socket,header,sizeof(MsgHeader),0);
    if ( length != sizeof(MsgHeader) ){
        printf ("cannot receive the right message from server, restart the process ");
        exit(1);
    }
    if (header->size!=0){
        header->data=(char*)malloc(header->size);
        if (NULL==header->data){
            printf ("failed to allocate the memory for MsgHeader data");
            free (header);
            return NULL;
        }
        length = recv(client_socket,header->data,header->size,0);
        if ( length != header->size ){
            printf ("cannot receive the right message data from server, restart the process ");
            exit(1);
        }
    }
    printf("received msg %d with data size %d",header->ID,header->size);
    return header;
}
int sendMsg(MsgHeader*header,unsigned size,char*data){
    header->size=size;
    if ( send(client_socket,header,sizeof(MsgHeader),0) < 0){
        printf("failed to send the header ");
        return -1;
    }
    if (size>0){
        if ( send(client_socket,data,size,0) < 0){
            printf("failed to send the data ");
            return -1;
        }
    }
    return 0;
}
void pollMsg(){
    unsigned int version = MSG_VERSION; 
    MsgHeader *msg=NULL;
    while (NULL!=(msg=getMsg())){
        switch (msg->ID){
            case HANDSHAKE:
                sendMsg(msg,sizeof(unsigned int),(char*)&version);
                MYFREEMSG(msg);
                MsgHeader request;
                request.ID=GET_DATA_SERVER_INFO;
                request.size=0;
                sendMsg(&request,0,0);
                break;
            case GET_DATA_SERVER_INFO:
                break;

            case GET_CAMERA_INFO:
                break;
            case ROTATE_CAMERA:
                break;
            default:
                break;
        }
    }
}

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <semaphore.h>
#include <string.h>
#include "interface.h"
/**the format of the memory laylout is 
 * |ABC|ABC|ABC...
 * buffer.......buffer+MAX_MEMORY
 * A-->type:char*; speicify the address of next frame
 * B-->type:FrameInfo; specify the frame information, including the data size of the frame
 * C-->the data of the frame, the size is specified by B and the address follows B
 * for most of the time, A points to the next address of A, except that the next address needs to from the beginning of the buffer;
 * **/
CallbackFunc callFuncs;
extern void* StartDataTransfer(void*arg);
extern void* MessageHandler(void*arg);
#define MAX_MEMORY (1024*2*1024)
char buffer[MAX_MEMORY];
char *read_pos=buffer;//the address of next filled space
char *write_pos=buffer;//the address of next free space
char *buffer_end_pos=buffer+MAX_MEMORY;//the last address of filled space, we can only get data from [buffer,buffer_end_pos)
pthread_mutex_t w_mutex = PTHREAD_MUTEX_INITIALIZER;
sem_t full; //缓冲区有数据信号量

int init_xhome_service(CallbackFunc *cb){
    pthread_t tMessageHandlerThread,tDataTransferThread;
    pthread_mutex_init(&w_mutex,NULL);
    sem_init(&full,0,0);
    if (pthread_create(&tMessageHandlerThread,NULL, MessageHandler,NULL)){
        /* handle exception */
        perror("Pthread_create() error for MessageClientThread");
        return 1;
    }
    if (pthread_create(&tDataTransferThread,NULL, StartDataTransfer,NULL)){
        /* handle exception */
        perror("Pthread_create() error for DataTransferThread");
        return 1;
    }
    callFuncs.setFSFunc = cb->setFSFunc;
    return 0;
}



void myfree(){
    /*
    char * currentFrame=*((char**)(read_pos-sizeof(char*)));
    FrameInfo_t *currFram=(FrameInfo_t*)currentFrame;
    unsigned size=sizeof(FrameInfo_t)+currFram->size;
    */
    read_pos += sizeof(FrameInfo_t)+((FrameInfo_t*)read_pos)->size+1;
    if ( read_pos >= buffer_end_pos){
        read_pos = buffer;
        buffer_end_pos = buffer+MAX_MEMORY;
    }
}


void discardFrame(){
    /* do some special handling for how to discard a frame*/
    myfree();
}



char * myallocate(unsigned int size){

    //write_pos==read_pos only means that there's no data
    if (write_pos<read_pos && write_pos + size +1  >= read_pos  ){//means that there's not enouth space for write
        while (write_pos + size +1>= read_pos){
            discardFrame();
        }
    }
    if ( write_pos + size > buffer+MAX_MEMORY){//need to wrap from the very beginning
        buffer_end_pos=write_pos;
        write_pos=buffer;
        if (write_pos==read_pos)//here menas that the space is full, at least to free one frame
            discardFrame();
        return myallocate(size);
    }else {
        write_pos+=size+1;
        return write_pos-size-1;
    }
}


int pushFrame(FrameInfo_t *frame,char* data){
    pthread_mutex_lock(&w_mutex);
    char *waddress = myallocate(sizeof(FrameInfo_t)+frame->size);
    memcpy(waddress,frame,sizeof(FrameInfo_t));
    memcpy(waddress+sizeof(FrameInfo_t),data,frame->size);
    pthread_mutex_unlock(&w_mutex);
    sem_post(&full);
    return 0;
}
void locktopFrame(){
    pthread_mutex_lock(&w_mutex);
}
void unlocktopFrame(){
    pthread_mutex_unlock(&w_mutex);
}
/*
   FrameInfo_t * topFrame(){
   char * currentFrame=*((char**)(read_pos-sizeof(char*)));
   return (FrameInfo_t*)currentFrame;
   }
   */
//!!!!!free the pointor if no need any more!!!!!
FrameInfo_t*  popFrame(){
    sem_wait(&full);
    pthread_mutex_lock(&w_mutex);
//    char * currentFrame=*((char**)(read_pos-sizeof(char*)));
    char * topFrame = (char*) malloc(sizeof(FrameInfo_t)+((FrameInfo_t*)read_pos)->size);
    memcpy(topFrame,read_pos,sizeof(FrameInfo_t)+((FrameInfo_t*)read_pos)->size);
    myfree();
    pthread_mutex_unlock(&w_mutex);
    return (FrameInfo_t*)topFrame;
}

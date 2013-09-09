#include <stdio.h>
#include <stdlib.h>
#include "commonData.h"
extern FrameInfo_t* popFrame();
unsigned data_server_port=0;
char data_server[50];
int send_data=0;


void StartDataTransfer(){
    unsigned offset=0;
    FILE* fpw = fopen("record.file","wb");
    while(1){
    FrameInfo_t* topframe = popFrame();
    if (offset<5*1024*1024){
        fwrite(topframe,sizeof(char*),sizeof(FrameInfo_t)+topframe->size,fpw);
        offset+=sizeof(FrameInfo_t)+topframe->size;
    }else{
        fclose(fpw);
        fpw = fopen("record.file","wb");
    }
    free(topframe);
    }
}

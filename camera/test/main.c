#include <stdio.h>
#include <string.h>
#include "interface.h"
int main(){
    CallbackFunc func;

    if (init_xhome_service(&func)){
        printf("failed to start the xhome service\n");
        return 1;
    }
    char received[10000];
    while(1){
        scanf("%s",received);
        if (strcmp("quiet",received)){
            FrameInfo_t av_data;
            av_data.size=strlen(received)+1;
            pushFrame(&av_data,received);
        }else{
            printf("exit");
            return 1;
        }
    }
    return 0;
}

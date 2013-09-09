#include "commonData.h"

const FrameInfo_t*  popFrame();//data transfer call the function to get a frame info and data, 

//command data between data transfer and message handler thread
//bool send_data = False; 
//unsigned data_server_port = 0;//the dataServer port, modified by message handler thread
//char data_server_ip[50];//the dataServer ip, modified by message handler thread
//when send_data is True,  the client request the data and send it to the data server, initial the socket with the port and ip and then send the data
//when send_Data is false, you make a choice, discard all the frames or store it on the local filesystem;

/*****the interface you provided to me for initial the data transfer thread****/
//please tell me the function to initial the data transfer thread , i will create a thread with the function for you
//Example: startDataTransfer();
//pthread_create(&tTransferDataThread, NULL, startDataTransfer, NULL)

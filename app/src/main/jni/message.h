//
// Created by Jason YU on 2018/5/11.
//

#ifndef VPN4OVER6_MESSAGE_H
#define VPN4OVER6_MESSAGE_H

#include <netinet/in.h>
#include <arpa/inet.h>

#define MAX_MESSAGE_PAYLOAD 4096
#define MESSAGE_TYPE_IP_REQUEST 100
#define MESSAGE_TYPE_IP_RESPONSE 101
#define MESSAGE_TYPE_LOGIN_REQUEST 102
#define MESSAGE_TYPE_LOGIN_RESPONSE 103
#define MESSAGE_TYPE_ALIVE   104



typedef struct Message{
    int length;
    char type;
    char data[MAX_MESSAGE_PAYLOAD];
} message_t;

inline int message_get_length(message_t* msg){
    return ntohl(msg->length);
}

message_t* message_create(char type, void* buf, int len);
void message_free(message_t* msg);

#endif //VPN4OVER6_MESSAGE_H

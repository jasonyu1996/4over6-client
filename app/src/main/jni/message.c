//
// Created by Jason YU on 2018/5/13.
//

#include <stdlib.h>
#include <string.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include "message.h"


message_t* message_create(char type, void* buf, int len){
    message_t* msg = malloc(sizeof(struct Message));
    msg->type = type;
    msg->length = htonl(len + sizeof(char) + sizeof(int));
    memcpy(msg->data, buf, len);
    return msg;
}

void message_free(message_t* msg){
    free(msg);
}


const message_t MESSAGE_PULSE = {
    .length = htonl(sizeof(int) + sizeof(char)),
    .type = MESSAGE_TYPE_PULSE,
};

const message_t MESSAGE_IP_REQUEST = {
    .length = htonl(sizeof(int) + sizeof(char)),
    .type = MESSAGE_TYPE_IP_REQUEST,
};


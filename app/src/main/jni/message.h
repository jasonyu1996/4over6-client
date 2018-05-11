//
// Created by Jason YU on 2018/5/11.
//

#ifndef VPN4OVER6_MESSAGE_H
#define VPN4OVER6_MESSAGE_H

#define MAX_MESSAGE_PAYLOAD 4096

typedef struct Message{
    int length;
    char type;
    char data[MAX_MESSAGE_PAYLOAD];
} message_t;

#endif //VPN4OVER6_MESSAGE_H

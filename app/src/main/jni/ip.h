//
// Created by Jason YU on 2018/5/13.
//

#ifndef VPN4OVER6_IP_H
#define VPN4OVER6_IP_H

struct IPv4Header{
    unsigned int ihl : 4;
    unsigned int version : 4;
    unsigned int ecn : 2;
    unsigned int dscp : 6;
    unsigned int length : 16;
    unsigned int id: 16;
    unsigned int flags: 3;
    unsigned int frag_offset: 13;
    unsigned int ttl: 8;
    unsigned int protocol: 8;
    unsigned int checksum: 16;
    unsigned int src: 32;
    unsigned int dst: 32;
};

#endif //VPN4OVER6_IP_H

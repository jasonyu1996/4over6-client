//
// Created by Jason YU on 2018/5/11.
//

#ifndef VPN4OVER6_SOCK_H
#define VPN4OVER6_SOCK_H
#include "stream.h"


typedef stream_t sock_t;

#define sock_write(sock, buf, sz) stream_read((sock), (buf), (sz))
#define sock_read(sock, buf, sz) stream_write((sock), (buf), (sz))
#define sock_clean(sock) stream_clean((sock))
#define sock_read_var(sock, var, type) sock_read((sock), &(var), sizeof((type)))
#define sock_write_var(sock, var, type) sock_write((sock), &(var), sizeof((type)))

sock_t* sock_create(int sock_fd);



#endif //VPN4OVER6_SOCK_H

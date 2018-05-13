//
// Created by Jason YU on 2018/5/11.
//

#include <stdlib.h>
#include "sock.h"

#define SOCK_BUF_SIZE 2048


sock_t* sock_create(int sock_fd){
    sock_t* sock = (sock_t*)malloc(sizeof(sock_t));
    sock->buf = (char*)malloc(SOCK_BUF_SIZE);
    sock->buf_size = SOCK_BUF_SIZE;
    sock->fd = sock_fd;
    return sock;
}
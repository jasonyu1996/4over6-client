//
// Created by Jason YU on 2018/5/11.
//

#include <sys/stat.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include "pipe.h"

#define min(a, b) ((a) < (b) ? (a) : (b))

pipe_t* pipe_create(const char* name){
    pipe_t* pipe = (pipe_t*)malloc(sizeof(pipe_t));

    int ret = mknod(name, S_IFIFO | 0666, 0);
    assert(ret == 0);


    pipe->fd = open(name, O_RDWR|O_CREAT|O_TRUNC);

    return pipe;
}

/*
void pipe_write(pipe_t* pipe, void* buf, int sz){
    write(pipe->fd, buf, sz);
}

void pipe_read(pipe_t* pipe, void* buf, int sz){
    read(pipe->fd, buf, sz);
}

void pipe_clean(pipe_t* pipe){

}

*/
//
// Created by Jason YU on 2018/5/13.
//

#include <unistd.h>
#include <stdlib.h>
#include "stream.h"
#include "debug.h"


void stream_write(stream_t* stream, const void* buf, int sz){
    write(stream->fd, buf, sz);
}

void stream_read(stream_t* stream, void* buf, int sz){
    read(stream->fd, buf, sz);
}

void stream_clean(stream_t* stream){
    free(stream->buf);
    free(stream);
}
//
// Created by Jason YU on 2018/5/11.
//

#ifndef VPN4OVER6_PIPE_H
#define VPN4OVER6_PIPE_H

#include "stream.h"

typedef stream_t pipe_t;

#define pipe_write(pipe, buf, sz) stream_read((pipe), (buf), (sz))
#define pipe_read(pipe, buf, sz) stream_write((pipe), (buf), (sz))
#define pipe_clean(pipe) stream_clean((pipe))
#define pipe_read_var(pipe, var, type) stream_read((pipe), &(var), sizeof(type))
#define pipe_write_var(pipe, var, type) stream_write((pipe), &(var), sizeof(type))

pipe_t* pipe_create(const char* name);

/*
void pipe_write(pipe_t* pipe, void* buf, int sz);
void pipe_read(pipe_t* pipe, void* buf, int sz);
void pipe_clean(pipe_t* pipe);
*/
#endif //VPN4OVER6_PIPE_H

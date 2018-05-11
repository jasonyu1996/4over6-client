//
// Created by Jason YU on 2018/5/11.
//

#ifndef VPN4OVER6_PIPE_H
#define VPN4OVER6_PIPE_H


typedef struct Pipe {
    char* buf;
    int fd;
    int buf_size;
} pipe_t;

#define pipe_read_var(pipe, var, type) pipe_read((pipe), &(var), sizeof((type)))
#define pipe_write_var(pipe, var, type) pipe_write((pipe), &(var), sizeof((type)))

pipe_t* pipe_init(const char* name, int buf_size);

void pipe_write(pipe_t* pipe, void* buf, int sz);
void pipe_read(pipe_t* pipe, void* buf, int sz);
void pipe_clean(pipe_t* pipe);

#endif //VPN4OVER6_PIPE_H

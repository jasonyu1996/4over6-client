//
// Created by Jason YU on 2018/5/13.
//

#ifndef VPN4OVER6_STREAM_H
#define VPN4OVER6_STREAM_H


typedef struct Stream {
    char* buf;
    int fd;
    int buf_size;
} stream_t;

void stream_write(stream_t* stream, const void* buf, int sz);
void stream_read(stream_t* stream, void* buf, int sz);
void stream_clean(stream_t* stream);

#define stream_read_var(stream, var, type) stream_read((stream), &(var), sizeof(type))
#define stream_write_var(stream, var, type) stream_write((stream), &(var), sizeof(type))

#endif //VPN4OVER6_STREAM_H

#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>

#include "message.h"
#include "cn_edu_tsinghua_vpn4over6_VPNBackend.h"
#include "pipe.h"
#include "sock.h"
#include "stream.h"
#include "debug.h"

#define SERVER_ADDRESS "2402:f000:1:4417:0:0:0:900"
#define SERVER_PORT 5678
#define PIPE_NAME "vpn4over6_pipe"
#define PIPE_BUF_SIZE 2048
#define stream_write_message(stream, message) stream_write((stream), (message), message_get_length((message)))

void stream_read_message(stream_t* stream, message_t* msg){
    stream_read_var(stream, msg->length, int);
    int len = message_get_length(msg);
    stream_read(stream, &msg->type, len - sizeof(int));
}

JNIEXPORT jstring JNICALL Java_cn_edu_tsinghua_vpn4over6_VPNBackend_startThread
  (JNIEnv * env, jobject obj){
    pipe_t* pipe = pipe_create(PIPE_NAME, PIPE_BUF_SIZE);

    int sock_fd = socket(AF_INET6, SOCK_STREAM, 0);
    if(sock_fd == -1)
        return errno;
    struct sockaddr_in6 addr;
    addr.sin6_family = AF_INET6;
    addr.sin6_port = htons(SERVER_PORT);

    //2402:f000:1:4417::900
    inet_pton(AF_INET6, SERVER_ADDRESS, &addr.sin6_addr);
    int res = connect(sock_fd, &addr, sizeof(addr));

    char dumb;


    sock_t* sock = sock_create(sock_fd);



    message_t* msg = message_create(MESSAGE_TYPE_IP_REQUEST, &dumb, 0);
    LOGD("msg %d", message_get_length(msg));


    stream_write_message(sock, msg);

    stream_read_message(sock, msg);
    return (*env)->NewStringUTF(env, msg->data);
}


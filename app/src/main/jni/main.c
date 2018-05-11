#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include "cn_edu_tsinghua_vpn4over6_VPNBackend.h"
#include "pipe.h"

#define SERVER_ADDRESS "2402:f000:1:4417:0:0:0:900"
#define SERVER_PORT 5678
#define PIPE_NAME "vpn4over6_pipe"
#define PIPE_BUF_SIZE 2048
#define pipe_write_message(pipe, message) pipe_write((pipe), (message), (message)->length)

JNIEXPORT jint JNICALL Java_cn_edu_tsinghua_vpn4over6_VPNBackend_startThread
  (JNIEnv * env, jobject obj){
    pipe_t* pipe = pipe_init(PIPE_NAME, PIPE_BUF_SIZE);

    int sock_fd = socket(AF_INET6, SOCK_STREAM, 0);
    if(sock_fd == -1)
        return errno;
    struct sockaddr_in6 addr;
    addr.sin6_family = AF_INET6;
    addr.sin6_port = htons(SERVER_PORT);


    //2402:f000:1:4417::900
    inet_pton(AF_INET6, SERVER_ADDRESS, &addr.sin6_addr);
    int res = connect(sock_fd, &addr, sizeof(addr));
    if(res != 0)
        return errno;
    return 0;
}


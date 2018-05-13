#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <string.h>

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
#define SERVER_LIFE_SPAN 60

typedef unsigned int ipv4_t;

extern const message_t MESSAGE_IP_REQUEST, MESSAGE_PULSE;
static sock_t *sock;
static int pulse_count, server_pulse_count = 0;
static pthread_mutex_t server_pulse_count_lock;
static pthread_t pulse_thread, postman_thread;
static ipv4_t dns[3], host, router;

static void stream_read_message(stream_t* stream, message_t* msg){
    stream_read_var(stream, msg->length, int);
    int len = message_get_length(msg);
    stream_read(stream, &msg->type, len - sizeof(int));
}


// check the tun interface
static void pack_loop(){
    while(1){

    }
}

// this does nothing other than sending statistics to the frontend
// and sending pulse signal to the server
static void pulse_loop(){
    while(1){
        sleep(1);

        pthread_mutex_lock(&server_pulse_count_lock);
        ++ server_pulse_count;
        if(server_pulse_count > SERVER_LIFE_SPAN){
            // the server is down
        }
        pthread_mutex_unlock(&server_pulse_count_lock);

        ++ pulse_count;
        if(pulse_count > 20){
            pulse_count = 0;
            // send pulse signal to server every 20 secs
            stream_write_message(sock, &MESSAGE_PULSE);

            LOGD("Pulse sent!");
        }

        // TODO: write statistics to pipe
    }
}

// parse the ip info from the server
static void parse_ip(char* ip_str){
    char* ss[5];
    int i, c = 0;
    ss[0] = ip_str;

    for(i = 0; i < 4; i ++){
        ss[i + 1] = strsep(ss + i, " ");
    }

    inet_pton(AF_INET, ss[0], &host);
    inet_pton(AF_INET, ss[1], &router);
    for(i = 0; i < 3; i ++)
        inet_pton(AF_INET, ss[i + 2], dns + i);
}

// this continually checks messages from the server
// and acts accordingly
static void postman_loop(){
    static message_t msg;
    while(1){
        stream_read_message(sock, &msg);
        switch(msg.type){
        case MESSAGE_TYPE_IP_RESPONSE:
            LOGD("IP Response pack received: %s", msg.data);
            parse_ip(msg.data);
            //TODO: do something for IP response
        case MESSAGE_TYPE_SERVICE_RESPONSE:

        case MESSAGE_TYPE_PULSE:
            LOGD("Pulse pack received");

            pthread_mutex_lock(&server_pulse_count_lock);
            server_pulse_count = 0;
            pthread_mutex_unlock(&server_pulse_count_lock);
            break;
        default:
            LOGD("Caution: an unsupported pack (%d) received from the server.", msg.type);
        }
    }
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


    sock = sock_create(sock_fd);

    stream_write_message(sock, &MESSAGE_IP_REQUEST);

    // initialize the mutexes
    pthread_mutex_init(&server_pulse_count_lock, NULL);


    // initialize and start the threads
    pthread_create(&postman_thread, NULL, postman_loop, NULL);
    pthread_create(&pulse_thread, NULL, pulse_loop, NULL);


    return (*env)->NewStringUTF(env, "Well done!");
}


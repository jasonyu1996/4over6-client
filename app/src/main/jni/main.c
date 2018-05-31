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
#include <assert.h>
#include <fcntl.h>

#include "message.h"
#include "cn_edu_tsinghua_vpn4over6_VPNBackend.h"
#include "pipe.h"
#include "sock.h"
#include "stream.h"
#include "debug.h"
#include "ip.h"

#define SERVER_ADDRESS "2402:f000:1:4417:0:0:0:900"
#define SERVER_PORT 5678
#define OUT_PIPE_NAME "/data/data/cn.edu.tsinghua.vpn4over6/vpn4over6_pipe_out"
#define IN_PIPE_NAME "/data/data/cn.edu.tsinghua.vpn4over6/vpn4over6_pipe_in"
#define PIPE_BUF_SIZE 2048
#define stream_write_message(stream, message) stream_write((stream), (message), message_get_length((message)))
#define SERVER_LIFE_SPAN 60
#define INST_TUN_READY 0x1
#define min(a, b) ((a) < (b) ? (a) : (b))

typedef unsigned int ipv4_t;
typedef unsigned long long UL;

extern const message_t MESSAGE_IP_REQUEST, MESSAGE_PULSE;
static sock_t *sock, *tun_sock;
static int pulse_count, server_pulse_count = 0;
static UL sent_packet_len, sent_packet_cnt, received_packet_len, received_packet_cnt;
static pthread_mutex_t server_pulse_count_lock, pipe_lock;
static pthread_t pulse_thread, postman_thread, pack_thread;
static ipv4_t dns[3], host, router;
static pipe_t* pipe_v, *pipe_v_out;
static volatile int backend_running; // switch to start or end the threadse

static void stream_read_message(stream_t* stream, message_t* msg){
    stream_read_var(stream, msg->length, int);
    int len = message_get_length(msg);
    stream_read(stream, &msg->type, len - sizeof(int));
}

// check the tun interface
static void pack_loop(){
    struct IPv4Header header;
    static message_t msg = {
        .type = MESSAGE_TYPE_SERVICE_REQUEST,
    };
    unsigned int payload_len, rem_len;
    while(backend_running){
        sock_read_var(tun_sock, header, struct IPv4Header);
        LOGD("A packet intercepted at tun.");

        // split into multiple packets
        memcpy(msg.data, &header, sizeof(struct IPv4Header));
        // first 4over6 packet
        payload_len = min(header.length, MAX_MESSAGE_PAYLOAD) - sizeof(struct IPv4Header);
        sock_read(tun_sock, msg.data + sizeof(struct IPv4Header), payload_len);
        msg.length = htonl(payload_len + sizeof(struct IPv4Header) + sizeof(int) + sizeof(char));

        stream_write_message(tun_sock, &msg);

        // subsequent packet
        rem_len = msg.length - payload_len - sizeof(struct IPv4Header);
        while(rem_len){
            payload_len = min(rem_len, MAX_MESSAGE_PAYLOAD);
            msg.length = htonl(payload_len + sizeof(int) + sizeof(char));
            stream_write_message(tun_sock, &msg);

            rem_len -= payload_len;
        }

        ++ sent_packet_cnt;
        sent_packet_len += header.length;
    }
}

// this does nothing other than sending statistics to the frontend
// and sending pulse signal to the server
static void pulse_loop(){
    while(backend_running){
        sleep(1);

        pthread_mutex_lock(&server_pulse_count_lock);
        ++ server_pulse_count;
        if(server_pulse_count > SERVER_LIFE_SPAN){
            // the server is down

            //TODO: do something when the server is found to be down
        }
        pthread_mutex_unlock(&server_pulse_count_lock);

        ++ pulse_count;
        if(pulse_count > 20){
            pulse_count = 0;
            // send pulse signal to server every 20 secs
            stream_write_message(sock, &MESSAGE_PULSE);

            LOGD("Pulse sent!");
        }

        // write statistics to the pipe
        pthread_mutex_lock(&pipe_lock);
        pipe_write_var(pipe_v, sent_packet_len, UL);
        pipe_write_var(pipe_v, sent_packet_cnt, UL);
        pipe_write_var(pipe_v, received_packet_len, UL);
        pipe_write_var(pipe_v, received_packet_cnt, UL);
        pthread_mutex_unlock(&pipe_lock);
    }
}

// parse the ip info from the server
static void parse_ip(char* ip_str){
    char* ss[5], *ip_str_c = ip_str;
    int i, c = 0;

    for(i = 0; i < 5; i ++){
        ss[i] = strsep(&ip_str_c, " ");
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
    int tun_fd, i;
    char inst;
    while(backend_running){
        stream_read_message(sock, &msg);
        switch(msg.type){
        case MESSAGE_TYPE_IP_RESPONSE:
            LOGD("IP Response pack received: %s", msg.data);
            parse_ip(msg.data);


            // write ip info to the pipe
            pthread_mutex_lock(&pipe_lock);

            pipe_write_var(pipe_v_out, host, ipv4_t);

            pipe_write_var(pipe_v_out, router, ipv4_t);

            pthread_mutex_unlock(&pipe_lock);

            for(i = 0; i < 3; i ++)
                pipe_write_var(pipe_v_out, dns[i], ipv4_t);

            // fetch the fd for tun
            while(1){
                pipe_read_var(pipe_v, inst, char);
                if(inst == INST_TUN_READY){
                    tun_fd = open("/dev/tun", O_RDWR | O_APPEND);
                    assert(tun_fd >= 0);
                    tun_sock = sock_create(tun_fd);
                    break;
                }
            }

            // start forwarding packets
            pthread_create(&pack_thread, NULL, pack_loop, NULL);

            break;
        case MESSAGE_TYPE_SERVICE_RESPONSE:
            sock_write(tun_sock, msg.data, message_get_length(&msg) - sizeof(char) - sizeof(int));
            ++ received_packet_cnt;
            received_packet_len += message_get_length(&msg) - sizeof(char) - sizeof(int);

            break;

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

JNIEXPORT jint JNICALL Java_cn_edu_tsinghua_vpn4over6_VPNBackend_startThread
  (JNIEnv * env, jobject obj){

    int sock_fd = socket(AF_INET6, SOCK_STREAM, 0);
    if(sock_fd == -1)
        return errno;
    struct sockaddr_in6 addr;
    addr.sin6_family = AF_INET6;
    addr.sin6_port = htons(SERVER_PORT);

    //2402:f000:1:4417::900
    inet_pton(AF_INET6, SERVER_ADDRESS, &addr.sin6_addr);
    int res = connect(sock_fd, &addr, sizeof(addr));

    if(res != 0){
        return res;
    }

    pipe_v = pipe_create(IN_PIPE_NAME);
    pipe_v_out = pipe_create(OUT_PIPE_NAME);


    char dumb;


    sock = sock_create(sock_fd);

    stream_write_message(sock, &MESSAGE_IP_REQUEST);

    // initialize the mutexes
    pthread_mutex_init(&server_pulse_count_lock, NULL);
    pthread_mutex_init(&pipe_lock, NULL);


    // initialize and start the threads
    backend_running = 1;
    pthread_create(&postman_thread, NULL, postman_loop, NULL);
    pthread_create(&pulse_thread, NULL, pulse_loop, NULL);


    return 0;
}


JNIEXPORT jint JNICALL Java_cn_edu_tsinghua_vpn4over6_VPNBackend_endThread
  (JNIEnv * env, jobject obj){
    // not tested yet
    // this implementation is problematic
    // TODO: try making IO nonblocking
    backend_running = 0;
    sock_clean(sock);
    sock_clean(tun_sock);
    pipe_clean(pipe_v);

    return 0;
}

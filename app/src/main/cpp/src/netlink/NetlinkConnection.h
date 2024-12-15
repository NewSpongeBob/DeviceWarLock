#ifndef WARLOCK_NETLINK_CONNECTION_H
#define WARLOCK_NETLINK_CONNECTION_H

#include <sys/types.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <cstring>
#include <linux/if.h>
#include <linux/if_packet.h>
#include <errno.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>
#include "syscall.h"

struct nlmsghdr;
extern "C" {
__attribute__((always_inline)) long raw_syscall(long number, ...);
}
class NetlinkConnection {
public:
    // 构造和析构函数
    NetlinkConnection();
    ~NetlinkConnection();

    // 发送netlink请求
    bool SendRequest(int type);

    // 读取响应并通过回调处理
    bool ReadResponses(void callback(void*, nlmsghdr*), void* context);

private:
    int fd_;      // netlink socket文件描述符
    char* data_;  // 数据缓冲区
    size_t size_; // 缓冲区大小
};

#endif // WARLOCK_NETLINK_CONNECTION_H
#include "NetlinkConnection.h"
#include "../inc/utils/LogUtils.h"

NetlinkConnection::NetlinkConnection() {
    fd_ = -1;
    // 内核保持数据包在8KiB以下(NLMSG_GOODSIZE)
    size_ = 8192;
    data_ = new char[size_];
}

NetlinkConnection::~NetlinkConnection() {
    if (fd_ != -1) close(fd_);
    delete[] data_;
}

bool NetlinkConnection::SendRequest(int type) {
    if (data_ == nullptr) return false;

    // 如果还没有打开netlink socket，则创建一个
    if (fd_ == -1) {
        fd_ = socket(PF_NETLINK, SOCK_RAW | SOCK_CLOEXEC, NETLINK_ROUTE);
        if (fd_ == -1) return false;
    }

    // 构造并发送消息
    struct NetlinkMessage {
        nlmsghdr hdr;
        rtgenmsg msg;
    } request;

    memset(&request, 0, sizeof(request));
    request.hdr.nlmsg_flags = NLM_F_DUMP | NLM_F_REQUEST;
    request.hdr.nlmsg_type = type;
    request.hdr.nlmsg_len = sizeof(request);
    request.msg.rtgen_family = AF_UNSPEC;  // 所有协议族

    return (TEMP_FAILURE_RETRY(send(fd_, &request, sizeof(request), 0)) == sizeof(request));
}

bool NetlinkConnection::ReadResponses(void callback(void*, nlmsghdr*), void* out) {
    // 读取所有响应，将感兴趣的传给回调函数
    ssize_t bytes_read;
    struct iovec iov{};
    iov.iov_base = data_;
    iov.iov_len = size_;

    struct sockaddr_nl nladdr{};
    struct msghdr msg = {
            .msg_name = &nladdr,
            .msg_namelen = sizeof(nladdr),
            .msg_iov = &iov,
            .msg_iovlen = 1,
    };

    while ((bytes_read = TEMP_FAILURE_RETRY(raw_syscall(__NR_recvmsg, fd_, &msg, 0))) > 0) {
        auto* hdr = reinterpret_cast<nlmsghdr*>(msg.msg_iov->iov_base);

        for (; NLMSG_OK(hdr, static_cast<size_t>(bytes_read));
               hdr = NLMSG_NEXT(hdr, bytes_read)) {

            if (hdr->nlmsg_type == NLMSG_DONE) return true;

            if (hdr->nlmsg_type == NLMSG_ERROR) {
                auto* err = reinterpret_cast<nlmsgerr*>(NLMSG_DATA(hdr));
                errno = (hdr->nlmsg_len >= NLMSG_LENGTH(sizeof(nlmsgerr)))
                        ? -err->error : EIO;
                return false;
            }

            callback(out, hdr);  // 处理每条消息
        }
    }
    return false;
}
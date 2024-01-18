#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <asm/types.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include <string.h>
#include <poll.h>

#ifndef SOL_NETLINK
#define SOL_NETLINK 270
#endif

int main(int argc, char **argv) {
  const struct sockaddr_nl sa = {
    .nl_family	= AF_NETLINK,
    .nl_pid 	= 0,
    .nl_groups 	= 1,
  };
  char buff[8192 << 4];
  struct iovec iov = { buff, sizeof(buff) };
  struct msghdr msg = {
    .msg_name 		= (void*)&sa,
    .msg_namelen 	= sizeof(sa),
    .msg_iov 		= &iov,
    .msg_iovlen 	= 1,
    .msg_control	= NULL,
    .msg_controllen	= 0,
    .msg_flags		= 0,
  };
  int fd = socket(AF_NETLINK, SOCK_RAW|SOCK_CLOEXEC|SOCK_NONBLOCK, NETLINK_AUDIT);
  if (fd==-1) {
    perror("Cannot open netlink interface via socket()");
    exit(1);
  }
  int size = sizeof(buff);
  int ret = setsockopt(fd, SOL_SOCKET, SO_RCVBUF, &size, sizeof(size));
  /* we can live with an error here */

  ret = setsockopt(fd, SOL_NETLINK, NETLINK_ADD_MEMBERSHIP, &sa.nl_groups, sizeof(sa.nl_groups));
  /* it works on newer kernels, we ignore ret */

  int opt_on = 1;
  /* we won't get ENOBUFS */
  ret = setsockopt(fd, SOL_NETLINK, NETLINK_NO_ENOBUFS, &opt_on, sizeof(opt_on));

  ret = bind(fd, (const struct sockaddr*)&sa, sizeof(sa));
  if (ret==-1) {
    perror("Binding error via bind()");
    exit(2);
  }

  struct pollfd pollfd = { .fd = fd, .events = POLLIN, .revents = 0 };

  printf("Starting event polling...\n");

  while (1) {
    ret = poll(&pollfd, 1, 1000);
    if (ret==-1) {
      perror("Error in waiting for socket data via poll()");
      exit(3);
    }
    else if (ret==0) {
      //continue; /* unexpected timeout */
    }
    memset(buff, 0, sizeof(buff));
    ret = recvmsg(fd, &msg, MSG_DONTWAIT);
    if (ret==0) {
      printf("Kaudit shutdown. Exiting.\n");
      exit(0);
    }
    else if (ret > 0) {
      struct nlmsghdr *payload = (struct nlmsghdr*)buff;
      char *data = (char*)NLMSG_DATA(payload);
      printf("CODE: %d, DATA: %s\n", payload->nlmsg_type, data);
      if (payload->nlmsg_type = 1300) {
        /* syscall start */
        // https://docs.huihoo.com/doxygen/linux/kernel/3.7/include_2uapi_2linux_2audit_8h.html
      }
    }
    else if (errno==EAGAIN || errno==EWOULDBLOCK || errno==EINTR) {
      continue;
    }
    else if (ret==ENOBUFS) {
      printf("Buffer content lost, expect inconsistent event\n");
    }
    else {
      printf("errno is %d\n", errno);
      perror("recvmsg()");
      exit(4);
    }
  }
  return 0;
}
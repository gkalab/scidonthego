#ifndef __TCLMY_H__
#define __TCLMY_H__

  #include <unistd.h>
  #define PRINT_MEM(x) { int pid = getpid(); fprintf(stderr, "======== " x "\t"); \
  char cmd[256]; \
  sprintf(cmd, "more /proc/%d/status | grep VmData", pid); \
  system(cmd);}


extern int logMemory;

#endif

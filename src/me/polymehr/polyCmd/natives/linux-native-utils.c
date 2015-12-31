#include <jni.h>
#include "linux-native-utils.h"
#include <sys/ioctl.h>


JNIEXPORT jint JNICALL Java_me_polymehr_polyCmd_util_LinuxUtil_getTerminalColumns
  (JNIEnv *env, jclass cls) {

  struct winsize w;
  ioctl(0, TIOCGWINSZ, &w);

  return w.ws_col;
}

JNIEXPORT jint JNICALL Java_me_polymehr_polyCmd_util_LinuxUtil_getTerminalLines
  (JNIEnv *env, jclass cls) {

  struct winsize w;
  ioctl(0, TIOCGWINSZ, &w);

  return w.ws_row;
}

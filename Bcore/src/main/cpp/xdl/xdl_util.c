






















#include "xdl_util.h"

#include <android/api-level.h>
#include <ctype.h>
#include <inttypes.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/system_properties.h>

static int GetAndroidApiLevel() {
    char prop_value[PROP_VALUE_MAX];
    __system_property_get("ro.build.version.sdk", prop_value);
    return atoi(prop_value);
}

bool xdl_util_starts_with(const char *str, const char *start) {
  while (*str && *str == *start) {
    str++;
    start++;
  }

  return '\0' == *start;
}

bool xdl_util_ends_with(const char *str, const char *ending) {
  size_t str_len = strlen(str);
  size_t ending_len = strlen(ending);

  if (ending_len > str_len) return false;

  return 0 == strcmp(str + (str_len - ending_len), ending);
}

size_t xdl_util_trim_ending(char *start) {
  char *end = start + strlen(start);
  while (start < end && isspace((int)(*(end - 1)))) {
    end--;
    *end = '\0';
  }
  return (size_t)(end - start);
}

static int xdl_util_get_api_level_from_build_prop(void) {
  char buf[128];
  int api_level = -1;

  FILE *fp = fopen("/system/build.prop", "r");
  if (NULL == fp) goto end;

  while (fgets(buf, sizeof(buf), fp)) {
    if (xdl_util_starts_with(buf, "ro.build.version.sdk=")) {
      api_level = atoi(buf + 21);
      break;
    }
  }
  fclose(fp);

end:
  return (api_level > 0) ? api_level : -1;
}

int xdl_util_get_api_level(void) {
  static int xdl_util_api_level = -1;

  if (xdl_util_api_level < 0) {
    int api_level = GetAndroidApiLevel();
    if (api_level < 0)
      api_level = xdl_util_get_api_level_from_build_prop();  
    if (api_level < __ANDROID_API_J__) api_level = __ANDROID_API_J__;

    __atomic_store_n(&xdl_util_api_level, api_level, __ATOMIC_SEQ_CST);
  }

  return xdl_util_api_level;
}

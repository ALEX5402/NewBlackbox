






















#include "xdl_iterate.h"

#include <android/api-level.h>
#include <ctype.h>
#include <dlfcn.h>
#include <elf.h>
#include <inttypes.h>
#include <link.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/auxv.h>

#include "xdl.h"
#include "xdl_linker.h"
#include "xdl_util.h"



extern __attribute((weak)) int dl_iterate_phdr(int (*)(struct dl_phdr_info *, size_t, void *), void *);
extern __attribute((weak)) unsigned long int getauxval(unsigned long int);

static uintptr_t xdl_iterate_get_min_vaddr(struct dl_phdr_info *info) {
  uintptr_t min_vaddr = UINTPTR_MAX;
  for (size_t i = 0; i < info->dlpi_phnum; i++) {
    const ElfW(Phdr) *phdr = &(info->dlpi_phdr[i]);
    if (PT_LOAD == phdr->p_type) {
      if (min_vaddr > phdr->p_vaddr) min_vaddr = phdr->p_vaddr;
    }
  }
  return min_vaddr;
}

static int xdl_iterate_open_or_rewind_maps(FILE **maps) {
  if (NULL == *maps) {
    *maps = fopen("/proc/self/maps", "r");
    if (NULL == *maps) return -1;
  } else
    rewind(*maps);

  return 0;
}

static int xdl_iterate_get_pathname_from_maps(uintptr_t base, char *buf, size_t buf_len, FILE **maps) {
  
  if (0 != xdl_iterate_open_or_rewind_maps(maps)) return -1;  

  char line[1024];
  while (fgets(line, sizeof(line), *maps)) {
    
    uintptr_t start, end;
    if (2 != sscanf(line, "%" SCNxPTR "-%" SCNxPTR " r", &start, &end)) continue;
    if (base < start) break;  
    if (base >= end) continue;

    
    char *pathname = strchr(line, '/');
    if (NULL == pathname) break;  
    xdl_util_trim_ending(pathname);

    
    strlcpy(buf, pathname, buf_len);
    return 0;  
  }

  return -1;  
}

static int xdl_iterate_by_linker_cb(struct dl_phdr_info *info, size_t size, void *arg) {
  uintptr_t *pkg = (uintptr_t *)arg;
  xdl_iterate_phdr_cb_t cb = (xdl_iterate_phdr_cb_t)*pkg++;
  void *cb_arg = (void *)*pkg++;
  FILE **maps = (FILE **)*pkg++;
  uintptr_t linker_load_bias = *pkg++;
  int flags = (int)*pkg;

  
  if (0 == info->dlpi_addr || NULL == info->dlpi_name || '\0' == info->dlpi_name[0]) return 0;

  
  if (linker_load_bias == info->dlpi_addr) return 0;

  struct dl_phdr_info info_fixed;
  info_fixed.dlpi_addr = info->dlpi_addr;
  info_fixed.dlpi_name = info->dlpi_name;
  info_fixed.dlpi_phdr = info->dlpi_phdr;
  info_fixed.dlpi_phnum = info->dlpi_phnum;
  info = &info_fixed;

  
  if (NULL == info->dlpi_phdr || 0 == info->dlpi_phnum) {
    ElfW(Ehdr) *ehdr = (ElfW(Ehdr) *)info->dlpi_addr;
    info->dlpi_phdr = (ElfW(Phdr) *)(info->dlpi_addr + ehdr->e_phoff);
    info->dlpi_phnum = ehdr->e_phnum;
  }

  
  if ('/' != info->dlpi_name[0] && '[' != info->dlpi_name[0] && (0 != (flags & XDL_FULL_PATHNAME))) {
    
    uintptr_t min_vaddr = xdl_iterate_get_min_vaddr(info);
    if (UINTPTR_MAX == min_vaddr) return 0;  
    uintptr_t base = (uintptr_t)(info->dlpi_addr + min_vaddr);

    char buf[1024];
    if (0 != xdl_iterate_get_pathname_from_maps(base, buf, sizeof(buf), maps)) return 0;  

    info->dlpi_name = (const char *)buf;
  }

  
  return cb(info, size, cb_arg);
}

static uintptr_t xdl_iterate_get_linker_base(void) {
  if (NULL == getauxval) return 0;

  uintptr_t base = (uintptr_t)getauxval(AT_BASE);
  if (0 == base) return 0;
  if (0 != memcmp((void *)base, ELFMAG, SELFMAG)) return 0;

  return base;
}

static int xdl_iterate_do_callback(xdl_iterate_phdr_cb_t cb, void *cb_arg, uintptr_t base,
                                   const char *pathname, uintptr_t *load_bias) {
  ElfW(Ehdr) *ehdr = (ElfW(Ehdr) *)base;

  struct dl_phdr_info info;
  info.dlpi_name = pathname;
  info.dlpi_phdr = (const ElfW(Phdr) *)(base + ehdr->e_phoff);
  info.dlpi_phnum = ehdr->e_phnum;

  
  uintptr_t min_vaddr = xdl_iterate_get_min_vaddr(&info);
  if (UINTPTR_MAX == min_vaddr) return 0;  
  info.dlpi_addr = (ElfW(Addr))(base - min_vaddr);
  if (NULL != load_bias) *load_bias = info.dlpi_addr;

  return cb(&info, sizeof(struct dl_phdr_info), cb_arg);
}

static int xdl_iterate_by_linker(xdl_iterate_phdr_cb_t cb, void *cb_arg, int flags) {
  if (NULL == dl_iterate_phdr) return 0;

  int api_level = xdl_util_get_api_level();
  FILE *maps = NULL;
  int r;

  
  
  uintptr_t linker_load_bias = 0;
  uintptr_t linker_base = xdl_iterate_get_linker_base();
  if (0 != linker_base) {
    if (0 !=
        (r = xdl_iterate_do_callback(cb, cb_arg, linker_base, XDL_UTIL_LINKER_PATHNAME, &linker_load_bias)))
      return r;
  }

  
  uintptr_t pkg[5] = {(uintptr_t)cb, (uintptr_t)cb_arg, (uintptr_t)&maps, linker_load_bias, (uintptr_t)flags};
  if (__ANDROID_API_L__ == api_level || __ANDROID_API_L_MR1__ == api_level) xdl_linker_lock();
  r = dl_iterate_phdr(xdl_iterate_by_linker_cb, pkg);
  if (__ANDROID_API_L__ == api_level || __ANDROID_API_L_MR1__ == api_level) xdl_linker_unlock();

  if (NULL != maps) fclose(maps);
  return r;
}

#if (defined(__arm__) || defined(__i386__)) && __ANDROID_API__ < __ANDROID_API_L__
static int xdl_iterate_by_maps(xdl_iterate_phdr_cb_t cb, void *cb_arg) {
  FILE *maps = fopen("/proc/self/maps", "r");
  if (NULL == maps) return 0;

  int r = 0;
  char buf1[1024], buf2[1024];
  char *line = buf1;
  uintptr_t prev_base = 0;
  bool try_next_line = false;

  while (fgets(line, sizeof(buf1), maps)) {
    
    uintptr_t base, offset;
    char exec;
    if (3 != sscanf(line, "%" SCNxPTR "-%*" SCNxPTR " r%*c%cp %" SCNxPTR " ", &base, &exec, &offset))
      goto clean;

    if ('-' == exec && 0 == offset) {
      
      prev_base = base;
      line = (line == buf1 ? buf2 : buf1);
      try_next_line = true;
      continue;
    } else if (exec == 'x') {
      
      char *pathname = NULL;
      if (try_next_line && 0 != offset) {
        char *prev = (line == buf1 ? buf2 : buf1);
        char *prev_pathname = strchr(prev, '/');
        if (NULL == prev_pathname) goto clean;

        pathname = strchr(line, '/');
        if (NULL == pathname) goto clean;

        xdl_util_trim_ending(prev_pathname);
        xdl_util_trim_ending(pathname);
        if (0 != strcmp(prev_pathname, pathname)) goto clean;

        
        base = prev_base;
        offset = 0;
      }

      if (0 != offset) goto clean;

      
      if (NULL == pathname) {
        pathname = strchr(line, '/');
        if (NULL == pathname) goto clean;
        xdl_util_trim_ending(pathname);
      }

      if (0 != memcmp((void *)base, ELFMAG, SELFMAG)) goto clean;
      ElfW(Ehdr) *ehdr = (ElfW(Ehdr) *)base;
      struct dl_phdr_info info;
      info.dlpi_name = pathname;
      info.dlpi_phdr = (const ElfW(Phdr) *)(base + ehdr->e_phoff);
      info.dlpi_phnum = ehdr->e_phnum;

      
      if (0 != (r = xdl_iterate_do_callback(cb, cb_arg, base, pathname, NULL))) break;
    }

  clean:
    try_next_line = false;
  }

  fclose(maps);
  return r;
}
#endif

int xdl_iterate_phdr_impl(xdl_iterate_phdr_cb_t cb, void *cb_arg, int flags) {
  
#if (defined(__arm__) || defined(__i386__)) && __ANDROID_API__ < __ANDROID_API_L__
  if (xdl_util_get_api_level() < __ANDROID_API_L__) return xdl_iterate_by_maps(cb, cb_arg);
#endif

  
  return xdl_iterate_by_linker(cb, cb_arg, flags);
}

int xdl_iterate_get_full_pathname(uintptr_t base, char *buf, size_t buf_len) {
  FILE *maps = NULL;
  int r = xdl_iterate_get_pathname_from_maps(base, buf, buf_len, &maps);
  if (NULL != maps) fclose(maps);
  return r;
}

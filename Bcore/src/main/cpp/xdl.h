






























#ifndef IO_GITHUB_HEXHACKING_XDL
#define IO_GITHUB_HEXHACKING_XDL

#include <dlfcn.h>
#include <link.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  
  const char *dli_fname;  
  void *dli_fbase;        
  const char *dli_sname;  
  void *dli_saddr;        
  
  size_t dli_ssize;             
  const ElfW(Phdr) *dlpi_phdr;  
  size_t dlpi_phnum;            
} xdl_info_t;




#define XDL_DEFAULT 0x00




#define XDL_TRY_FORCE_LOAD    0x01
#define XDL_ALWAYS_FORCE_LOAD 0x02
void *xdl_open(const char *filename, int flags);
void *xdl_close(void *handle);
void *xdl_sym(void *handle, const char *symbol, size_t *symbol_size);
void *xdl_dsym(void *handle, const char *symbol, size_t *symbol_size);




int xdl_addr(void *addr, xdl_info_t *info, void **cache);
void xdl_addr_clean(void **cache);




#define XDL_FULL_PATHNAME 0x01
int xdl_iterate_phdr(int (*callback)(struct dl_phdr_info *, size_t, void *), void *data, int flags);




#define XDL_DI_DLINFO 1  
int xdl_info(void *handle, int request, void *info);

#ifdef __cplusplus
}
#endif

#endif

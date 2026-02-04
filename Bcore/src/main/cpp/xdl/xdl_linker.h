






















#ifndef IO_GITHUB_HEXHACKING_XDL_LINKER
#define IO_GITHUB_HEXHACKING_XDL_LINKER

#ifdef __cplusplus
extern "C" {
#endif

void xdl_linker_lock(void);
void xdl_linker_unlock(void);

void *xdl_linker_force_dlopen(const char *filename);

#ifdef __cplusplus
}
#endif

#endif

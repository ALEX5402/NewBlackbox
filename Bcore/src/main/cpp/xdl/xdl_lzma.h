






















#ifndef IO_GITHUB_HEXHACKING_XDL_LZMA
#define IO_GITHUB_HEXHACKING_XDL_LZMA

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int xdl_lzma_decompress(uint8_t *src, size_t src_size, uint8_t **dst, size_t *dst_size);

#ifdef __cplusplus
}
#endif

#endif

#pragma once
#include <windows.h>

namespace Renderer {
    typedef BOOL(WINAPI* twglSwapBuffers)(HDC hdc);
    extern twglSwapBuffers owglSwapBuffers;

    BOOL WINAPI hkwglSwapBuffers(HDC hdc);
    void Init();
}

#pragma once
#include <windows.h>
#include <vector>
#include <Psapi.h>

namespace Utils {
    // Escaneia a memria por um padro de bytes (ex: "48 89 5C 24 ? ? ? ?")
    inline uintptr_t PatternScan(const char* moduleName, const char* signature) {
        auto moduleHandle = GetModuleHandleA(moduleName);
        if (!moduleHandle) return 0;

        MODULEINFO moduleInfo;
        GetModuleInformation(GetCurrentProcess(), moduleHandle, &moduleInfo, sizeof(MODULEINFO));

        uintptr_t start = (uintptr_t)moduleHandle;
        uintptr_t end = start + moduleInfo.SizeOfImage;

        auto patternToByte = [](const char* pattern) {
            auto bytes = std::vector<int>{};
            auto start = const_cast<char*>(pattern);
            auto end = const_cast<char*>(pattern) + strlen(pattern);

            for (auto curr = start; curr < end; ++curr) {
                if (*curr == '?') {
                    ++curr;
                    if (*curr == '?') ++curr;
                    bytes.push_back(-1);
                } else {
                    bytes.push_back(strtoul(curr, &curr, 16));
                }
            }
            return bytes;
        };

        auto patternBytes = patternToByte(signature);
        auto size = patternBytes.size();
        auto data = patternBytes.data();

        for (uintptr_t i = start; i < end - size; ++i) {
            bool found = true;
            for (size_t j = 0; j < size; ++j) {
                if (data[j] != -1 && data[j] != *(uint8_t*)(i + j)) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }

        return 0;
    }
}

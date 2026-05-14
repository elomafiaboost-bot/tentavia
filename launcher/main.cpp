#define WIN32_LEAN_AND_MEAN
#define UNICODE
#define _UNICODE
#include <windows.h>
#include <windowsx.h>
#include <tlhelp32.h>
#include <shlwapi.h>
#include <string>

#pragma comment(lib, "user32.lib")
#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "shlwapi.lib")
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "advapi32.lib")
#pragma comment(linker, "/SUBSYSTEM:WINDOWS")

// ── Dimensões ──────────────────────────────────────────────────────────────
static constexpr int W = 360;
static constexpr int H = 230;

// ── Paleta ─────────────────────────────────────────────────────────────────
static constexpr COLORREF C_BG     = RGB( 10,  12,  20);
static constexpr COLORREF C_BORDER = RGB( 32,  38,  65);
static constexpr COLORREF C_TEXT   = RGB(220, 228, 252);
static constexpr COLORREF C_DIM    = RGB( 72,  82, 115);
static constexpr COLORREF C_BLUE   = RGB( 41, 121, 255);
static constexpr COLORREF C_PURP   = RGB(124,  77, 255);
static constexpr COLORREF C_GREEN  = RGB( 46, 213, 115);
static constexpr COLORREF C_RED    = RGB(235,  77,  75);
static constexpr COLORREF C_AMBER  = RGB(255, 196,  50);

// ── Estado ─────────────────────────────────────────────────────────────────
enum class Phase { Waiting, Injecting, Success, Error };

static HWND         g_wnd      = nullptr;
static Phase        g_phase    = Phase::Waiting;
static std::wstring g_errMsg;
static std::wstring g_dllPath;
static std::wstring g_procName = L"javaw.exe";
static bool         g_done     = false;
static bool         g_elevated = false;
static int          g_tick     = 0;
static bool         g_closeHov = false;
static POINT        g_dragOff  = {};
static bool         g_dragging = false;

// ── Helpers ────────────────────────────────────────────────────────────────
static bool CheckElevated() {
    BOOL ok = FALSE; HANDLE h = nullptr;
    if (OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &h)) {
        TOKEN_ELEVATION e{}; DWORD sz = sizeof(e);
        if (GetTokenInformation(h, TokenElevation, &e, sz, &sz)) ok = e.TokenIsElevated;
        CloseHandle(h);
    }
    return ok != FALSE;
}

static DWORD FindProc(const wchar_t* name) {
    HANDLE s = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (s == INVALID_HANDLE_VALUE) return 0;
    PROCESSENTRY32W pe{ sizeof(pe) };
    DWORD pid = 0;
    if (Process32FirstW(s, &pe)) do {
        if (!_wcsicmp(pe.szExeFile, name)) { pid = pe.th32ProcessID; break; }
    } while (Process32NextW(s, &pe));
    CloseHandle(s); return pid;
}

static std::wstring DefaultDll() {
    wchar_t d[MAX_PATH]; GetModuleFileNameW(nullptr, d, MAX_PATH);
    PathRemoveFileSpecW(d);
    return std::wstring(d) + L"\\tentavia.dll";
}

// ── Injeção (thread separada para não travar a UI) ─────────────────────────
static DWORD WINAPI InjectThread(LPVOID arg) {
    DWORD pid = (DWORD)(uintptr_t)arg;

    int sz = WideCharToMultiByte(CP_ACP, 0, g_dllPath.c_str(), -1, nullptr, 0, nullptr, nullptr);
    std::string a(sz, '\0');
    WideCharToMultiByte(CP_ACP, 0, g_dllPath.c_str(), -1, &a[0], sz, nullptr, nullptr);
    a.resize(sz - 1);

    HANDLE hp = OpenProcess(PROCESS_ALL_ACCESS, FALSE, pid);
    bool ok = false;
    if (hp) {
        void* p = VirtualAllocEx(hp, nullptr, a.size() + 1, MEM_COMMIT, PAGE_READWRITE);
        if (p) {
            WriteProcessMemory(hp, p, a.c_str(), a.size() + 1, nullptr);
            void* ll = (void*)GetProcAddress(GetModuleHandleW(L"kernel32.dll"), "LoadLibraryA");
            HANDLE ht = CreateRemoteThread(hp, nullptr, 0, (LPTHREAD_START_ROUTINE)ll, p, 0, nullptr);
            if (ht) {
                WaitForSingleObject(ht, 6000);
                DWORD c = 0; GetExitCodeThread(ht, &c); ok = (c != 0);
                CloseHandle(ht);
            }
            VirtualFreeEx(hp, p, 0, MEM_RELEASE);
        }
        CloseHandle(hp);
    }

    g_phase = ok ? Phase::Success : Phase::Error;
    if (!ok) g_errMsg = L"Falha. Verifique a DLL e permissões.";
    g_done = true;
    InvalidateRect(g_wnd, nullptr, FALSE);
    if (ok) SetTimer(g_wnd, 3, 2200, nullptr); // auto-close
    return 0;
}

// ── GDI ────────────────────────────────────────────────────────────────────
static HFONT Font(int size, bool bold = false, const wchar_t* face = L"Segoe UI") {
    return CreateFontW(size, 0, 0, 0, bold ? FW_BOLD : FW_NORMAL,
        FALSE, FALSE, FALSE, DEFAULT_CHARSET,
        OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH | FF_DONTCARE, face);
}

static void TXT(HDC dc, const wchar_t* t, RECT r, COLORREF c, HFONT f, UINT fl = DT_CENTER | DT_VCENTER | DT_SINGLELINE) {
    auto old = (HFONT)SelectObject(dc, f);
    SetTextColor(dc, c); DrawTextW(dc, t, -1, &r, fl);
    SelectObject(dc, old);
}

static void FILL(HDC dc, RECT r, COLORREF c) {
    HBRUSH b = CreateSolidBrush(c); FillRect(dc, &r, b); DeleteObject(b);
}

static void Dot(HDC dc, int cx, int cy, int r, COLORREF c, bool hollow = false) {
    HBRUSH b = hollow ? (HBRUSH)GetStockObject(NULL_BRUSH) : CreateSolidBrush(c);
    HPEN   p = CreatePen(PS_SOLID, 1, c);
    auto ob = (HBRUSH)SelectObject(dc, b); auto op = (HPEN)SelectObject(dc, p);
    Ellipse(dc, cx - r, cy - r, cx + r, cy + r);
    SelectObject(dc, ob); SelectObject(dc, op);
    if (!hollow) DeleteObject(b); DeleteObject(p);
}

// Simula glow com círculos concêntricos cada vez mais escuros
static void Glow(HDC dc, int cx, int cy, int r, COLORREF c) {
    for (int i = r + 8; i > r; i -= 2) {
        float t = (float)(i - r) / 8.0f;
        COLORREF d = RGB(
            (BYTE)(GetRValue(c) * (1.0f - t * 0.92f)),
            (BYTE)(GetGValue(c) * (1.0f - t * 0.92f)),
            (BYTE)(GetBValue(c) * (1.0f - t * 0.92f)));
        Dot(dc, cx, cy, i, d);
    }
    Dot(dc, cx, cy, r, c);
}

// ── Paint ──────────────────────────────────────────────────────────────────
static void Paint(HWND wnd) {
    PAINTSTRUCT ps; HDC rdc = BeginPaint(wnd, &ps);
    HDC dc = CreateCompatibleDC(rdc);
    HBITMAP bm = CreateCompatibleBitmap(rdc, W, H);
    auto obm = (HBITMAP)SelectObject(dc, bm);
    SetBkMode(dc, TRANSPARENT);

    // Fundo
    FILL(dc, {0, 0, W, H}, C_BG);

    // Linha de acento superior: azul → roxo
    FILL(dc, {16, 0, W / 2, 2}, C_BLUE);
    FILL(dc, {W / 2, 0, W - 16, 2}, C_PURP);

    // Borda sutil
    {
        auto bp = CreatePen(PS_SOLID, 1, C_BORDER);
        auto op = (HPEN)SelectObject(dc, bp);
        auto ob = (HBRUSH)SelectObject(dc, GetStockObject(NULL_BRUSH));
        RoundRect(dc, 0, 0, W, H, 14, 14);
        SelectObject(dc, op); SelectObject(dc, ob); DeleteObject(bp);
    }

    // Botão fechar ✕
    {
        COLORREF xCol = g_closeHov ? C_RED : C_DIM;
        auto f = Font(13);
        TXT(dc, L"✕", {W - 34, 7, W - 8, 27}, xCol, f);
        DeleteObject(f);
    }

    // Título principal
    {
        auto f = Font(25, true);
        // Mede largura para gradiente manual (usamos cor sólida branca aqui)
        TXT(dc, L"TENTAVIA", {0, 16, W, 52}, C_TEXT, f);
        DeleteObject(f);
    }

    // Subtítulo
    {
        auto f = Font(11);
        TXT(dc, L"Internal Framework", {0, 52, W, 68}, C_DIM, f);
        DeleteObject(f);
    }

    // Divisor horizontal
    {
        auto dp = CreatePen(PS_SOLID, 1, C_BORDER);
        auto op = (HPEN)SelectObject(dc, dp);
        MoveToEx(dc, 30, 75, nullptr); LineTo(dc, W - 30, 75);
        SelectObject(dc, op); DeleteObject(dp);
    }

    // ── Status ─────────────────────────────────────────────────────────
    static const wchar_t* dots[] = { L"", L".", L"..", L"..." };
    COLORREF sc; std::wstring st;
    switch (g_phase) {
    case Phase::Waiting:
        sc = C_BLUE;
        st = std::wstring(L"Aguardando Minecraft") + dots[g_tick % 4];
        break;
    case Phase::Injecting:
        sc = C_AMBER;
        st = L"Injetando DLL" + std::wstring(dots[g_tick % 4]);
        break;
    case Phase::Success:
        sc = C_GREEN;
        st = L"Injetado com sucesso!";
        break;
    case Phase::Error:
        sc = C_RED;
        st = g_errMsg.empty() ? L"Falha na injeção." : g_errMsg;
        break;
    }

    // Dot pulsando à esquerda do texto
    int dotR = 6 + (g_phase == Phase::Waiting ? g_tick % 2 : 0);
    Glow(dc, 54, 108, dotR, sc);

    // Texto de status
    {
        auto f = Font(13, true);
        TXT(dc, st.c_str(), {68, 96, W - 10, 122}, sc, f, DT_LEFT | DT_VCENTER | DT_SINGLELINE);
        DeleteObject(f);
    }

    // Info: DLL e processo
    {
        auto f = Font(11);
        std::wstring dll  = std::wstring(L"DLL: ") + PathFindFileNameW(g_dllPath.c_str());
        std::wstring proc = L"Target: " + g_procName;
        TXT(dc, dll.c_str(),  {0, 136, W, 152}, C_DIM, f);
        TXT(dc, proc.c_str(), {0, 152, W, 168}, C_DIM, f);
        DeleteObject(f);
    }

    // Alerta de não-admin
    if (!g_elevated) {
        auto f = Font(11);
        TXT(dc, L"⚠  Execute como Administrador", {0, 172, W, 188}, C_RED, f);
        DeleteObject(f);
    }

    // Watermark
    {
        auto f = Font(10);
        TXT(dc, L"github.com/elomafiaboost-bot/tentavia", {0, H - 18, W, H - 4}, RGB(26, 30, 50), f);
        DeleteObject(f);
    }

    BitBlt(rdc, 0, 0, W, H, dc, 0, 0, SRCCOPY);
    SelectObject(dc, obm); DeleteObject(bm); DeleteDC(dc);
    EndPaint(wnd, &ps);
}

// ── WndProc ────────────────────────────────────────────────────────────────
static LRESULT CALLBACK WndProc(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) {
    switch (msg) {
    case WM_CREATE:
        g_wnd      = wnd;
        g_dllPath  = DefaultDll();
        g_elevated = CheckElevated();
        SetTimer(wnd, 1, 450, nullptr); // animação
        SetTimer(wnd, 2, 900, nullptr); // poll
        break;

    case WM_TIMER:
        if (wp == 1) {
            g_tick++;
            InvalidateRect(wnd, nullptr, FALSE);
        } else if (wp == 2 && !g_done) {
            DWORD pid = FindProc(g_procName.c_str());
            if (pid) {
                KillTimer(wnd, 2);
                g_phase = Phase::Injecting;
                InvalidateRect(wnd, nullptr, FALSE);
                UpdateWindow(wnd);
                HANDLE ht = CreateThread(nullptr, 0, InjectThread, (LPVOID)(uintptr_t)pid, 0, nullptr);
                if (ht) CloseHandle(ht);
            }
        } else if (wp == 3) {
            KillTimer(wnd, 3);
            DestroyWindow(wnd);
        }
        break;

    case WM_PAINT: Paint(wnd); break;
    case WM_ERASEBKGND: return 1;

    case WM_MOUSEMOVE: {
        int x = GET_X_LPARAM(lp), y = GET_Y_LPARAM(lp);
        bool hov = (x >= W - 36 && y <= 30);
        if (hov != g_closeHov) { g_closeHov = hov; InvalidateRect(wnd, nullptr, FALSE); }

        // Rastreia saída do mouse para limpar hover
        TRACKMOUSEEVENT tme{ sizeof(tme), TME_LEAVE, wnd, 0 };
        TrackMouseEvent(&tme);

        if (g_dragging) {
            POINT cur; GetCursorPos(&cur);
            SetWindowPos(wnd, nullptr, cur.x - g_dragOff.x, cur.y - g_dragOff.y,
                0, 0, SWP_NOZORDER | SWP_NOSIZE);
        }
        break;
    }
    case WM_MOUSELEAVE:
        g_closeHov = false; InvalidateRect(wnd, nullptr, FALSE); break;

    case WM_LBUTTONDOWN: {
        int x = GET_X_LPARAM(lp), y = GET_Y_LPARAM(lp);
        if (x >= W - 36 && y <= 30) { DestroyWindow(wnd); break; }
        g_dragging = true;
        g_dragOff  = { x, y };
        SetCapture(wnd);
        break;
    }
    case WM_LBUTTONUP: g_dragging = false; ReleaseCapture(); break;

    case WM_DESTROY: PostQuitMessage(0); break;
    default: return DefWindowProcW(wnd, msg, wp, lp);
    }
    return 0;
}

// ── Entry point ────────────────────────────────────────────────────────────
int WINAPI WinMain(HINSTANCE hi, HINSTANCE, LPSTR, int) {
    WNDCLASSEXW wc{};
    wc.cbSize        = sizeof(wc);
    wc.lpfnWndProc   = WndProc;
    wc.hInstance     = hi;
    wc.hbrBackground = (HBRUSH)GetStockObject(NULL_BRUSH);
    wc.lpszClassName = L"TLauncher";
    wc.hCursor       = LoadCursorW(nullptr, IDC_ARROW);
    RegisterClassExW(&wc);

    int sx = GetSystemMetrics(SM_CXSCREEN), sy = GetSystemMetrics(SM_CYSCREEN);
    HWND wnd = CreateWindowExW(WS_EX_APPWINDOW, L"TLauncher", L"Tentavia",
        WS_POPUP, (sx - W) / 2, (sy - H) / 2, W, H,
        nullptr, nullptr, hi, nullptr);

    // Cantos arredondados via region (funciona em Win10 e Win11)
    HRGN rgn = CreateRoundRectRgn(0, 0, W + 1, H + 1, 14, 14);
    SetWindowRgn(wnd, rgn, FALSE);

    ShowWindow(wnd, SW_SHOW);
    UpdateWindow(wnd);

    MSG m;
    while (GetMessageW(&m, nullptr, 0, 0)) { TranslateMessage(&m); DispatchMessageW(&m); }
    return 0;
}

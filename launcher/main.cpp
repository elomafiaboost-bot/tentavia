#define WIN32_LEAN_AND_MEAN
#define UNICODE
#define _UNICODE
#include <windows.h>
#include <tlhelp32.h>
#include <shlwapi.h>
#include <commdlg.h>
#include <shellapi.h>
#include <string>
#include <vector>
#include <sstream>

#pragma comment(lib, "user32.lib")
#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "shlwapi.lib")
#pragma comment(lib, "comdlg32.lib")
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "advapi32.lib")
#pragma comment(linker, "/SUBSYSTEM:WINDOWS")

// ── IDs ────────────────────────────────────────────────────────────────────
#define IDC_PROCESS_EDIT   101
#define IDC_SCAN_BTN       102
#define IDC_PROCESS_LIST   103
#define IDC_DLL_EDIT       104
#define IDC_BROWSE_BTN     105
#define IDC_INJECT_BTN     106
#define IDC_LOG            107
#define IDC_HEADER         108

// ── Layout (pixels, área cliente 510×460) ─────────────────────────────────
#define W_CLIENT  510
#define H_CLIENT  462
#define PAD       10

// ── Estado global ──────────────────────────────────────────────────────────
static HWND g_hWnd          = nullptr;
static HWND g_hProcessEdit  = nullptr;
static HWND g_hProcessList  = nullptr;
static HWND g_hDllEdit      = nullptr;
static HWND g_hLog          = nullptr;
static HFONT g_hFontUI      = nullptr;
static HFONT g_hFontMono    = nullptr;
static HBRUSH g_hBrushHeader = nullptr;

static std::vector<DWORD> g_pids;
static bool g_elevated = false;

// ── Helpers ────────────────────────────────────────────────────────────────
static bool IsElevated() {
    BOOL ok = FALSE;
    HANDLE hTok = nullptr;
    if (OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &hTok)) {
        TOKEN_ELEVATION e; DWORD sz = sizeof(e);
        if (GetTokenInformation(hTok, TokenElevation, &e, sz, &sz))
            ok = e.TokenIsElevated;
        CloseHandle(hTok);
    }
    return ok != FALSE;
}

static void Log(const std::wstring& msg) {
    int len = GetWindowTextLengthW(g_hLog);
    SendMessageW(g_hLog, EM_SETSEL, len, len);
    SendMessageW(g_hLog, EM_REPLACESEL, FALSE, (LPARAM)(msg + L"\r\n").c_str());
    SendMessageW(g_hLog, EM_SCROLLCARET, 0, 0);
}

static std::wstring GetCtrlText(HWND h) {
    int n = GetWindowTextLengthW(h) + 1;
    std::wstring s(n, L'\0');
    GetWindowTextW(h, &s[0], n);
    s.resize(wcslen(s.c_str()));
    return s;
}

static HWND MakeCtrl(const wchar_t* cls, const wchar_t* txt, DWORD style,
                     int x, int y, int w, int h, int id) {
    HWND hC = CreateWindowExW(0, cls, txt, WS_CHILD | WS_VISIBLE | style,
        x, y, w, h, g_hWnd, (HMENU)(intptr_t)id,
        GetModuleHandleW(nullptr), nullptr);
    SendMessageW(hC, WM_SETFONT, (WPARAM)g_hFontUI, TRUE);
    return hC;
}

// ── Scan ───────────────────────────────────────────────────────────────────
static void ScanProcesses() {
    g_pids.clear();
    SendMessageW(g_hProcessList, LB_RESETCONTENT, 0, 0);

    std::wstring procName = GetCtrlText(g_hProcessEdit);
    if (procName.empty()) procName = L"javaw.exe";

    HANDLE hSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (hSnap == INVALID_HANDLE_VALUE) { Log(L"[-] Falha ao listar processos."); return; }

    PROCESSENTRY32W pe; pe.dwSize = sizeof(pe);
    int count = 0;
    if (Process32FirstW(hSnap, &pe)) {
        do {
            if (_wcsicmp(pe.szExeFile, procName.c_str()) == 0) {
                g_pids.push_back(pe.th32ProcessID);
                std::wstringstream ss;
                ss << pe.szExeFile << L"   PID: " << pe.th32ProcessID;
                SendMessageW(g_hProcessList, LB_ADDSTRING, 0, (LPARAM)ss.str().c_str());
                count++;
            }
        } while (Process32NextW(hSnap, &pe));
    }
    CloseHandle(hSnap);

    if (count == 0) {
        std::wstring msg = L"[-] Nenhum processo \"" + procName + L"\" encontrado.";
        Log(msg);
    } else {
        std::wstringstream ss;
        ss << L"[+] " << count << L" processo(s) encontrado(s).";
        Log(ss.str());
        SendMessageW(g_hProcessList, LB_SETCURSEL, 0, 0);
    }
}

// ── Browse DLL ─────────────────────────────────────────────────────────────
static void BrowseDLL() {
    wchar_t buf[MAX_PATH] = {};
    OPENFILENAMEW ofn = {};
    ofn.lStructSize = sizeof(ofn);
    ofn.hwndOwner   = g_hWnd;
    ofn.lpstrFilter = L"DLL Files\0*.dll\0All Files\0*.*\0";
    ofn.lpstrFile   = buf;
    ofn.nMaxFile    = MAX_PATH;
    ofn.Flags       = OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST;
    ofn.lpstrTitle  = L"Selecione a DLL do Tentavia";
    if (GetOpenFileNameW(&ofn))
        SetWindowTextW(g_hDllEdit, buf);
}

// ── Inject ─────────────────────────────────────────────────────────────────
static void InjectDLL() {
    int sel = (int)SendMessageW(g_hProcessList, LB_GETCURSEL, 0, 0);
    if (sel < 0 || sel >= (int)g_pids.size()) {
        Log(L"[-] Selecione um processo na lista."); return;
    }

    std::wstring wDll = GetCtrlText(g_hDllEdit);
    if (wDll.empty()) { Log(L"[-] Informe o caminho da DLL."); return; }
    if (!PathFileExistsW(wDll.c_str())) { Log(L"[-] DLL nao encontrada: " + wDll); return; }

    // Converte para char* (LoadLibraryA precisa de ANSI/UTF-8)
    int sz = WideCharToMultiByte(CP_ACP, 0, wDll.c_str(), -1, nullptr, 0, nullptr, nullptr);
    std::string dllA(sz, '\0');
    WideCharToMultiByte(CP_ACP, 0, wDll.c_str(), -1, &dllA[0], sz, nullptr, nullptr);
    dllA.resize(sz - 1);

    DWORD pid = g_pids[sel];
    {
        std::wstringstream ss; ss << L"[>] Injetando em PID " << pid << L"..."; Log(ss.str());
    }

    HANDLE hProc = OpenProcess(PROCESS_ALL_ACCESS, FALSE, pid);
    if (!hProc) {
        std::wstringstream ss;
        ss << L"[-] OpenProcess falhou (erro " << GetLastError() << L"). Execute como Administrador.";
        Log(ss.str()); return;
    }

    void* pBuf = VirtualAllocEx(hProc, nullptr, dllA.size() + 1, MEM_COMMIT, PAGE_READWRITE);
    if (!pBuf) {
        Log(L"[-] VirtualAllocEx falhou."); CloseHandle(hProc); return;
    }

    if (!WriteProcessMemory(hProc, pBuf, dllA.c_str(), dllA.size() + 1, nullptr)) {
        Log(L"[-] WriteProcessMemory falhou.");
        VirtualFreeEx(hProc, pBuf, 0, MEM_RELEASE); CloseHandle(hProc); return;
    }

    void* pLL = (void*)GetProcAddress(GetModuleHandleW(L"kernel32.dll"), "LoadLibraryA");
    HANDLE hThr = CreateRemoteThread(hProc, nullptr, 0,
        (LPTHREAD_START_ROUTINE)pLL, pBuf, 0, nullptr);

    if (hThr) {
        Log(L"[+] Thread remota criada. Aguardando...");
        WaitForSingleObject(hThr, 6000);
        DWORD code = 0; GetExitCodeThread(hThr, &code);
        CloseHandle(hThr);
        if (code)
            Log(L"[+] DLL injetada com sucesso!");
        else
            Log(L"[-] LoadLibraryA retornou NULL — verifique o caminho.");
    } else {
        std::wstringstream ss;
        ss << L"[-] CreateRemoteThread falhou (erro " << GetLastError() << L").";
        Log(ss.str());
    }

    VirtualFreeEx(hProc, pBuf, 0, MEM_RELEASE);
    CloseHandle(hProc);
}

// ── DLL path padrão (lado do launcher.exe) ────────────────────────────────
static std::wstring DefaultDllPath() {
    wchar_t dir[MAX_PATH];
    GetModuleFileNameW(nullptr, dir, MAX_PATH);
    PathRemoveFileSpecW(dir);
    return std::wstring(dir) + L"\\tentavia.dll";
}

// ── Centraliza janela na tela ──────────────────────────────────────────────
static void CenterWindow(HWND hWnd) {
    RECT rc; GetWindowRect(hWnd, &rc);
    int w = rc.right - rc.left, h = rc.bottom - rc.top;
    int sw = GetSystemMetrics(SM_CXSCREEN), sh = GetSystemMetrics(SM_CYSCREEN);
    SetWindowPos(hWnd, nullptr, (sw - w) / 2, (sh - h) / 2, 0, 0, SWP_NOZORDER | SWP_NOSIZE);
}

// ── WndProc ────────────────────────────────────────────────────────────────
static LRESULT CALLBACK WndProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {

    case WM_CREATE: {
        g_hWnd = hWnd;

        // Fontes
        g_hFontUI   = CreateFontW(15, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE,
            DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
        g_hFontMono = CreateFontW(13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE,
            DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, FIXED_PITCH | FF_DONTCARE, L"Consolas");

        // Brush do header azul escuro
        g_hBrushHeader = CreateSolidBrush(RGB(20, 30, 50));

        // ── Header band (pintado no WM_PAINT) ─────────────────────────────
        // apenas placeholders; texto real desenhado no WM_PAINT

        // ── Row 1: Processo ───────────────────────────────────────────────
        int y = 46;
        MakeCtrl(L"STATIC", L"Processo:", SS_LEFT, PAD, y + 3, 72, 18, 0);
        g_hProcessEdit = MakeCtrl(L"EDIT", L"javaw.exe",
            WS_BORDER | ES_AUTOHSCROLL, 85, y, 240, 24, IDC_PROCESS_EDIT);
        MakeCtrl(L"BUTTON", L"Scan", BS_PUSHBUTTON,
            333, y, 80, 24, IDC_SCAN_BTN);

        // ── Row 2: Lista de processos ─────────────────────────────────────
        y = 80;
        MakeCtrl(L"STATIC", L"Processos detectados:", SS_LEFT, PAD, y, 200, 18, 0);
        g_hProcessList = MakeCtrl(L"LISTBOX", L"",
            WS_BORDER | LBS_NOTIFY | WS_VSCROLL | LBS_NOINTEGRALHEIGHT,
            PAD, y + 20, W_CLIENT - PAD * 2, 84, IDC_PROCESS_LIST);

        // ── Row 3: DLL path ───────────────────────────────────────────────
        y = 196;
        MakeCtrl(L"STATIC", L"DLL:", SS_LEFT, PAD, y + 3, 35, 18, 0);
        g_hDllEdit = MakeCtrl(L"EDIT", DefaultDllPath().c_str(),
            WS_BORDER | ES_AUTOHSCROLL, 48, y, 342, 24, IDC_DLL_EDIT);
        MakeCtrl(L"BUTTON", L"Browse", BS_PUSHBUTTON,
            398, y, 100, 24, IDC_BROWSE_BTN);

        // ── Inject button ─────────────────────────────────────────────────
        HWND hInj = MakeCtrl(L"BUTTON", L"INJETAR",
            BS_PUSHBUTTON | BS_DEFPUSHBUTTON,
            PAD, 234, W_CLIENT - PAD * 2, 38, IDC_INJECT_BTN);
        // Deixa a fonte do botão de injetar um pouco maior/bold
        HFONT hFontBold = CreateFontW(15, 0, 0, 0, FW_BOLD, FALSE, FALSE, FALSE,
            DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
        SendMessageW(hInj, WM_SETFONT, (WPARAM)hFontBold, TRUE);

        // ── Log ───────────────────────────────────────────────────────────
        y = 284;
        MakeCtrl(L"STATIC", L"Log:", SS_LEFT, PAD, y, 40, 18, 0);
        g_hLog = MakeCtrl(L"EDIT", L"",
            WS_BORDER | ES_MULTILINE | ES_READONLY | ES_AUTOVSCROLL | WS_VSCROLL,
            PAD, y + 20, W_CLIENT - PAD * 2, 154, IDC_LOG);
        SendMessageW(g_hLog, WM_SETFONT, (WPARAM)g_hFontMono, TRUE);

        break;
    }

    // ── Header band personalizado ──────────────────────────────────────────
    case WM_PAINT: {
        PAINTSTRUCT ps;
        HDC hdc = BeginPaint(hWnd, &ps);

        // Fundo do header
        RECT rcHeader = { 0, 0, W_CLIENT, 38 };
        FillRect(hdc, &rcHeader, g_hBrushHeader);

        // Título
        SetBkMode(hdc, TRANSPARENT);
        SetTextColor(hdc, RGB(220, 230, 255));
        HFONT hFontTitle = CreateFontW(16, 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE,
            DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
        HFONT hOld = (HFONT)SelectObject(hdc, hFontTitle);
        RECT rcTitle = { PAD, 0, 300, 38 };
        DrawTextW(hdc, L"Tentavia Launcher", -1, &rcTitle, DT_LEFT | DT_VCENTER | DT_SINGLELINE);

        // Status de elevação
        if (g_elevated)
            SetTextColor(hdc, RGB(80, 220, 100));
        else
            SetTextColor(hdc, RGB(230, 80, 80));

        RECT rcStatus = { 0, 0, W_CLIENT - PAD, 38 };
        DrawTextW(hdc, g_elevated ? L"● ADMINISTRADOR" : L"● SEM ELEVAÇÃO", -1,
            &rcStatus, DT_RIGHT | DT_VCENTER | DT_SINGLELINE);

        SelectObject(hdc, hOld);
        DeleteObject(hFontTitle);
        EndPaint(hWnd, &ps);
        break;
    }

    // ── Cor do fundo das labels (casam com o fundo da janela) ─────────────
    case WM_CTLCOLORSTATIC: {
        HDC hdc = (HDC)wParam;
        SetBkMode(hdc, TRANSPARENT);
        SetTextColor(hdc, RGB(30, 30, 30));
        return (LRESULT)GetSysColorBrush(COLOR_BTNFACE);
    }

    case WM_COMMAND:
        switch (LOWORD(wParam)) {
        case IDC_SCAN_BTN:   ScanProcesses(); break;
        case IDC_BROWSE_BTN: BrowseDLL();     break;
        case IDC_INJECT_BTN: InjectDLL();     break;
        }
        break;

    case WM_DESTROY:
        DeleteObject(g_hFontUI);
        DeleteObject(g_hFontMono);
        DeleteObject(g_hBrushHeader);
        PostQuitMessage(0);
        break;

    default:
        return DefWindowProcW(hWnd, msg, wParam, lParam);
    }
    return 0;
}

// ── WinMain ────────────────────────────────────────────────────────────────
int WINAPI WinMain(HINSTANCE hInst, HINSTANCE, LPSTR, int nCmdShow) {
    g_elevated = IsElevated();

    WNDCLASSEXW wc    = {};
    wc.cbSize         = sizeof(wc);
    wc.style          = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc    = WndProc;
    wc.hInstance      = hInst;
    wc.hbrBackground  = (HBRUSH)(COLOR_BTNFACE + 1);
    wc.lpszClassName  = L"TentaviaLauncher";
    wc.hCursor        = LoadCursorW(nullptr, IDC_ARROW);
    wc.hIcon          = LoadIconW(nullptr, IDI_APPLICATION);
    RegisterClassExW(&wc);

    // Calcula tamanho da janela (área cliente fixa)
    RECT rc = { 0, 0, W_CLIENT, H_CLIENT };
    DWORD style = WS_OVERLAPPEDWINDOW & ~WS_THICKFRAME & ~WS_MAXIMIZEBOX;
    AdjustWindowRect(&rc, style, FALSE);

    g_hWnd = CreateWindowExW(0, L"TentaviaLauncher", L"Tentavia Launcher",
        style, CW_USEDEFAULT, CW_USEDEFAULT,
        rc.right - rc.left, rc.bottom - rc.top,
        nullptr, nullptr, hInst, nullptr);

    CenterWindow(g_hWnd);
    ShowWindow(g_hWnd, nCmdShow);
    UpdateWindow(g_hWnd);

    // Scan automático ao abrir
    ScanProcesses();

    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
    return (int)msg.wParam;
}

#include <windows.h>

/* Isolated reproduction of the observed HWSendData.cpp behavior.
   Not the vendor DLL. HWSendData.cpp names its entry DLLMain and links MFC;
   this file uses FindWindowA/SendMessageA. Indices 41-50 exist in that cpp
   but are absent from HWSendData.def, so they are not exported here. */
static int ipc_send(const char *cls, long a, long b, long c) {
    HWND hwnd;
    COPYDATASTRUCT cds;
    long data[3];
    hwnd = FindWindowA(cls, NULL);
    if (!hwnd) return 0;
    data[0] = a; data[1] = b; data[2] = c;
    cds.dwData = 0xCCCC;
    cds.cbData = (DWORD)sizeof(data);
    cds.lpData = data;
    SendMessageA(hwnd, WM_COPYDATA, 0, (LPARAM)&cds);
    return 1;
}
long __stdcall SendDataToHW(long a, long b, long c) { return ipc_send("HWClass", a, b, c); }
long __stdcall SendDataToHW1(long a, long b, long c) { return ipc_send("HWClass1", a, b, c); }
long __stdcall SendDataToHW2(long a, long b, long c) { return ipc_send("HWClass2", a, b, c); }
long __stdcall SendDataToHW3(long a, long b, long c) { return ipc_send("HWClass3", a, b, c); }
long __stdcall SendDataToHW4(long a, long b, long c) { return ipc_send("HWClass4", a, b, c); }
long __stdcall SendDataToHW5(long a, long b, long c) { return ipc_send("HWClass5", a, b, c); }
long __stdcall SendDataToHW6(long a, long b, long c) { return ipc_send("HWClass6", a, b, c); }
long __stdcall SendDataToHW7(long a, long b, long c) { return ipc_send("HWClass7", a, b, c); }
long __stdcall SendDataToHW8(long a, long b, long c) { return ipc_send("HWClass8", a, b, c); }
long __stdcall SendDataToHW9(long a, long b, long c) { return ipc_send("HWClass9", a, b, c); }
long __stdcall SendDataToHW10(long a, long b, long c) { return ipc_send("HWClass10", a, b, c); }
long __stdcall SendDataToHW11(long a, long b, long c) { return ipc_send("HWClass11", a, b, c); }
long __stdcall SendDataToHW12(long a, long b, long c) { return ipc_send("HWClass12", a, b, c); }
long __stdcall SendDataToHW13(long a, long b, long c) { return ipc_send("HWClass13", a, b, c); }
long __stdcall SendDataToHW14(long a, long b, long c) { return ipc_send("HWClass14", a, b, c); }
long __stdcall SendDataToHW15(long a, long b, long c) { return ipc_send("HWClass15", a, b, c); }
long __stdcall SendDataToHW16(long a, long b, long c) { return ipc_send("HWClass16", a, b, c); }
long __stdcall SendDataToHW17(long a, long b, long c) { return ipc_send("HWClass17", a, b, c); }
long __stdcall SendDataToHW18(long a, long b, long c) { return ipc_send("HWClass18", a, b, c); }
long __stdcall SendDataToHW19(long a, long b, long c) { return ipc_send("HWClass19", a, b, c); }
long __stdcall SendDataToHW20(long a, long b, long c) { return ipc_send("HWClass20", a, b, c); }
long __stdcall SendDataToHW21(long a, long b, long c) { return ipc_send("HWClass21", a, b, c); }
long __stdcall SendDataToHW22(long a, long b, long c) { return ipc_send("HWClass22", a, b, c); }
long __stdcall SendDataToHW23(long a, long b, long c) { return ipc_send("HWClass23", a, b, c); }
long __stdcall SendDataToHW24(long a, long b, long c) { return ipc_send("HWClass24", a, b, c); }
long __stdcall SendDataToHW25(long a, long b, long c) { return ipc_send("HWClass25", a, b, c); }
long __stdcall SendDataToHW26(long a, long b, long c) { return ipc_send("HWClass26", a, b, c); }
long __stdcall SendDataToHW27(long a, long b, long c) { return ipc_send("HWClass27", a, b, c); }
long __stdcall SendDataToHW28(long a, long b, long c) { return ipc_send("HWClass28", a, b, c); }
long __stdcall SendDataToHW29(long a, long b, long c) { return ipc_send("HWClass29", a, b, c); }
long __stdcall SendDataToHW30(long a, long b, long c) { return ipc_send("HWClass30", a, b, c); }
long __stdcall SendDataToHW31(long a, long b, long c) { return ipc_send("HWClass31", a, b, c); }
long __stdcall SendDataToHW32(long a, long b, long c) { return ipc_send("HWClass32", a, b, c); }
long __stdcall SendDataToHW33(long a, long b, long c) { return ipc_send("HWClass33", a, b, c); }
long __stdcall SendDataToHW34(long a, long b, long c) { return ipc_send("HWClass34", a, b, c); }
long __stdcall SendDataToHW35(long a, long b, long c) { return ipc_send("HWClass35", a, b, c); }
long __stdcall SendDataToHW36(long a, long b, long c) { return ipc_send("HWClass36", a, b, c); }
long __stdcall SendDataToHW37(long a, long b, long c) { return ipc_send("HWClass37", a, b, c); }
long __stdcall SendDataToHW38(long a, long b, long c) { return ipc_send("HWClass38", a, b, c); }
long __stdcall SendDataToHW39(long a, long b, long c) { return ipc_send("HWClass39", a, b, c); }
long __stdcall SendDataToHW40(long a, long b, long c) { return ipc_send("HWClass40", a, b, c); }
long __stdcall SendDataToHW51(long a, long b, long c) { return ipc_send("HWClass51", a, b, c); }
long __stdcall SendDataToHW52(long a, long b, long c) { return ipc_send("HWClass52", a, b, c); }
long __stdcall SendDataToHW53(long a, long b, long c) { return ipc_send("HWClass53", a, b, c); }
long __stdcall SendDataToHW54(long a, long b, long c) { return ipc_send("HWClass54", a, b, c); }
long __stdcall SendDataToHW55(long a, long b, long c) { return ipc_send("HWClass55", a, b, c); }
long __stdcall SendDataToHW56(long a, long b, long c) { return ipc_send("HWClass56", a, b, c); }
long __stdcall SendDataToHW57(long a, long b, long c) { return ipc_send("HWClass57", a, b, c); }
long __stdcall SendDataToHW58(long a, long b, long c) { return ipc_send("HWClass58", a, b, c); }
long __stdcall SendDataToHW59(long a, long b, long c) { return ipc_send("HWClass59", a, b, c); }
long __stdcall SendDataToHW60(long a, long b, long c) { return ipc_send("HWClass60", a, b, c); }
long __stdcall SendDataToHW61(long a, long b, long c) { return ipc_send("HWClass61", a, b, c); }
long __stdcall SendDataToHW62(long a, long b, long c) { return ipc_send("HWClass62", a, b, c); }
long __stdcall SendDataToHW63(long a, long b, long c) { return ipc_send("HWClass63", a, b, c); }
long __stdcall SendDataToHW64(long a, long b, long c) { return ipc_send("HWClass64", a, b, c); }
long __stdcall SendDataToHW65(long a, long b, long c) { return ipc_send("HWClass65", a, b, c); }
long __stdcall SendDataToHW66(long a, long b, long c) { return ipc_send("HWClass66", a, b, c); }
long __stdcall SendDataToHW67(long a, long b, long c) { return ipc_send("HWClass67", a, b, c); }
long __stdcall SendDataToHW68(long a, long b, long c) { return ipc_send("HWClass68", a, b, c); }
long __stdcall SendDataToHW69(long a, long b, long c) { return ipc_send("HWClass69", a, b, c); }
long __stdcall SendDataToHW70(long a, long b, long c) { return ipc_send("HWClass70", a, b, c); }
long __stdcall SendDataToHW71(long a, long b, long c) { return ipc_send("HWClass71", a, b, c); }
long __stdcall SendDataToHW72(long a, long b, long c) { return ipc_send("HWClass72", a, b, c); }
long __stdcall SendDataToHW73(long a, long b, long c) { return ipc_send("HWClass73", a, b, c); }
long __stdcall SendDataToHW74(long a, long b, long c) { return ipc_send("HWClass74", a, b, c); }
long __stdcall SendDataToHW75(long a, long b, long c) { return ipc_send("HWClass75", a, b, c); }
long __stdcall SendDataToHW76(long a, long b, long c) { return ipc_send("HWClass76", a, b, c); }
long __stdcall SendDataToHW77(long a, long b, long c) { return ipc_send("HWClass77", a, b, c); }
long __stdcall SendDataToHW78(long a, long b, long c) { return ipc_send("HWClass78", a, b, c); }
long __stdcall SendDataToHW79(long a, long b, long c) { return ipc_send("HWClass79", a, b, c); }
long __stdcall SendDataToHW80(long a, long b, long c) { return ipc_send("HWClass80", a, b, c); }
long __stdcall SendDataToHW81(long a, long b, long c) { return ipc_send("HWClass81", a, b, c); }
long __stdcall SendDataToHW82(long a, long b, long c) { return ipc_send("HWClass82", a, b, c); }
long __stdcall SendDataToHW83(long a, long b, long c) { return ipc_send("HWClass83", a, b, c); }
long __stdcall SendDataToHW84(long a, long b, long c) { return ipc_send("HWClass84", a, b, c); }
long __stdcall SendDataToHW85(long a, long b, long c) { return ipc_send("HWClass85", a, b, c); }
long __stdcall SendDataToHW86(long a, long b, long c) { return ipc_send("HWClass86", a, b, c); }
long __stdcall SendDataToHW87(long a, long b, long c) { return ipc_send("HWClass87", a, b, c); }
long __stdcall SendDataToHW88(long a, long b, long c) { return ipc_send("HWClass88", a, b, c); }
long __stdcall SendDataToHW89(long a, long b, long c) { return ipc_send("HWClass89", a, b, c); }
long __stdcall SendDataToHW90(long a, long b, long c) { return ipc_send("HWClass90", a, b, c); }
long __stdcall SendDataToHW91(long a, long b, long c) { return ipc_send("HWClass91", a, b, c); }
long __stdcall SendDataToHW92(long a, long b, long c) { return ipc_send("HWClass92", a, b, c); }
long __stdcall SendDataToHW93(long a, long b, long c) { return ipc_send("HWClass93", a, b, c); }
long __stdcall SendDataToHW94(long a, long b, long c) { return ipc_send("HWClass94", a, b, c); }
long __stdcall SendDataToHW95(long a, long b, long c) { return ipc_send("HWClass95", a, b, c); }
long __stdcall SendDataToHW96(long a, long b, long c) { return ipc_send("HWClass96", a, b, c); }
long __stdcall SendDataToHW97(long a, long b, long c) { return ipc_send("HWClass97", a, b, c); }
long __stdcall SendDataToHW98(long a, long b, long c) { return ipc_send("HWClass98", a, b, c); }
long __stdcall SendDataToHW99(long a, long b, long c) { return ipc_send("HWClass99", a, b, c); }
long __stdcall SendDataToHW100(long a, long b, long c) { return ipc_send("HWClass100", a, b, c); }

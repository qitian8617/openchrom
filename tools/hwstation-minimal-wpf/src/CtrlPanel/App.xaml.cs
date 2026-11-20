using System.Windows;

namespace HwMinimal.CtrlPanel;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        ShutdownMode = ShutdownMode.OnExplicitShutdown;
        if (!NativeShell.ClaimOrActivate())
        {
            Shutdown(0);
            return;
        }
        NativeShell.Show();
    }
}

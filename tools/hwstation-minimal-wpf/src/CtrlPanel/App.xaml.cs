using System.Windows;

namespace HwMinimal.CtrlPanel;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        ShutdownMode = ShutdownMode.OnExplicitShutdown;
        DispatcherUnhandledException += (_, args) =>
        {
            MessageBox.Show(args.Exception.ToString(), "反控");
            args.Handled = true;
            Shutdown(1);
        };
        if (!NativeShell.ClaimOrActivate())
        {
            Shutdown(0);
            return;
        }
        NativeShell.Show();
    }
}

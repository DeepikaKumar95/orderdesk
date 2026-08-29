using System;
using System.Configuration;
using System.Windows.Forms;
using OrderDesk.Core;
using Serilog;

namespace OrderDesk.WinForms
{
    static class Program
    {
        [STAThread]
        static void Main()
        {
            // Phase 2 addition: structured logging so we can see what the app actually does.
            Log.Logger = new LoggerConfiguration()
                .WriteTo.File("logs/orderdesk-.log", rollingInterval: RollingInterval.Day)
                .CreateLogger();

            // Phase 2/7 composition root: pick the repository implementation behind the strangler flag.
            bool useModern = bool.Parse(ConfigurationManager.AppSettings["UseModernApi"] ?? "false");
            IOrderRepository repo = useModern
                ? (IOrderRepository)new HttpOrderRepository(ConfigurationManager.AppSettings["ModernApiBaseUrl"])
                : new SqlOrderRepository(Globals.ConnectionString);
            var service = new OrderService(repo);

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new OrdersForm(service));
        }
    }
}

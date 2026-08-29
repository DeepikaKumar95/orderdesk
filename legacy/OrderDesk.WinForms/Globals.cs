using System.Configuration;

namespace OrderDesk.WinForms
{
    // LEGACY SMELL: static global state; every form reaches in here for a connection string.
    public static class Globals
    {
        public static string ConnectionString =>
            ConfigurationManager.ConnectionStrings["OrderDesk"].ConnectionString;
    }
}

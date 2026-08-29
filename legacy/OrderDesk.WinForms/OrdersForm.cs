using System;
using System.Data;
using System.Data.SqlClient;
using System.Windows.Forms;
using OrderDesk.Core;
using Serilog;

namespace OrderDesk.WinForms
{
    public class OrdersForm : Form
    {
        private readonly DataGridView _grid = new DataGridView { Dock = DockStyle.Fill, ReadOnly = true, AutoGenerateColumns = true };
        private readonly TextBox _customer = new TextBox { Dock = DockStyle.Top, Text = "C-1001" };
        private readonly TextBox _sku = new TextBox { Dock = DockStyle.Top, Text = "SKU-1" };
        private readonly NumericUpDown _qty = new NumericUpDown { Dock = DockStyle.Top, Minimum = 1, Value = 1 };
        private readonly NumericUpDown _price = new NumericUpDown { Dock = DockStyle.Top, DecimalPlaces = 2, Minimum = 0, Maximum = 100000, Value = 10 };
        private readonly Button _save = new Button { Dock = DockStyle.Top, Text = "Place order" };
        private readonly Button _refresh = new Button { Dock = DockStyle.Top, Text = "Refresh" };
        private readonly OrderService _service;

        public OrdersForm(OrderService service)
        {
            _service = service;
            Text = "OrderDesk (legacy)";
            Width = 900; Height = 600;
            Controls.AddRange(new Control[] { _grid, _refresh, _save, _price, _qty, _sku, _customer });
            _refresh.Click += Refresh_Click;
            _save.Click += Save_Click;
            Load += (s, e) => Refresh_Click(s, e);
        }

        // LEGACY SMELL (kept on purpose): synchronous ADO.NET on the UI thread, SELECT * with no paging.
        // This is the "screen freezes every morning" scenario. Phase 2 fix: async + paged repository call.
        private void Refresh_Click(object sender, EventArgs e)
        {
            var sw = System.Diagnostics.Stopwatch.StartNew();
            using (var conn = new SqlConnection(Globals.ConnectionString))
            using (var da = new SqlDataAdapter("SELECT order_id, customer_id, status, total, created_at FROM orders ORDER BY created_at DESC", conn))
            {
                var table = new DataTable();
                da.Fill(table);
                _grid.DataSource = table;
            }
            Log.Information("Orders grid loaded in {Ms} ms", sw.ElapsedMilliseconds);
        }

        // Phase 2 state: the credit-limit rule USED to live here (see git history); it now lives in
        // OrderDesk.Core.OrderService so it is testable and could be ported verbatim to Java.
        private async void Save_Click(object sender, EventArgs e)
        {
            _save.Enabled = false;
            try
            {
                var line = new OrderLine(_sku.Text, (int)_qty.Value, _price.Value);
                var id = await _service.PlaceOrderAsync(_customer.Text, new[] { line });
                Log.Information("Placed order {OrderId} via {Repo}", id, _service.RepositoryName);
                Refresh_Click(sender, e);
            }
            catch (InsufficientCreditException ex)
            {
                MessageBox.Show(ex.Message, "Rejected", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Place order failed");
                MessageBox.Show("Could not place order. See log.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally { _save.Enabled = true; }
        }
    }
}

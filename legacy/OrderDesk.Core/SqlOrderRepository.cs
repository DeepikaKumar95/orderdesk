using System;
using System.Collections.Generic;
using System.Data.SqlClient;
using System.Threading.Tasks;

namespace OrderDesk.Core
{
    /// Original ADO.NET path, now async and parameterized (the legacy code concatenated SQL strings).
    public class SqlOrderRepository : IOrderRepository
    {
        private readonly string _cs;
        public SqlOrderRepository(string connectionString) { _cs = connectionString; }
        public string Name => "ADO.NET";

        public async Task<Customer> GetCustomerAsync(string customerId)
        {
            using (var conn = new SqlConnection(_cs))
            using (var cmd = new SqlCommand("SELECT credit_limit FROM customers WHERE customer_id = @id", conn))
            {
                cmd.Parameters.AddWithValue("@id", customerId);
                await conn.OpenAsync();
                var limit = await cmd.ExecuteScalarAsync();
                return limit == null ? null : new Customer(customerId, (decimal)limit);
            }
        }

        public async Task<Guid> SaveOrderAsync(string customerId, IReadOnlyList<OrderLine> lines, decimal total)
        {
            var id = Guid.NewGuid();
            using (var conn = new SqlConnection(_cs))
            {
                await conn.OpenAsync();
                using (var tx = conn.BeginTransaction())
                {
                    var ins = new SqlCommand("INSERT INTO orders(order_id, customer_id, status, total) VALUES(@id, @c, 'PENDING', @t)", conn, tx);
                    ins.Parameters.AddWithValue("@id", id); ins.Parameters.AddWithValue("@c", customerId); ins.Parameters.AddWithValue("@t", total);
                    await ins.ExecuteNonQueryAsync();
                    int n = 1;
                    foreach (var l in lines)
                    {
                        var li = new SqlCommand("INSERT INTO order_lines(order_id, line_no, sku, qty, unit_price) VALUES(@id, @n, @s, @q, @p)", conn, tx);
                        li.Parameters.AddWithValue("@id", id); li.Parameters.AddWithValue("@n", n++); li.Parameters.AddWithValue("@s", l.Sku);
                        li.Parameters.AddWithValue("@q", l.Qty); li.Parameters.AddWithValue("@p", l.UnitPrice);
                        await li.ExecuteNonQueryAsync();
                    }
                    tx.Commit();
                }
            }
            // NOTE: no outbox row here -> the legacy write path never publishes OrderPlaced. That is exactly
            // why the strangler moves writes to the Java service before Kafka consumers can be trusted.
            return id;
        }
    }
}

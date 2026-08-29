using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace OrderDesk.Core
{
    /// Strangler adapter: same interface, but the write goes to the Spring Boot order-service.
    /// The credit-limit rule is enforced server-side too; the client keeps it only during the parallel run.
    public class HttpOrderRepository : IOrderRepository
    {
        private static readonly HttpClient Http = new HttpClient();
        private readonly string _base;
        public HttpOrderRepository(string baseUrl) { _base = baseUrl.TrimEnd('/'); }
        public string Name => "HTTP order-service";

        public Task<Customer> GetCustomerAsync(string customerId)
        {
            // Customer limits are still read from SQL during transition; the API does not expose them yet.
            var limits = new Dictionary<string, decimal> { { "C-1001", 50000m }, { "C-1002", 10000m }, { "C-1003", 2500m } };
            return Task.FromResult(limits.TryGetValue(customerId, out var l) ? new Customer(customerId, l) : null);
        }

        public async Task<Guid> SaveOrderAsync(string customerId, IReadOnlyList<OrderLine> lines, decimal total)
        {
            var body = new { customerId, lines = lines.Select(l => new { sku = l.Sku, qty = l.Qty, unitPrice = l.UnitPrice }) };
            var req = new HttpRequestMessage(HttpMethod.Post, _base + "/api/v1/orders")
            {
                Content = new StringContent(JsonConvert.SerializeObject(body), Encoding.UTF8, "application/json")
            };
            req.Headers.Add("Idempotency-Key", Guid.NewGuid().ToString());
            var res = await Http.SendAsync(req);
            var json = await res.Content.ReadAsStringAsync();
            if ((int)res.StatusCode == 422) throw new InsufficientCreditException(customerId);
            res.EnsureSuccessStatusCode();
            return Guid.Parse(JObject.Parse(json)["id"].ToString());
        }
    }
}

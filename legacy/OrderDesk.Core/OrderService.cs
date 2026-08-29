using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace OrderDesk.Core
{
    /// Business rules extracted from OrdersForm.Save_Click (Phase 2). This class is the contract the
    /// Java OrderService must satisfy; OrderDesk.Tests pins its behaviour.
    public class OrderService
    {
        private readonly IOrderRepository _repo;
        public OrderService(IOrderRepository repo) { _repo = repo; }
        public string RepositoryName => _repo.Name;

        public static decimal ComputeTotal(IReadOnlyList<OrderLine> lines) => lines.Sum(l => l.LineTotal);

        public async Task<Guid> PlaceOrderAsync(string customerId, IReadOnlyList<OrderLine> lines)
        {
            if (lines == null || lines.Count == 0) throw new ArgumentException("An order needs at least one line");
            var customer = await _repo.GetCustomerAsync(customerId) ?? throw new InvalidOperationException("Unknown customer " + customerId);
            var total = ComputeTotal(lines);
            if (total > customer.CreditLimit) throw new InsufficientCreditException(customerId);
            return await _repo.SaveOrderAsync(customerId, lines, total);
        }
    }
}

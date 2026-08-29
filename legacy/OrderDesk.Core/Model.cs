using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace OrderDesk.Core
{
    public class OrderLine
    {
        public OrderLine(string sku, int qty, decimal unitPrice) { Sku = sku; Qty = qty; UnitPrice = unitPrice; }
        public string Sku { get; }
        public int Qty { get; }
        public decimal UnitPrice { get; }
        public decimal LineTotal => Qty * UnitPrice;
    }

    public class Customer
    {
        public Customer(string id, decimal creditLimit) { Id = id; CreditLimit = creditLimit; }
        public string Id { get; }
        public decimal CreditLimit { get; }
    }

    public class InsufficientCreditException : Exception
    {
        public InsufficientCreditException(string customerId) : base("Order exceeds credit limit for customer " + customerId) { }
    }

    /// Boundary the WinForms app depends on. Two implementations: ADO.NET (legacy) and HTTP (strangler).
    public interface IOrderRepository
    {
        string Name { get; }
        Task<Customer> GetCustomerAsync(string customerId);
        Task<Guid> SaveOrderAsync(string customerId, IReadOnlyList<OrderLine> lines, decimal total);
    }
}

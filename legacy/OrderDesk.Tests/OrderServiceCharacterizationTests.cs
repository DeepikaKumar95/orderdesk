using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using OrderDesk.Core;
using Xunit;

namespace OrderDesk.Tests
{
    /// Characterization tests: these pin what the legacy Save_Click did, including the exact boundary
    /// (total == limit is allowed, total > limit is rejected). Same fixtures as the Java OrderServiceTest.
    public class OrderServiceCharacterizationTests
    {
        private class FakeRepo : IOrderRepository
        {
            public decimal? SavedTotal;
            public string Name => "fake";
            public Task<Customer> GetCustomerAsync(string id) => Task.FromResult(id == "C-1003" ? new Customer(id, 2500m) : null);
            public Task<Guid> SaveOrderAsync(string c, IReadOnlyList<OrderLine> l, decimal total) { SavedTotal = total; return Task.FromResult(Guid.NewGuid()); }
        }

        [Fact]
        public async Task TotalIsSumOfLineTotals()
        {
            var repo = new FakeRepo();
            await new OrderService(repo).PlaceOrderAsync("C-1003", new[] { new OrderLine("SKU-1", 2, 100m), new OrderLine("SKU-2", 1, 50.5m) });
            Assert.Equal(250.5m, repo.SavedTotal);
        }

        [Fact]
        public async Task ExactlyAtCreditLimitIsAccepted()
        {
            var repo = new FakeRepo();
            await new OrderService(repo).PlaceOrderAsync("C-1003", new[] { new OrderLine("SKU-1", 25, 100m) });
            Assert.Equal(2500m, repo.SavedTotal);
        }

        [Fact]
        public async Task OverCreditLimitIsRejectedAndNothingSaved()
        {
            var repo = new FakeRepo();
            await Assert.ThrowsAsync<InsufficientCreditException>(() =>
                new OrderService(repo).PlaceOrderAsync("C-1003", new[] { new OrderLine("SKU-1", 30, 100m) }));
            Assert.Null(repo.SavedTotal);
        }

        [Fact]
        public async Task EmptyOrderIsRejected()
        {
            await Assert.ThrowsAsync<ArgumentException>(() => new OrderService(new FakeRepo()).PlaceOrderAsync("C-1003", new OrderLine[0]));
        }
    }
}

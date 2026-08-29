import { test, expect } from '@playwright/test';

test('placing an order shows it in the list and inventory reserves it', async ({ page, request }) => {
  await page.goto('/');
  await page.getByTestId('customer').selectOption('C-1001');
  await page.getByTestId('sku').fill('SKU-1');
  await page.getByTestId('qty').fill('2');
  await page.getByTestId('price').fill('10');
  await page.getByTestId('add-line').click();
  await expect(page.getByTestId('total')).toHaveText('$20.00');

  await page.getByTestId('place-order').click();
  await expect(page.getByTestId('created')).toBeVisible();
  const id = (await page.getByTestId('created').innerText()).replace('Created order ', '').trim();

  // list refreshes and shows the new order
  const row = page.locator(`tr[data-order-id="${id}"]`);
  await expect(row).toBeVisible();

  // eventually consistent: order -> outbox -> Kafka -> inventory -> reply -> RESERVED
  await expect.poll(async () => {
    const res = await request.get(`http://localhost:8080/api/v1/orders/${id}`);
    return (await res.json()).status;
  }, { timeout: 15_000 }).toBe('RESERVED');
});

test('an order over the credit limit is rejected with a problem detail', async ({ page }) => {
  await page.goto('/');
  await page.getByTestId('customer').selectOption('C-1003');
  await page.getByTestId('sku').fill('SKU-1');
  await page.getByTestId('qty').fill('100');
  await page.getByTestId('price').fill('100');
  await page.getByTestId('add-line').click();
  await page.getByTestId('place-order').click();
  await expect(page.getByTestId('error')).toContainText('credit limit');
});

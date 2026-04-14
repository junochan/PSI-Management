import { printJson, readBodyFromOptions, parseQueryJson } from '../lib/util.mjs'

export function registerSales (program, { getApi }) {
  const sales = program.command('sales').description('销售：订单、付款/发货/收货、统计')

  const orders = sales.command('orders').description('销售订单')

  orders
    .command('list')
    .description('GET /sales/orders')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/sales/orders', { params: parseQueryJson(opts.query) }))
    })

  orders
    .command('get <id>')
    .description('GET /sales/orders/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/sales/orders/${id}`))
    })

  orders
    .command('create')
    .description('POST /sales/orders')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/sales/orders', body)
      printJson({ ok: true })
    })

  orders
    .command('update <id>')
    .description('PUT /sales/orders/:id')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`/sales/orders/${id}`, body)
      printJson({ ok: true })
    })

  orders
    .command('payment <id>')
    .description('PUT /sales/orders/:id/payment 确认付款')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).put(`/sales/orders/${id}/payment`)
      printJson({ ok: true })
    })

  orders
    .command('shipping <id>')
    .description('POST /sales/orders/:id/shipping 确认发货（ShippingDTO）')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post(`/sales/orders/${id}/shipping`, body)
      printJson({ ok: true })
    })

  orders
    .command('received <id>')
    .description('PUT /sales/orders/:id/received 确认收货')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).put(`/sales/orders/${id}/received`)
      printJson({ ok: true })
    })

  orders
    .command('cancel <id>')
    .description('PUT /sales/orders/:id/cancel')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).put(`/sales/orders/${id}/cancel`)
      printJson({ ok: true })
    })

  sales
    .command('stats')
    .description('GET /sales/stats')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/sales/stats'))
    })
}

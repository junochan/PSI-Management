import { printJson, readBodyFromOptions, parseQueryJson } from '../lib/util.mjs'

export function registerPurchase (program, { getApi }) {
  const purchase = program.command('purchase').description('采购：订单、入库、统计')

  const orders = purchase.command('orders').description('采购订单')

  orders
    .command('list')
    .description('GET /purchase/orders')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/purchase/orders', { params: parseQueryJson(opts.query) }))
    })

  orders
    .command('get <id>')
    .description('GET /purchase/orders/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/purchase/orders/${id}`))
    })

  orders
    .command('create')
    .description('POST /purchase/orders')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/purchase/orders', body)
      printJson({ ok: true })
    })

  orders
    .command('update <id>')
    .description('PUT /purchase/orders/:id')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`/purchase/orders/${id}`, body)
      printJson({ ok: true })
    })

  orders
    .command('cancel <id>')
    .description('PUT /purchase/orders/:id/cancel')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).put(`/purchase/orders/${id}/cancel`)
      printJson({ ok: true })
    })

  orders
    .command('delete <id>')
    .description('DELETE /purchase/orders/:id')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).delete(`/purchase/orders/${id}`)
      printJson({ ok: true })
    })

  orders
    .command('inbound <id>')
    .description('POST /purchase/orders/:id/inbound（采购入库）')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post(`/purchase/orders/${id}/inbound`, body)
      printJson({ ok: true })
    })

  const inbounds = purchase.command('inbounds').description('采购入库单查询')

  inbounds
    .command('list')
    .description('GET /purchase/inbounds')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/purchase/inbounds', { params: parseQueryJson(opts.query) }))
    })

  inbounds
    .command('get <id>')
    .description('GET /purchase/inbounds/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/purchase/inbounds/${id}`))
    })

  purchase
    .command('stats')
    .description('GET /purchase/stats 采购统计')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/purchase/stats'))
    })
}

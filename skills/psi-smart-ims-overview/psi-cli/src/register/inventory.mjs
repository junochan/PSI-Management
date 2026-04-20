import fs from 'fs'
import FormData from 'form-data'
import { printJson, readBodyFromOptions, parseQueryJson } from '../lib/util.mjs'

export function registerInventory (program, { getApi }) {
  const inv = program.command('inventory').description('库存：查询、调拨、预警、其它出入库、属性、以图搜图')

  inv
    .command('list')
    .description('GET /inventory')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/inventory', { params: parseQueryJson(opts.query) }))
    })

  inv
    .command('get <id>')
    .description('GET /inventory/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/inventory/${id}`))
    })

  inv
    .command('by-product <productId>')
    .description('GET /inventory/product/:productId')
    .action(async (productId, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/inventory/product/${productId}`))
    })

  inv
    .command('search-image')
    .description('POST /inventory/search-by-image（multipart，字段 image + 可选筛选字段）')
    .requiredOption('--image <path>', '查询图片路径（multipart 字段 image）')
    .option('-d, --data <json>', 'JSON（不含 image 字段）')
    .option('-f, --file <path>', 'JSON 文件（不含 image 字段）')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts) || {}
      const form = new FormData()
      form.append('image', fs.createReadStream(opts.image))
      for (const [k, v] of Object.entries(body)) {
        if (v === undefined || v === null) continue
        form.append(k, String(v))
      }
      printJson(await getApi(cmd).post('/inventory/search-by-image', form, {
        headers: form.getHeaders(),
        maxBodyLength: Infinity,
        maxContentLength: Infinity,
        timeout: 120000
      }))
    })

  const transfers = inv.command('transfers').description('库存调拨')

  transfers
    .command('list')
    .description('GET /inventory/transfers')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/inventory/transfers', { params: parseQueryJson(opts.query) }))
    })

  transfers
    .command('create')
    .description('POST /inventory/transfers')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/inventory/transfers', body)
      printJson({ ok: true })
    })

  transfers
    .command('confirm <id>')
    .description('PUT /inventory/transfers/:id/confirm')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).put(`/inventory/transfers/${id}/confirm`)
      printJson({ ok: true })
    })

  const warnings = inv.command('warnings').description('库存预警')

  warnings
    .command('list')
    .description('GET /inventory/warnings')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/inventory/warnings', { params: parseQueryJson(opts.query) }))
    })

  warnings
    .command('handle <id>')
    .description('PUT /inventory/warnings/:id/handle')
    .requiredOption('--remark <text>', 'handleRemark')
    .action(async (id, opts, cmd) => {
      await getApi(cmd).put(`/inventory/warnings/${id}/handle`, null, {
        params: { handleRemark: opts.remark }
      })
      printJson({ ok: true })
    })

  inv
    .command('stats')
    .description('GET /inventory/stats')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/inventory/stats'))
    })

  inv
    .command('inbound-simple')
    .description('POST /inventory/inbound（query: inventoryId, quantity, remark）')
    .requiredOption('--inventory-id <id>', '库存记录 id')
    .requiredOption('--quantity <n>', '数量')
    .option('--remark <text>', '备注')
    .action(async (opts, cmd) => {
      await getApi(cmd).post('/inventory/inbound', null, {
        params: {
          inventoryId: opts.inventoryId,
          quantity: opts.quantity,
          remark: opts.remark
        }
      })
      printJson({ ok: true })
    })

  inv
    .command('outbound-simple')
    .description('POST /inventory/outbound（query: inventoryId, quantity, remark）')
    .requiredOption('--inventory-id <id>', '库存记录 id')
    .requiredOption('--quantity <n>', '数量')
    .option('--remark <text>', '备注')
    .action(async (opts, cmd) => {
      await getApi(cmd).post('/inventory/outbound', null, {
        params: {
          inventoryId: opts.inventoryId,
          quantity: opts.quantity,
          remark: opts.remark
        }
      })
      printJson({ ok: true })
    })

  inv
    .command('inbound-new')
    .description('POST /inventory/inbound/new')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/inventory/inbound/new', body)
      printJson({ ok: true })
    })

  inv
    .command('outbounds-list')
    .description('GET /inventory/outbounds')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/inventory/outbounds', { params: parseQueryJson(opts.query) }))
    })

  inv
    .command('set-safe-stock <inventoryId>')
    .description('PUT /inventory/:id/safe-stock')
    .requiredOption('--safe-stock <n>', '安全库存')
    .action(async (inventoryId, opts, cmd) => {
      await getApi(cmd).put(`/inventory/${inventoryId}/safe-stock`, null, {
        params: { safeStock: opts.safeStock }
      })
      printJson({ ok: true })
    })

  inv
    .command('set-location <inventoryId>')
    .description('PUT /inventory/:id/location')
    .requiredOption('--location <text>', '库位')
    .action(async (inventoryId, opts, cmd) => {
      await getApi(cmd).put(`/inventory/${inventoryId}/location`, null, {
        params: { location: opts.location }
      })
      printJson({ ok: true })
    })

  inv
    .command('set-stagnant-days <inventoryId>')
    .description('PUT /inventory/:id/stagnant-days')
    .requiredOption('--days <n>', '呆滞预警天数')
    .action(async (inventoryId, opts, cmd) => {
      await getApi(cmd).put(`/inventory/${inventoryId}/stagnant-days`, null, {
        params: { stagnantDays: opts.days }
      })
      printJson({ ok: true })
    })
}

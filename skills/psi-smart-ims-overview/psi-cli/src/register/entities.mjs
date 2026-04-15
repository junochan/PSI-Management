import { printJson, readBodyFromOptions, parseQueryJson } from '../lib/util.mjs'

function attachCrud (parent, base, { getApi }) {
  parent.description(base.label)

  parent
    .command('list')
    .description(`GET ${base.path} 分页列表`)
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get(base.path, { params: parseQueryJson(opts.query) }))
    })

  parent
    .command('get <id>')
    .description(`GET ${base.path}/:id`)
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`${base.path}/${id}`))
    })

  parent
    .command('create')
    .description(`POST ${base.path}`)
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post(base.path, body)
      printJson({ ok: true })
    })

  parent
    .command('update <id>')
    .description(`PUT ${base.path}/:id`)
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`${base.path}/${id}`, body)
      printJson({ ok: true })
    })

  parent
    .command('delete <id>')
    .description(`DELETE ${base.path}/:id`)
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).delete(`${base.path}/${id}`)
      printJson({ ok: true })
    })

  if (base.batchPath) {
    parent
      .command('batch-delete')
      .description(`DELETE ${base.batchPath}，Body 为 id 数组`)
      .option('-d, --data <json>', '如 [1,2,3]')
      .option('-f, --file <path>', 'JSON 文件')
      .action(async (opts, cmd) => {
        const body = readBodyFromOptions(opts)
        if (!body) throw new Error('请提供 --data 或 --file')
        await getApi(cmd).delete(base.batchPath, { data: body })
        printJson({ ok: true })
      })
  }
}

export function registerEntities (program, ctx) {
  const { getApi } = ctx

  attachCrud(program.command('suppliers'), { path: '/suppliers', batchPath: '/suppliers/batch', label: '供应商' }, {
    getApi
  })

  attachCrud(program.command('customers'), { path: '/customers', batchPath: '/customers/batch', label: '客户' }, {
    getApi
  })

  const warehouses = program.command('warehouses').description('仓库')
  warehouses
    .command('options')
    .description('GET /warehouses/options 下拉（id/编码/名称，无统计）')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/warehouses/options'))
    })
  warehouses
    .command('list')
    .description('GET /warehouses')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/warehouses', { params: parseQueryJson(opts.query) }))
    })
  warehouses
    .command('all')
    .description('GET /warehouses/all 全部仓库（下拉）')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/warehouses/all'))
    })
  warehouses
    .command('get <id>')
    .description('GET /warehouses/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/warehouses/${id}`))
    })
  warehouses
    .command('create')
    .description('POST /warehouses')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/warehouses', body)
      printJson({ ok: true })
    })
  warehouses
    .command('update <id>')
    .description('PUT /warehouses/:id')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`/warehouses/${id}`, body)
      printJson({ ok: true })
    })
  warehouses
    .command('delete <id>')
    .description('DELETE /warehouses/:id')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).delete(`/warehouses/${id}`)
      printJson({ ok: true })
    })
  warehouses
    .command('batch-delete')
    .description('DELETE /warehouses/batch')
    .option('-d, --data <json>', 'id 数组')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).delete('/warehouses/batch', { data: body })
      printJson({ ok: true })
    })

  program
    .command('supplier-industries')
    .description('GET /supplier-industries 供应商行业字典（下拉）')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/supplier-industries'))
    })
}

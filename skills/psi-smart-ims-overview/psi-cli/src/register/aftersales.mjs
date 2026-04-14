import { printJson, readBodyFromOptions, parseQueryJson } from '../lib/util.mjs'

export function registerAftersales (program, { getApi }) {
  const a = program.command('aftersales').description('售后')

  a.command('list')
    .description('GET /aftersales')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/aftersales', { params: parseQueryJson(opts.query) }))
    })

  a.command('get <id>')
    .description('GET /aftersales/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/aftersales/${id}`))
    })

  a.command('create')
    .description('POST /aftersales')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/aftersales', body)
      printJson({ ok: true })
    })

  a.command('handle <id>')
    .description('PUT /aftersales/:id/handle')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`/aftersales/${id}/handle`, body)
      printJson({ ok: true })
    })

  a.command('close <id>')
    .description('PUT /aftersales/:id/close')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).put(`/aftersales/${id}/close`)
      printJson({ ok: true })
    })
}

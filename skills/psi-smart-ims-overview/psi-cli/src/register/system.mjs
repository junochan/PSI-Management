import { printJson, readBodyFromOptions, parseQueryJson, getRootOptions } from '../lib/util.mjs'
import { fetchUtilEncode } from '../lib/client.mjs'

export function registerSystem (program, { getApi }) {
  const users = program.command('users').description('系统用户')

  users
    .command('list')
    .description('GET /users')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/users', { params: parseQueryJson(opts.query) }))
    })

  users
    .command('all')
    .description('GET /users/all')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/users/all'))
    })

  users
    .command('get <id>')
    .description('GET /users/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/users/${id}`))
    })

  users
    .command('create')
    .description('POST /users')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/users', body)
      printJson({ ok: true })
    })

  users
    .command('update <id>')
    .description('PUT /users/:id')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`/users/${id}`, body)
      printJson({ ok: true })
    })

  users
    .command('delete <id>')
    .description('DELETE /users/:id')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).delete(`/users/${id}`)
      printJson({ ok: true })
    })

  const roles = program.command('roles').description('角色与权限')

  roles
    .command('list')
    .description('GET /roles')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/roles'))
    })

  roles
    .command('get <id>')
    .description('GET /roles/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/roles/${id}`))
    })

  roles
    .command('create')
    .description('POST /roles')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/roles', body)
      printJson({ ok: true })
    })

  roles
    .command('update <id>')
    .description('PUT /roles/:id')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`/roles/${id}`, body)
      printJson({ ok: true })
    })

  roles
    .command('delete <id>')
    .description('DELETE /roles/:id')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).delete(`/roles/${id}`)
      printJson({ ok: true })
    })

  roles
    .command('permissions <id>')
    .description('GET /roles/:id/permissions')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/roles/${id}/permissions`))
    })

  roles
    .command('permissions-all')
    .description('GET /roles/permissions')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/roles/permissions'))
    })

  roles
    .command('set-permissions <id>')
    .description('PUT /roles/:id/permissions，Body 为权限 id 数组')
    .option('-d, --data <json>', '如 [1,2,3]')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`/roles/${id}/permissions`, body)
      printJson({ ok: true })
    })

  program
    .command('logs')
    .description('操作日志 GET /logs')
    .option('-q, --query <json>', '查询 JSON', '{}')
    .action(async (opts, cmd) => {
      printJson(await getApi(cmd).get('/logs', { params: parseQueryJson(opts.query) }))
    })

  program
    .command('util-encode')
    .description('GET /util/encode?password= 生成 BCrypt（仅用于测试/运维）')
    .requiredOption('-p, --password <pwd>', '明文密码')
    .action(async (opts, cmd) => {
      const root = getRootOptions(cmd)
      const base =
        root.baseUrl || process.env.PSI_API_BASE || 'http://localhost:8080/api/v1'
      const text = await fetchUtilEncode(base, opts.password)
      console.log(text)
    })
}

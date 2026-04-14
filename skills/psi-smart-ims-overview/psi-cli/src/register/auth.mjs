import { getRootOptions, printJson } from '../lib/util.mjs'
import { createApi, saveToken, clearToken, TOKEN_PATH } from '../lib/client.mjs'

export function registerAuth (program, { getApi }) {
  const auth = program.command('auth').description('认证：登录获取 JWT，登出；登录成功默认写入 ~/.psi-smart-ims/token')

  auth
    .command('login <username>')
    .description('调用 POST /auth/login；默认把 JWT 写入本机供后续子命令使用')
    .option('-p, --password <pwd>', '密码（不传则在终端询问）')
    .option('--remember', '对应后端 remember', false)
    .option('--no-save', '不把 token 写入文件（仍打印登录 JSON）')
    .action(async (username, opts, cmd) => {
      let password = opts.password
      if (!password) {
        const readline = await import('readline')
        const rl = readline.createInterface({ input: process.stdin, output: process.stderr })
        password = await new Promise((resolve) => {
          rl.question('Password: ', (p) => {
            rl.close()
            resolve(p)
          })
        })
      }
      const root = getRootOptions(cmd)
      const api = createApi({
        baseURL: root.baseUrl || process.env.PSI_API_BASE || 'http://localhost:8080/api/v1',
        token: '',
        timeout: Number(root.timeout) || 120000
      })
      const data = await api.post('/auth/login', {
        username,
        password,
        remember: opts.remember
      })
      if (!opts.noSave && data?.token) saveToken(data.token)
      printJson(data)
    })

  auth
    .command('logout')
    .description('调用 POST /auth/logout，并删除本机 token 文件')
    .action(async (_opts, cmd) => {
      const api = getApi(cmd)
      await api.post('/auth/logout')
      clearToken()
      printJson({ ok: true })
    })

  auth
    .command('token-path')
    .description('打印 token 文件绝对路径')
    .action(() => {
      console.log(TOKEN_PATH)
    })

  auth
    .command('navigation')
    .description('GET /auth/navigation（需 Bearer：菜单树、权限码、前端动态路由）')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/auth/navigation'))
    })
}

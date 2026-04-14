#!/usr/bin/env node
import { Command } from 'commander'
import { createApi, loadToken } from '../src/lib/client.mjs'
import { getRootOptions } from '../src/lib/util.mjs'
import { registerAll } from '../src/register/index.mjs'

function getApi (cmd) {
  const root = getRootOptions(cmd)
  return createApi({
    baseURL: root.baseUrl || process.env.PSI_API_BASE || 'http://localhost:8080/api/v1',
    token: root.token ?? loadToken(),
    timeout: root.timeout
  })
}

const program = new Command()

program
  .name('psims')
  .description('智链进销存 smart-ims 后端 HTTP API 命令行（与前端 /api/v1 一致）')
  .version('1.1.0')
  .option(
    '--base-url <url>',
    'API 根路径（含 /api/v1）',
    process.env.PSI_API_BASE || 'http://localhost:8080/api/v1'
  )
  .option('--token <jwt>', 'Bearer Token，覆盖 PSI_TOKEN 与 ~/.psi-smart-ims/token')
  .option('--timeout <ms>', '请求超时毫秒', '120000')

registerAll(program, { getApi })

program.showHelpAfterError()

try {
  await program.parseAsync(process.argv)
} catch (e) {
  console.error(e.message || String(e))
  process.exit(1)
}

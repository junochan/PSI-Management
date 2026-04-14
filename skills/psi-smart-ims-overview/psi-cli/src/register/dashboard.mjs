import { printJson } from '../lib/util.mjs'

const ALLOWED_DAYS = new Set([7, 30, 90])

export function registerDashboard (program, { getApi }) {
  const dashboard = program.command('dashboard').description('仪表盘聚合数据')

  dashboard
    .command('overview')
    .description('GET /dashboard/overview（销售趋势、分类、排名、库存等）')
    .option('-d, --days <n>', '统计天数，仅 7 / 30 / 90', '7')
    .action(async (opts, cmd) => {
      let days = Number.parseInt(String(opts.days), 10)
      if (!ALLOWED_DAYS.has(days)) days = 7
      printJson(await getApi(cmd).get('/dashboard/overview', { params: { days } }))
    })
}

import fs from 'fs'
import FormData from 'form-data'
import { printJson, readBodyFromOptions, parseQueryJson } from '../lib/util.mjs'

export function registerProducts (program, { getApi }) {
  const products = program.command('products').description('商品：CRUD、批量删、Excel 导入、图片上传、以图搜图')

  products
    .command('list')
    .description('GET /products 分页与筛选（参数与后端 PageQuery 及筛选字段一致）')
    .option('-q, --query <json>', 'URL 查询 JSON，如 {"current":1,"size":10}', '{}')
    .action(async (opts, cmd) => {
      const api = getApi(cmd)
      const data = await api.get('/products', { params: parseQueryJson(opts.query) })
      printJson(data)
    })

  products
    .command('get <id>')
    .description('GET /products/:id 商品详情')
    .action(async (id, _opts, cmd) => {
      const api = getApi(cmd)
      printJson(await api.get(`/products/${id}`))
    })

  products
    .command('create')
    .description('POST /products 新建商品（Body 为 ProductDTO JSON）')
    .option('-d, --data <json>', 'JSON 字符串')
    .option('-f, --file <path>', '从文件读 JSON')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      const api = getApi(cmd)
      await api.post('/products', body)
      printJson({ ok: true })
    })

  products
    .command('update <id>')
    .description('PUT /products/:id')
    .option('-d, --data <json>', 'JSON 字符串')
    .option('-f, --file <path>', '从文件读 JSON')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      const api = getApi(cmd)
      await api.put(`/products/${id}`, body)
      printJson({ ok: true })
    })

  products
    .command('delete <id>')
    .description('DELETE /products/:id')
    .action(async (id, _opts, cmd) => {
      const api = getApi(cmd)
      await api.delete(`/products/${id}`)
      printJson({ ok: true })
    })

  products
    .command('batch-delete')
    .description('DELETE /products/batch，Body 为 id 数组，如 [1,2,3]')
    .option('-d, --data <json>', 'JSON 数组字符串')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      const api = getApi(cmd)
      await api.delete('/products/batch', { data: body })
      printJson({ ok: true })
    })

  products
    .command('import-excel')
    .description('POST /products/import（multipart，Excel，字段名 file，返回 jobId）')
    .requiredOption('--file <path>', '本地 Excel 路径')
    .action(async (opts, cmd) => {
      const api = getApi(cmd)
      const form = new FormData()
      form.append('file', fs.createReadStream(opts.file))
      const data = await api.post('/products/import', form, {
        headers: form.getHeaders(),
        maxBodyLength: Infinity,
        maxContentLength: Infinity,
        timeout: 120000
      })
      printJson(data)
    })

  products
    .command('import-task <jobId>')
    .description('GET /products/import/:jobId 查询异步导入任务进度')
    .action(async (jobId, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/products/import/${jobId}`))
    })

  products
    .command('upload-image')
    .description('POST /products/image（multipart，字段名 file）')
    .requiredOption('--file <path>', '本地图片路径')
    .action(async (opts, cmd) => {
      const api = getApi(cmd)
      const form = new FormData()
      form.append('file', fs.createReadStream(opts.file))
      const data = await api.post('/products/image', form, {
        headers: form.getHeaders(),
        maxBodyLength: Infinity,
        maxContentLength: Infinity
      })
      printJson(data)
    })

  products
    .command('search-image')
    .description('POST /products/search-by-image（multipart，字段 image + 可选筛选字段）')
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
      const data = await getApi(cmd).post('/products/search-by-image', form, {
        headers: form.getHeaders(),
        maxBodyLength: Infinity,
        maxContentLength: Infinity,
        timeout: 120000
      })
      printJson(data)
    })

  const categories = program.command('categories').description('商品分类')

  categories
    .command('list')
    .description('GET /categories')
    .action(async (_opts, cmd) => {
      printJson(await getApi(cmd).get('/categories'))
    })

  categories
    .command('get <id>')
    .description('GET /categories/:id')
    .action(async (id, _opts, cmd) => {
      printJson(await getApi(cmd).get(`/categories/${id}`))
    })

  categories
    .command('create')
    .description('POST /categories')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).post('/categories', body)
      printJson({ ok: true })
    })

  categories
    .command('update <id>')
    .description('PUT /categories/:id')
    .option('-d, --data <json>', 'JSON')
    .option('-f, --file <path>', 'JSON 文件')
    .action(async (id, opts, cmd) => {
      const body = readBodyFromOptions(opts)
      if (!body) throw new Error('请提供 --data 或 --file')
      await getApi(cmd).put(`/categories/${id}`, body)
      printJson({ ok: true })
    })

  categories
    .command('delete <id>')
    .description('DELETE /categories/:id')
    .action(async (id, _opts, cmd) => {
      await getApi(cmd).delete(`/categories/${id}`)
      printJson({ ok: true })
    })
}

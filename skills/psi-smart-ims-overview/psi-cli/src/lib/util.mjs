import fs from 'fs'

export function getRootOptions (command) {
  let p = command
  while (p.parent) p = p.parent
  return p.opts()
}

export function parseQueryJson (s) {
  if (s == null || String(s).trim() === '') return {}
  return JSON.parse(String(s))
}

export function parseBodyJson (s) {
  if (s == null || String(s).trim() === '') return undefined
  return JSON.parse(String(s))
}

export function printJson (data) {
  if (data === undefined || data === null) {
    console.log('')
    return
  }
  console.log(JSON.stringify(data, null, 2))
}

export function readBodyFromOptions ({ data, file }) {
  if (file) {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  }
  if (data !== undefined && data !== null && String(data).trim() !== '') {
    return JSON.parse(String(data))
  }
  return undefined
}

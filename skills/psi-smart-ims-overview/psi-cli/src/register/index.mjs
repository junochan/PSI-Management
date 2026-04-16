import { registerAuth } from './auth.mjs'
import { registerDashboard } from './dashboard.mjs'
import { registerProducts } from './products.mjs'
import { registerEntities } from './entities.mjs'
import { registerPurchase } from './purchase.mjs'
import { registerSales } from './sales.mjs'
import { registerInventory } from './inventory.mjs'
import { registerAftersales } from './aftersales.mjs'
import { registerSystem } from './system.mjs'
import { registerSpec } from './spec.mjs'

export function registerAll (program, ctx) {
  registerAuth(program, ctx)
  registerDashboard(program, ctx)
  registerProducts(program, ctx)
  registerEntities(program, ctx)
  registerPurchase(program, ctx)
  registerSales(program, ctx)
  registerInventory(program, ctx)
  registerAftersales(program, ctx)
  registerSystem(program, ctx)
  registerSpec(program, ctx)
}

import { requestJson } from './client'
import {
  isMemoryList,
  isMemoryResponse,
  type MemoryRequest,
  type MemoryResponse,
} from './types'

export const getMemories = () =>
  requestJson<MemoryResponse[]>('/memories', 'GET', undefined, isMemoryList)

export const createMemory = (memory: MemoryRequest) =>
  requestJson('/memories', 'POST', memory, isMemoryResponse)

import { requestJson } from './client'
import {
  isChatResponse,
  isConversationMessageList,
  type ConversationMessageResponse,
} from './types'

export const sendChatMessage = (message: string) =>
  requestJson('/chat', 'POST', { message }, isChatResponse)

export const getConversationHistory = () =>
  requestJson<ConversationMessageResponse[]>(
    '/chat/history',
    'GET',
    undefined,
    isConversationMessageList,
  )

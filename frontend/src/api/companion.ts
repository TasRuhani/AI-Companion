import { requestJson, requestText } from './client'
import {
  isCharacterProfile,
  isCompanionIdentity,
  type CharacterProfile,
  type CompanionIdentity,
  type IdentityFields,
} from './types'

export const getIdentity = () =>
  requestJson('/companion/identity', 'GET', undefined, isCompanionIdentity)

export const initializeIdentity = (identity: IdentityFields) =>
  requestJson('/companion/initialize', 'POST', identity, isCompanionIdentity)

export const updateIdentity = (identity: IdentityFields) =>
  requestJson('/companion/identity', 'PUT', identity, isCompanionIdentity)

export const resetIdentity = () => requestText('/companion/reset', 'POST')

export const researchCharacter = (character: string) =>
  requestJson<CharacterProfile>(
    '/companion/research',
    'POST',
    { character },
    isCharacterProfile,
  )

export function identityToFields(identity: CompanionIdentity): IdentityFields {
  return {
    name: identity.name,
    personality: identity.personality,
    background: identity.background,
    history: identity.history,
    speechStyle: identity.speechStyle,
    values: identity.values,
  }
}

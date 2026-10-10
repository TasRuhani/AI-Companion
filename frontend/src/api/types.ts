export interface ChatResponse {
  response: string
}

export interface ConversationMessageResponse {
  id: number
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface IdentityFields {
  name: string
  personality: string
  background: string
  history: string
  speechStyle: string
  values: string
}

export interface CompanionIdentity extends IdentityFields {
  id: number
  source: 'USER_CREATED' | 'WEB_RESEARCH' | 'GENERATED'
  active: boolean
  createdAt: string
  updatedAt: string
  portraitId: string | null
}

export interface UserProfile {
  id: number
  name: string
  preferredNickname: string | null
  age: number | null
  gender: string | null
  bio: string | null
  interests: string | null
  favoriteTopics: string | null
  communicationPreferences: string | null
  thingsToKnow: string | null
  topicsToAvoid: string | null
  createdAt: string
  updatedAt: string
}

export type UserProfileFields = Omit<UserProfile, 'id' | 'createdAt' | 'updatedAt'>
export type PersonalFactStatus = 'PROPOSED' | 'CONFIRMED' | 'REJECTED'

export interface PersonalFact {
  id: number
  content: string
  status: PersonalFactStatus
  source: string
  reviewReason: string
  createdAt: string
  updatedAt: string
}

export interface OnboardingStatus {
  userProfileComplete: boolean
  companionIdentityComplete: boolean
  nextStep: 'USER_PROFILE' | 'COMPANION_IDENTITY' | 'READY'
}

export type CharacterProfile = IdentityFields

export type MemoryCategory =
  | 'CORE_FACT'
  | 'PREFERENCE'
  | 'INTEREST'
  | 'EVENT'
  | 'EMOTIONAL'
  | 'RELATIONAL'
  | 'COMMITMENT'
  | 'GOAL'

export type MemorySource = 'EXTRACTED' | 'USER_CORRECTED' | 'MANUAL'

export interface MemoryRequest {
  content: string
  rawExcerpt: string | null
  category: MemoryCategory
  importance: number
  confidence: number
  source: MemorySource
}

export interface MemoryResponse extends MemoryRequest {
  id: number
  createdAt: string
  updatedAt: string
  lastReferencedAt: string | null
  referenceCount: number
  supersedesId: number | null
  active: boolean
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value)

const isString = (value: unknown): value is string => typeof value === 'string'
const isNumber = (value: unknown): value is number => typeof value === 'number'

export const isChatResponse = (value: unknown): value is ChatResponse =>
  isRecord(value) && isString(value.response)

export const isConversationMessageList = (
  value: unknown,
): value is ConversationMessageResponse[] =>
  Array.isArray(value) &&
  value.every(
    (message) =>
      isRecord(message) &&
      isNumber(message.id) &&
      (message.role === 'user' || message.role === 'assistant') &&
      isString(message.content) &&
      isString(message.createdAt),
  )

const identityFieldNames: (keyof IdentityFields)[] = [
  'name',
  'personality',
  'background',
  'history',
  'speechStyle',
  'values',
]

const hasIdentityFields = (value: Record<string, unknown>): boolean =>
  identityFieldNames.every((field) => isString(value[field]))

export const isCompanionIdentity = (
  value: unknown,
): value is CompanionIdentity =>
  isRecord(value) &&
  hasIdentityFields(value) &&
  isNumber(value.id) &&
  (value.source === 'USER_CREATED' ||
    value.source === 'WEB_RESEARCH' ||
    value.source === 'GENERATED') &&
  typeof value.active === 'boolean' &&
  isString(value.createdAt) &&
  isString(value.updatedAt) &&
  (value.portraitId === null || isString(value.portraitId))

const isNullableString = (value: unknown): value is string | null =>
  value === null || isString(value)

export const isUserProfile = (value: unknown): value is UserProfile =>
  isRecord(value) &&
  isNumber(value.id) &&
  isString(value.name) &&
  isNullableString(value.preferredNickname) &&
  (value.age === null || isNumber(value.age)) &&
  isNullableString(value.gender) &&
  isNullableString(value.bio) &&
  isNullableString(value.interests) &&
  isNullableString(value.favoriteTopics) &&
  isNullableString(value.communicationPreferences) &&
  isNullableString(value.thingsToKnow) &&
  isNullableString(value.topicsToAvoid) &&
  isString(value.createdAt) &&
  isString(value.updatedAt)

export const isPersonalFact = (value: unknown): value is PersonalFact =>
  isRecord(value) &&
  isNumber(value.id) &&
  isString(value.content) &&
  (value.status === 'PROPOSED' ||
    value.status === 'CONFIRMED' ||
    value.status === 'REJECTED') &&
  isString(value.source) &&
  isString(value.reviewReason) &&
  isString(value.createdAt) &&
  isString(value.updatedAt)

export const isPersonalFactList = (value: unknown): value is PersonalFact[] =>
  Array.isArray(value) && value.every(isPersonalFact)

export const isOnboardingStatus = (value: unknown): value is OnboardingStatus =>
  isRecord(value) &&
  typeof value.userProfileComplete === 'boolean' &&
  typeof value.companionIdentityComplete === 'boolean' &&
  (value.nextStep === 'USER_PROFILE' ||
    value.nextStep === 'COMPANION_IDENTITY' ||
    value.nextStep === 'READY')

export const isCharacterProfile = (
  value: unknown,
): value is CharacterProfile =>
  isRecord(value) && hasIdentityFields(value)

const memoryCategories: MemoryCategory[] = [
  'CORE_FACT',
  'PREFERENCE',
  'INTEREST',
  'EVENT',
  'EMOTIONAL',
  'RELATIONAL',
  'COMMITMENT',
  'GOAL',
]

const memorySources: MemorySource[] = [
  'EXTRACTED',
  'USER_CORRECTED',
  'MANUAL',
]

export const isMemoryCategory = (value: unknown): value is MemoryCategory =>
  typeof value === 'string' &&
  memoryCategories.some((category) => category === value)

export const isMemoryResponse = (value: unknown): value is MemoryResponse =>
  isRecord(value) &&
  isNumber(value.id) &&
  isString(value.content) &&
  (value.rawExcerpt === null || isString(value.rawExcerpt)) &&
  isMemoryCategory(value.category) &&
  isNumber(value.importance) &&
  isNumber(value.confidence) &&
  isString(value.createdAt) &&
  isString(value.updatedAt) &&
  (value.lastReferencedAt === null || isString(value.lastReferencedAt)) &&
  isNumber(value.referenceCount) &&
  (value.supersedesId === null || isNumber(value.supersedesId)) &&
  typeof value.active === 'boolean' &&
  typeof value.source === 'string' &&
  memorySources.some((source) => source === value.source)

export const isMemoryList = (value: unknown): value is MemoryResponse[] =>
  Array.isArray(value) && value.every(isMemoryResponse)

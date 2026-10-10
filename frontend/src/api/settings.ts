import { requestEmpty, requestJson, requestOptionalJson } from './client'
import {
  isOnboardingStatus,
  isPersonalFactList,
  isPersonalFact,
  isUserProfile,
  type OnboardingStatus,
  type PersonalFact,
  type PersonalFactStatus,
  type UserProfile,
  type UserProfileFields,
} from './types'

export const getOnboardingStatus = () =>
  requestJson<OnboardingStatus>('/settings/onboarding', 'GET', undefined, isOnboardingStatus)

export const getUserProfile = () =>
  requestOptionalJson<UserProfile>('/settings/profile', 'GET', undefined, isUserProfile)

export const saveUserProfile = (profile: UserProfileFields) =>
  requestJson('/settings/profile', 'PUT', profile, isUserProfile)

export const getPersonalFacts = () =>
  requestJson<PersonalFact[]>('/settings/personal-facts', 'GET', undefined, isPersonalFactList)

export const addPersonalFact = (content: string) =>
  requestJson('/settings/personal-facts', 'POST', { content }, isPersonalFact)

export const editPersonalFact = (id: number, content: string) =>
  requestJson(`/settings/personal-facts/${id}`, 'PUT', { content }, isPersonalFact)

export const updatePersonalFactStatus = (id: number, status: PersonalFactStatus) =>
  requestJson(`/settings/personal-facts/${id}`, 'PATCH', { status }, isPersonalFact)

export const deletePersonalFact = (id: number) =>
  requestEmpty(`/settings/personal-facts/${id}`, 'DELETE')

export const resetUserProfile = () => requestEmpty('/settings/reset-profile', 'POST')
export const resetAll = () => requestEmpty('/settings/reset-all', 'POST')

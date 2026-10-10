import { apiUrl, requestEmpty, requestForm } from './client'
import { isCompanionIdentity } from './types'

const portraitPath = (identityId: number) =>
  `/companion/identity/${identityId}/portrait`

export const portraitUrl = (identityId: number, portraitId: string) =>
  `${apiUrl(portraitPath(identityId))}?v=${encodeURIComponent(portraitId)}`

export const uploadPortrait = (identityId: number, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return requestForm(
    portraitPath(identityId),
    'POST',
    form,
    isCompanionIdentity,
  )
}

export const removePortrait = (identityId: number) =>
  requestEmpty(portraitPath(identityId), 'DELETE')

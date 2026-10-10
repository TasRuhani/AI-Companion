import { ApiError, isRecord } from './types'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(
  /\/+$/,
  '',
)
const REQUEST_TIMEOUT_MS = 120_000

export const apiUrl = (path: string): string =>
  `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`

async function fetchResponse(
  path: string,
  method: string,
  body?: unknown,
): Promise<Response> {
  const controller = new AbortController()
  const timeout = window.setTimeout(
    () => controller.abort(),
    REQUEST_TIMEOUT_MS,
  )

  try {
    return await fetch(apiUrl(path), {
      method,
      headers:
        body === undefined || body instanceof FormData
          ? undefined
          : { 'Content-Type': 'application/json' },
      body:
        body === undefined
          ? undefined
          : body instanceof FormData
            ? body
            : JSON.stringify(body),
      signal: controller.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError('The request took too long. Please try again.')
    }
    throw new ApiError('Could not reach the companion service. Check that it is running.')
  } finally {
    window.clearTimeout(timeout)
  }
}

async function getErrorMessage(response: Response): Promise<string> {
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    try {
      const payload: unknown = await response.json()
      if (isRecord(payload) && typeof payload.message === 'string') {
        return payload.message
      }
    } catch {
      return `The request failed (${response.status}).`
    }
  }
  return `The request failed (${response.status}).`
}

export async function requestJson<T>(
  path: string,
  method: string,
  body: unknown,
  validate: (value: unknown) => value is T,
): Promise<T> {
  const response = await fetchResponse(path, method, body)
  if (!response.ok) {
    throw new ApiError(await getErrorMessage(response), response.status)
  }

  let payload: unknown
  try {
    payload = await response.json()
  } catch {
    throw new ApiError('The service returned an unreadable response.')
  }
  if (!validate(payload)) {
    throw new ApiError('The service returned data in an unexpected format.')
  }
  return payload
}

export async function requestOptionalJson<T>(
  path: string,
  method: string,
  body: unknown,
  validate: (value: unknown) => value is T,
): Promise<T | null> {
  const response = await fetchResponse(path, method, body)
  if (!response.ok) {
    throw new ApiError(await getErrorMessage(response), response.status)
  }
  if (response.status === 204) return null

  let payload: unknown
  try {
    payload = await response.json()
  } catch {
    throw new ApiError('The service returned an unreadable response.')
  }
  if (!validate(payload)) {
    throw new ApiError('The service returned data in an unexpected format.')
  }
  return payload
}

export async function requestForm<T>(
  path: string,
  method: string,
  body: FormData,
  validate: (value: unknown) => value is T,
): Promise<T> {
  const response = await fetchResponse(path, method, body)
  if (!response.ok) {
    throw new ApiError(await getErrorMessage(response), response.status)
  }
  let payload: unknown
  try {
    payload = await response.json()
  } catch {
    throw new ApiError('The service returned an unreadable response.')
  }
  if (!validate(payload)) {
    throw new ApiError('The service returned data in an unexpected format.')
  }
  return payload
}

export async function requestText(path: string, method: string): Promise<string> {
  const response = await fetchResponse(path, method)
  if (!response.ok) {
    throw new ApiError(await getErrorMessage(response), response.status)
  }
  return response.text()
}

export async function requestEmpty(path: string, method: string): Promise<void> {
  const response = await fetchResponse(path, method)
  if (!response.ok) {
    throw new ApiError(await getErrorMessage(response), response.status)
  }
}

import { useState } from 'react'
import {
  BookOpen,
  Check,
  ChevronDown,
  FlaskConical,
  LoaderCircle,
  RotateCcw,
  UserRound,
} from 'lucide-react'
import PortraitImage from '../components/PortraitImage'
import PortraitUploader from '../components/PortraitUploader'
import {
  identityToFields,
  initializeIdentity,
  researchCharacter,
  resetIdentity,
  updateIdentity,
} from '../api/companion'
import { ApiError } from '../api/types'
import type {
  CharacterProfile,
  CompanionIdentity,
  IdentityFields,
} from '../api/types'

interface IdentityPanelProps {
  identity: CompanionIdentity | null
  identityLoading: boolean
  loadError: string | null
  onIdentityChange: (identity: CompanionIdentity | null) => void
  onReload: () => Promise<void>
}

const fieldDefinitions: {
  key: keyof IdentityFields
  label: string
  rows: number
  required: boolean
  hint: string
}[] = [
  { key: 'name', label: 'Name', rows: 1, required: true, hint: 'What should you call them?' },
  { key: 'personality', label: 'Personality', rows: 3, required: true, hint: 'How do they see and move through the world?' },
  { key: 'background', label: 'Background', rows: 3, required: true, hint: 'A little about where they come from.' },
  { key: 'history', label: 'History', rows: 3, required: true, hint: 'Experiences that shaped them.' },
  { key: 'speechStyle', label: 'Speech style', rows: 2, required: true, hint: 'How do they express themselves?' },
  { key: 'values', label: 'Values', rows: 2, required: true, hint: 'What matters to them?' },
]

const emptyIdentity: IdentityFields = {
  name: '',
  personality: '',
  background: '',
  history: '',
  speechStyle: '',
  values: '',
}

function present(value: string | null | undefined): string {
  return value?.trim() || 'Not provided'
}

export default function IdentityPanel({
  identity,
  identityLoading,
  loadError,
  onIdentityChange,
  onReload,
}: IdentityPanelProps) {
  const [formOpen, setFormOpen] = useState(false)
  const [fields, setFields] = useState<IdentityFields>(
    identity ? identityToFields(identity) : emptyIdentity,
  )
  const [researchName, setResearchName] = useState('')
  const [researchResult, setResearchResult] = useState<CharacterProfile | null>(
    null,
  )
  const [working, setWorking] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [resetDialogOpen, setResetDialogOpen] = useState(false)

  function applyProfile(profile: CharacterProfile) {
    setFields({
      ...profile,
    })
    setFormOpen(true)
    setResearchResult(null)
    setError(null)
  }

  async function submitIdentity(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (working) return
    setWorking(true)
    setError(null)
    setNotice(null)
    try {
      const created = identity
        ? await updateIdentity(fields)
        : await initializeIdentity(fields)
      onIdentityChange(created)
      setFields(identityToFields(created))
      setFormOpen(false)
      setNotice(identity ? 'Your companion identity was updated.' : 'Your companion identity is ready.')
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Could not create the identity.')
    } finally {
      setWorking(false)
    }
  }

  async function runResearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!researchName.trim() || working) return
    setWorking(true)
    setError(null)
    setNotice(null)
    try {
      const result = await researchCharacter(researchName.trim())
      setResearchResult(result)
      setNotice('A profile draft is ready. Review it before using it.')
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Research could not be completed.')
    } finally {
      setWorking(false)
    }
  }

  async function confirmReset() {
    if (!identity || working) return
    setWorking(true)
    setError(null)
    setNotice(null)
    try {
      await resetIdentity()
      onIdentityChange(null)
      setFields(emptyIdentity)
      setFormOpen(true)
      setNotice('Companion history, memories, profile facts, mood, and active identity were reset.')
      setResetDialogOpen(false)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Could not reset the identity.')
    } finally {
      setWorking(false)
    }
  }

  return (
    <section className="identity-subpanel">
      <header className="section-heading">
        <div>
          <p className="eyebrow">THE PERSON BEHIND THE VOICE</p>
          <h1>Identity</h1>
          <p className="section-intro">
            A little about who your companion is, as configured in the backend.
          </p>
        </div>
        <div className="section-icon">
          <UserRound size={19} />
        </div>
      </header>

      {loadError && (
        <div className="notice notice-error" role="alert">
          <span>{loadError}</span>
          <button className="text-action" type="button" onClick={() => void onReload()}>
            <RotateCcw size={14} /> Try again
          </button>
        </div>
      )}
      {resetDialogOpen && identity && (
        <div className="portrait-dialog-backdrop" role="presentation">
          <section
            className="portrait-dialog reset-warning-dialog"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="reset-warning-title"
            aria-describedby="reset-warning-description"
          >
            <p className="eyebrow">A FRESH START</p>
            <h2 id="reset-warning-title">Start fresh with a new companion?</h2>
            <p id="reset-warning-description">
              Resetting your companion will erase the current companion's
              conversation history and associated memories. This action cannot be
              undone. Your new companion will start with a clean slate.
            </p>
            <p className="quiet-note">
              This also clears companion profile facts and embeddings, mood state
              and events, and proactive-message tracking. Your identity record and
              its uploaded portrait remain saved but inactive. Your personal
              profile and personal facts are preserved.
            </p>
            {error && <div className="notice notice-error" role="alert">{error}</div>}
            <div className="portrait-dialog-actions">
              <button
                className="button button-quiet"
                type="button"
                disabled={working}
                onClick={() => setResetDialogOpen(false)}
              >
                Cancel
              </button>
              <button
                className="button button-danger"
                type="button"
                disabled={working}
                onClick={() => void confirmReset()}
              >
                {working ? <LoaderCircle className="spin" size={15} /> : <RotateCcw size={15} />}
                Confirm reset
              </button>
            </div>
          </section>
        </div>
      )}
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}

      {identityLoading ? (
        <div className="panel-loading" role="status">
          <span className="loading-orb" aria-hidden="true" />
          <span>Finding your companion’s identity…</span>
        </div>
      ) : null}

      {!identityLoading && identity && !formOpen && (
        <div className="identity-content">
          <div className="identity-hero">
            <div className="identity-avatar">
              <PortraitImage
                identity={identity}
                name={identity.name}
                className="identity-avatar-portrait"
              />
            </div>
            <div>
              <h2>{identity.name}</h2>
              <p>
                {identity.active ? 'Active identity' : 'Inactive identity'}
                <span className="inline-divider">·</span>
                {identity.source.replace(/_/g, ' ').toLowerCase()}
              </p>
            </div>
          </div>
          <PortraitUploader
            identity={identity}
            name={identity.name}
            onIdentityChange={onIdentityChange}
            compact
          />

          <div className="identity-detail-grid">
            {fieldDefinitions.slice(1).map(({ key, label }) => (
              <article className="identity-detail" key={key}>
                <h3>{label}</h3>
                <p>{present(identity[key])}</p>
              </article>
            ))}
          </div>
          <p className="quiet-note">
            Changes to this identity are saved in place and shape new responses.
          </p>
          <button
            className="button button-quiet"
            type="button"
            disabled={working}
            onClick={() => {
              setFields(identityToFields(identity))
              setFormOpen(true)
            }}
          >
            Edit companion identity
          </button>
          <button
            className="button button-quiet button-danger"
            type="button"
            disabled={working}
            onClick={() => {
              setError(null)
              setResetDialogOpen(true)
            }}
          >
            <RotateCcw size={15} />
            Reset companion
          </button>
        </div>
      )}

      {!identityLoading && identity && !formOpen && (
        <details className="research-disclosure">
          <summary>
            <FlaskConical size={15} />
            Research another character profile
            <ChevronDown className="disclosure-chevron" size={15} />
          </summary>
          <ResearchForm
            name={researchName}
            onNameChange={setResearchName}
            onSubmit={runResearch}
            working={working}
            result={researchResult}
            onUse={applyProfile}
          />
        </details>
      )}

      {!identityLoading && ((!identity && !loadError) || formOpen) && (
        <div className="identity-setup">
          <details className="research-disclosure">
            <summary>
              <FlaskConical size={15} />
              Start with a researched profile
              <ChevronDown className="disclosure-chevron" size={15} />
            </summary>
            <ResearchForm
              name={researchName}
              onNameChange={setResearchName}
              onSubmit={runResearch}
              working={working}
              result={researchResult}
              onUse={applyProfile}
            />
          </details>

          <form className="identity-form" onSubmit={submitIdentity}>
            <div className="form-heading">
              <div>
                <p className="eyebrow">MAKE IT YOURS</p>
                <h2>{identity ? 'Edit identity' : 'Create an identity'}</h2>
              </div>
              {identity && (
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Close identity form"
                  onClick={() => {
                    setFields(identityToFields(identity))
                    setFormOpen(false)
                    setError(null)
                  }}
                >
                  <span aria-hidden="true">×</span>
                </button>
              )}
            </div>

            <div className="identity-fields">
              {fieldDefinitions.map(({ key, label, rows, required, hint }) => (
                <label
                  className={rows === 1 ? 'field field-full' : 'field'}
                  key={key}
                >
                  <span>{label}{required ? ' *' : ''}</span>
                  {rows === 1 ? (
                    <input
                      required={required}
                      value={fields[key]}
                      placeholder={hint}
                      onChange={(event) =>
                        setFields({ ...fields, [key]: event.target.value })
                      }
                    />
                  ) : (
                    <textarea
                      required={required}
                      rows={rows}
                      value={fields[key]}
                      placeholder={hint}
                      onChange={(event) =>
                        setFields({ ...fields, [key]: event.target.value })
                      }
                    />
                  )}
                </label>
              ))}
            </div>
            <button className="button button-primary" type="submit" disabled={working}>
              {working ? <LoaderCircle className="spin" size={16} /> : <Check size={16} />}
              {identity ? 'Save companion identity' : 'Create companion identity'}
            </button>
            <p className="quiet-note">
              Required fields follow the backend database constraints. This form
              {identity
                ? 'Changes apply to the active companion identity.'
                : 'Required fields follow the backend database constraints.'}
            </p>
          </form>
        </div>
      )}
    </section>
  )
}

interface ResearchFormProps {
  name: string
  onNameChange: (name: string) => void
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void
  working: boolean
  result: CharacterProfile | null
  onUse: (profile: CharacterProfile) => void
}

function ResearchForm({
  name,
  onNameChange,
  onSubmit,
  working,
  result,
  onUse,
}: ResearchFormProps) {
  return (
    <div className="research-content">
      <p>
        The backend will use its configured research service to suggest a
        personality profile. Review the result before creating an identity.
      </p>
      <form className="research-form" onSubmit={onSubmit}>
        <label className="sr-only" htmlFor="research-character">
          Character name
        </label>
        <input
          id="research-character"
          value={name}
          required
          placeholder="Name to research"
          onChange={(event) => onNameChange(event.target.value)}
        />
        <button className="button button-quiet" type="submit" disabled={working}>
          {working ? <LoaderCircle className="spin" size={15} /> : <BookOpen size={15} />}
          Research
        </button>
      </form>
      {result && (
        <div className="research-result">
          <p className="eyebrow">PROFILE DRAFT</p>
          <h3>{result.name}</h3>
          <p>{result.personality}</p>
          <button
            className="text-action"
            type="button"
            onClick={() => onUse(result)}
          >
            Use this draft in the identity form
          </button>
        </div>
      )}
    </div>
  )
}

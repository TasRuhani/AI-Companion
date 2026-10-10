import { useEffect, useState } from 'react'
import { Archive, LoaderCircle, Plus, Sparkles } from 'lucide-react'
import { createMemory, getMemories } from '../api/memories'
import {
  addPersonalFact,
  deletePersonalFact,
  editPersonalFact,
  getPersonalFacts,
  updatePersonalFactStatus,
} from '../api/settings'
import { ApiError, isMemoryCategory } from '../api/types'
import type {
  MemoryCategory,
  MemoryRequest,
  MemoryResponse,
  PersonalFact,
  PersonalFactStatus,
} from '../api/types'

const categories: MemoryCategory[] = [
  'CORE_FACT',
  'PREFERENCE',
  'INTEREST',
  'EVENT',
  'EMOTIONAL',
  'RELATIONAL',
  'COMMITMENT',
  'GOAL',
]

const emptyDraft: MemoryRequest = {
  content: '',
  rawExcerpt: null,
  category: 'CORE_FACT',
  importance: 5,
  confidence: 1,
  source: 'MANUAL',
}

function label(value: string): string {
  return value
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ')
}

export default function MemoryPanel() {
  const [memories, setMemories] = useState<MemoryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [draft, setDraft] = useState<MemoryRequest>(emptyDraft)
  const [showForm, setShowForm] = useState(false)
  const [filter, setFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ACTIVE')
  const [notice, setNotice] = useState<string | null>(null)
  const [facts, setFacts] = useState<PersonalFact[]>([])
  const [factsLoading, setFactsLoading] = useState(true)
  const [factsWorking, setFactsWorking] = useState(false)
  const [factsError, setFactsError] = useState<string | null>(null)
  const [factDraft, setFactDraft] = useState('')
  const [editingFactId, setEditingFactId] = useState<number | null>(null)
  const [editingFactContent, setEditingFactContent] = useState('')

  async function loadMemories() {
    setLoading(true)
    setError(null)
    try {
      setMemories(await getMemories())
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Could not load memories.')
    } finally {
      setLoading(false)
    }
  }

  async function loadFacts() {
    setFactsLoading(true)
    setFactsError(null)
    try {
      setFacts(await getPersonalFacts())
    } catch (cause) {
      setFactsError(cause instanceof ApiError ? cause.message : 'Could not load personal facts.')
    } finally {
      setFactsLoading(false)
    }
  }

  useEffect(() => {
    void loadMemories()
    void loadFacts()
  }, [])

  async function submitFact(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!factDraft.trim() || factsWorking) return
    setFactsWorking(true)
    setFactsError(null)
    setNotice(null)
    try {
      const fact = await addPersonalFact(factDraft.trim())
      setFacts((current) => [fact, ...current])
      setFactDraft('')
      setNotice('Fact saved as proposed. Confirm it to include it in companion prompts.')
    } catch (cause) {
      setFactsError(cause instanceof ApiError ? cause.message : 'Could not save the fact.')
    } finally {
      setFactsWorking(false)
    }
  }

  async function changeFactStatus(fact: PersonalFact, status: PersonalFactStatus) {
    setFactsWorking(true)
    setFactsError(null)
    try {
      const updated = await updatePersonalFactStatus(fact.id, status)
      setFacts((current) => current.map((item) => item.id === fact.id ? updated : item))
    } catch (cause) {
      setFactsError(cause instanceof ApiError ? cause.message : 'Could not update the fact.')
    } finally {
      setFactsWorking(false)
    }
  }

  async function removeFact(fact: PersonalFact) {
    if (!window.confirm('Delete this personal fact?')) return
    setFactsWorking(true)
    setFactsError(null)
    try {
      await deletePersonalFact(fact.id)
      setFacts((current) => current.filter((item) => item.id !== fact.id))
    } catch (cause) {
      setFactsError(cause instanceof ApiError ? cause.message : 'Could not delete the fact.')
    } finally {
      setFactsWorking(false)
    }
  }

  async function saveEditedFact(fact: PersonalFact) {
    if (!editingFactContent.trim() || factsWorking) return
    setFactsWorking(true)
    setFactsError(null)
    try {
      const updated = await editPersonalFact(fact.id, editingFactContent.trim())
      setFacts((current) => current.map((item) => item.id === fact.id ? updated : item))
      setEditingFactId(null)
      setNotice('Personal fact updated.')
    } catch (cause) {
      setFactsError(cause instanceof ApiError ? cause.message : 'Could not update the fact.')
    } finally {
      setFactsWorking(false)
    }
  }

  async function submitMemory(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (working) return
    setWorking(true)
    setError(null)
    setNotice(null)
    try {
      const created = await createMemory(draft)
      setMemories((current) => [created, ...current])
      setDraft(emptyDraft)
      setShowForm(false)
      setNotice('Memory saved.')
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Could not save this memory.')
    } finally {
      setWorking(false)
    }
  }

  const visibleMemories = memories.filter((memory) => {
    if (filter === 'ACTIVE') return memory.active
    if (filter === 'INACTIVE') return !memory.active
    return true
  })

  return (
    <section className="secondary-panel">
      <header className="section-heading">
        <div>
          <p className="eyebrow">THINGS WORTH KEEPING</p>
          <h1>Memory</h1>
          <p className="section-intro">
            A transparent view of memories the backend has saved for your companion.
          </p>
        </div>
        <div className="section-icon">
          <Archive size={19} />
        </div>
      </header>

      {error && (
        <div className="notice notice-error" role="alert">
          <span>{error}</span>
          <button className="text-action" type="button" onClick={() => void loadMemories()}>
            Try again
          </button>
        </div>
      )}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}

      <section className="settings-section memory-fact-review" aria-labelledby="personal-facts-heading">
        <div className="form-heading">
          <div>
            <p className="eyebrow">PERSONAL FACTS</p>
            <h2 id="personal-facts-heading">Review what your companion knows</h2>
          </div>
          <button className="text-action" type="button" disabled={factsLoading} onClick={() => void loadFacts()}>
            Refresh
          </button>
        </div>
        <p className="section-intro">
          Conversation suggestions and facts you add appear here for review. Only confirmed facts are included in companion prompts.
        </p>
        {factsError && (
          <div className="notice notice-error" role="alert">
            <span>{factsError}</span>
            <button className="text-action" type="button" onClick={() => void loadFacts()}>
              Try again
            </button>
          </div>
        )}
        <form className="fact-compose" onSubmit={(event) => void submitFact(event)}>
          <label className="field field-full">
            <span>Add a personal fact</span>
            <textarea
              rows={2}
              value={factDraft}
              maxLength={2000}
              onChange={(event) => setFactDraft(event.target.value)}
              placeholder="For example: I prefer short answers when I'm busy."
            />
          </label>
          <button className="button button-primary" type="submit" disabled={factsWorking || !factDraft.trim()}>
            {factsWorking && <LoaderCircle className="spin" size={15} />}
            Add proposed fact
          </button>
        </form>
        {factsLoading ? (
          <div className="panel-loading" role="status"><LoaderCircle className="spin" size={17} /> Loading personal facts…</div>
        ) : factsError && facts.length === 0 ? null : facts.length === 0 ? (
          <div className="empty-state"><p>No personal facts saved yet.</p></div>
        ) : (
          <div className="memory-list">
            {facts.map((fact) => (
              <article className="memory-item" key={fact.id}>
                <div className="memory-item-top">
                  <span className="memory-category">{fact.source}</span>
                  <span className="memory-status">{fact.status.toLowerCase()}</span>
                </div>
                {editingFactId === fact.id ? (
                  <div className="fact-edit">
                    <textarea
                      rows={2}
                      maxLength={2000}
                      value={editingFactContent}
                      onChange={(event) => setEditingFactContent(event.target.value)}
                    />
                    <button className="text-action" type="button" disabled={factsWorking || !editingFactContent.trim()} onClick={() => void saveEditedFact(fact)}>Save correction</button>
                    <button className="text-action" type="button" onClick={() => setEditingFactId(null)}>Cancel</button>
                  </div>
                ) : <p>{fact.content}</p>}
                <p className="fact-reason">{fact.reviewReason}</p>
                <div className="fact-actions">
                  {editingFactId !== fact.id && <button className="text-action" type="button" disabled={factsWorking} onClick={() => { setEditingFactId(fact.id); setEditingFactContent(fact.content) }}>Correct</button>}
                  {fact.status !== 'CONFIRMED' && <button className="text-action" type="button" disabled={factsWorking} onClick={() => void changeFactStatus(fact, 'CONFIRMED')}>Confirm</button>}
                  {fact.status !== 'REJECTED' && <button className="text-action" type="button" disabled={factsWorking} onClick={() => void changeFactStatus(fact, 'REJECTED')}>Reject</button>}
                  {fact.status !== 'PROPOSED' && <button className="text-action" type="button" disabled={factsWorking} onClick={() => void changeFactStatus(fact, 'PROPOSED')}>Mark proposed</button>}
                  <button className="text-action fact-delete" type="button" disabled={factsWorking} onClick={() => void removeFact(fact)}>Delete</button>
                </div>
                <div className="memory-meta">
                  <span>Added {new Date(fact.createdAt).toLocaleDateString()}</span>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <div className="memory-toolbar">
        <div className="segmented-control" aria-label="Filter memories">
          {(['ACTIVE', 'ALL', 'INACTIVE'] as const).map((value) => (
            <button
              type="button"
              key={value}
              className={filter === value ? 'selected' : ''}
              aria-pressed={filter === value}
              onClick={() => setFilter(value)}
            >
              {label(value)}
            </button>
          ))}
        </div>
        <button
          className="button button-primary"
          type="button"
          onClick={() => {
            setShowForm((open) => !open)
            setError(null)
          }}
        >
          <Plus size={16} />
          Add memory
        </button>
      </div>

      {showForm && (
        <form className="memory-form" onSubmit={submitMemory}>
          <div className="form-heading">
            <div>
              <p className="eyebrow">MANUAL MEMORY</p>
              <h2>Add something to remember</h2>
            </div>
            <button
              className="icon-button"
              type="button"
              aria-label="Close memory form"
              onClick={() => setShowForm(false)}
            >
              <span aria-hidden="true">×</span>
            </button>
          </div>
          <label className="field field-full">
            <span>Memory</span>
            <textarea
              value={draft.content}
              required
              rows={3}
              maxLength={4000}
              placeholder="What should your companion remember?"
              onChange={(event) => setDraft({ ...draft, content: event.target.value })}
            />
          </label>
          <div className="memory-form-options">
            <label className="field">
              <span>Category</span>
              <select
                value={draft.category}
                onChange={(event) => {
                  const category = event.target.value
                  if (isMemoryCategory(category)) {
                    setDraft({ ...draft, category })
                  }
                }}
              >
                {categories.map((category) => (
                  <option value={category} key={category}>{label(category)}</option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Importance: {draft.importance} / 10</span>
              <input
                type="range"
                min={1}
                max={10}
                value={draft.importance}
                onChange={(event) =>
                  setDraft({ ...draft, importance: Number(event.target.value) })
                }
              />
            </label>
            <label className="field">
              <span>Confidence: {draft.confidence.toFixed(2)}</span>
              <input
                type="range"
                min={0}
                max={1}
                step={0.05}
                value={draft.confidence}
                onChange={(event) =>
                  setDraft({ ...draft, confidence: Number(event.target.value) })
                }
              />
            </label>
          </div>
          <button className="button button-primary" type="submit" disabled={working}>
            {working ? <LoaderCircle className="spin" size={16} /> : <Sparkles size={16} />}
            Save memory
          </button>
          <p className="quiet-note">
            The backend supports creating and listing memories. This screen does not
            offer edit or delete actions because the API does not expose them.
          </p>
        </form>
      )}

      {loading ? (
        <div className="panel-loading">
          <LoaderCircle className="spin" size={18} />
          <span>Gathering what’s been remembered…</span>
        </div>
      ) : error && memories.length === 0 ? null : visibleMemories.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><Sparkles size={19} /></div>
          <h2>{filter === 'ACTIVE' ? 'Nothing here just yet' : 'No memories in this view'}</h2>
          <p>
            {filter === 'ACTIVE'
              ? 'As your conversations unfold, meaningful details may find a place here. You can also add a memory yourself.'
              : 'Try another filter or add a memory when there’s something you want to keep.'}
          </p>
        </div>
      ) : (
        <div className="memory-list">
          {visibleMemories.map((memory) => (
            <article className={`memory-item${memory.active ? '' : ' memory-inactive'}`} key={memory.id}>
              <div className="memory-item-top">
                <span className="memory-category">{label(memory.category)}</span>
                {!memory.active && <span className="memory-status">Inactive</span>}
              </div>
              <p>{memory.content}</p>
              <div className="memory-meta">
                <span>Importance {memory.importance}/10</span>
                <span>Confidence {memory.confidence.toFixed(2)}</span>
                <span>{label(memory.source)}</span>
                <time dateTime={memory.createdAt}>
                  {new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(
                    new Date(memory.createdAt),
                  )}
                </time>
              </div>
            </article>
          ))}
        </div>
      )}
      <p className="quiet-note memory-footnote">
        <Archive size={13} />
        Memory content is returned by the backend and may include details from your conversations.
      </p>
    </section>
  )
}

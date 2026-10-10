import { useEffect, useState } from 'react'
import { Archive, LoaderCircle, Plus, Sparkles } from 'lucide-react'
import { createMemory, getMemories } from '../api/memories'
import { ApiError, isMemoryCategory } from '../api/types'
import type {
  MemoryCategory,
  MemoryRequest,
  MemoryResponse,
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

  useEffect(() => {
    void loadMemories()
  }, [])

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

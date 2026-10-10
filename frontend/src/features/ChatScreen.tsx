import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { ArrowUp, Check, RotateCcw, Sparkles } from 'lucide-react'
import PortraitImage from '../components/PortraitImage'
import type { CompanionIdentity } from '../api/types'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
  failure?: string
}

interface ChatScreenProps {
  companionName: string
  identity: CompanionIdentity | null
  messages: ChatMessage[]
  pendingId: string | null
  historyLoading: boolean
  historyError: string | null
  onSend: (message: string) => void
  onRetry: (message: ChatMessage) => void
}

function timestamp(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(value))
}

function splitSources(content: string): { message: string; sources: string | null } {
  const match = /(?:^|\n)Sources:\s*\n?/i.exec(content)
  if (!match || match.index === undefined) {
    return { message: content, sources: null }
  }

  return {
    message: content.slice(0, match.index).trimEnd(),
    sources: content.slice(match.index + match[0].length).trim(),
  }
}

function renderMessageText(content: string) {
  return content.split(/(\*\*.+?\*\*)/s).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return <strong key={index}>{part.slice(2, -2)}</strong>
    }
    return part
  })
}

export default function ChatScreen({
  companionName,
  identity,
  messages,
  pendingId,
  historyLoading,
  historyError,
  onSend,
  onRetry,
}: ChatScreenProps) {
  const [draft, setDraft] = useState('')
  const conversationRef = useRef<HTMLDivElement>(null)
  const composerRef = useRef<HTMLTextAreaElement>(null)
  const shouldFollowRef = useRef(true)

  useLayoutEffect(() => {
    if (shouldFollowRef.current) {
      conversationRef.current?.scrollTo({
        top: conversationRef.current.scrollHeight,
        behavior: 'smooth',
      })
    }
  }, [messages, pendingId])

  useEffect(() => {
    const textarea = composerRef.current
    if (!textarea) return
    textarea.style.height = 'auto'
    textarea.style.height = `${Math.min(textarea.scrollHeight, 160)}px`
  }, [draft])

  function submit() {
    const message = draft.trim()
    if (!message || pendingId || historyLoading) return
    setDraft('')
    onSend(message)
  }

  return (
    <section className="chat-panel" aria-label="Conversation">
      <div className="chat-heading">
        <div className="chat-title-group">
          <div className="mobile-orb">
            <PortraitImage identity={identity} name={companionName} className="mobile-portrait" />
          </div>
          <div>
            <p className="eyebrow">YOUR SPACE</p>
            <h1>{companionName}</h1>
          </div>
        </div>
        <div className={`presence-pill${pendingId ? ' presence-thinking' : ''}`}>
          <span className="presence-dot" />
          {pendingId ? 'Thinking' : 'Here with you'}
        </div>
      </div>

      <div
        className="conversation"
        ref={conversationRef}
        onScroll={(event) => {
          const element = event.currentTarget
          shouldFollowRef.current =
            element.scrollHeight - element.scrollTop - element.clientHeight < 96
        }}
        aria-live="polite"
        aria-relevant="additions text"
      >
        {historyLoading && (
          <div className="panel-loading" role="status">
            <span>Loading your conversation…</span>
          </div>
        )}
        {historyError && (
          <div className="notice notice-error" role="alert">{historyError}</div>
        )}
        {messages.length === 0 ? (
          <div className="chat-welcome">
            <PortraitImage
              identity={identity}
              name={companionName}
              className={`welcome-portrait${pendingId ? ' portrait-thinking' : ''}`}
            />
            <p className="eyebrow">A MOMENT FOR YOU</p>
            <h2>What’s on your mind?</h2>
            <p className="welcome-copy">
              Start wherever you are. This is a quiet space to think out loud.
            </p>
          </div>
        ) : (
          <div className="message-list">
            {messages.map((message) => {
              const { message: responseText, sources } =
                message.role === 'assistant'
                  ? splitSources(message.content)
                  : { message: message.content, sources: null }

              return (
                <article
                  className={`message-row message-${message.role}`}
                  key={message.id}
                >
                  {message.role === 'assistant' && (
                    <div className="message-mark" aria-hidden="true">
                      <Sparkles size={14} />
                    </div>
                  )}
                  <div className="message-content">
                    <div className="message-bubble">
                      {renderMessageText(responseText)}
                      {sources && (
                        <details className="message-sources">
                          <summary>Sources</summary>
                          <div className="message-sources-content">
                            {renderMessageText(sources)}
                          </div>
                        </details>
                      )}
                    </div>
                    <div className="message-meta">
                      <time
                        dateTime={message.createdAt}
                        title="Timestamp from this browser"
                      >
                        {timestamp(message.createdAt)}
                      </time>
                      {message.role === 'user' && !message.failure && (
                        <Check size={13} aria-label="Sent" />
                      )}
                    </div>
                    {message.failure && (
                      <div className="message-failure" role="alert">
                        <span>{message.failure}</span>
                        <button
                          className="text-action"
                          type="button"
                          disabled={pendingId !== null || historyLoading || !!historyError}
                          onClick={() => onRetry(message)}
                        >
                          <RotateCcw size={13} />
                          Retry
                        </button>
                        <p>
                          If the response was lost, retrying will send this
                          message again.
                        </p>
                      </div>
                    )}
                  </div>
                </article>
              )
            })}
            {pendingId && (
              <div className="message-row message-assistant" aria-label="Reply generating">
                <div className="message-mark" aria-hidden="true">
                  <Sparkles size={14} />
                </div>
                <div className="typing-bubble">
                  <span />
                  <span />
                  <span />
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      <form
        className="composer-wrap"
        onSubmit={(event) => {
          event.preventDefault()
          submit()
        }}
      >
        <label className="sr-only" htmlFor="chat-composer">
          Write a message
        </label>
        <div className="composer">
          <textarea
            ref={composerRef}
            id="chat-composer"
            value={draft}
            rows={1}
            maxLength={8000}
            placeholder="Write what’s on your mind..."
            disabled={pendingId !== null}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault()
                submit()
              }
            }}
          />
          <button
            className="send-button"
            type="submit"
            disabled={!draft.trim() || pendingId !== null || historyLoading || !!historyError}
            aria-label="Send message"
          >
            <ArrowUp size={18} />
          </button>
        </div>
        <p className="composer-note">
          Enter to send <span aria-hidden="true">·</span> Shift + Enter for a new
          line <span aria-hidden="true">·</span> Conversation saved automatically
        </p>
      </form>
    </section>
  )
}

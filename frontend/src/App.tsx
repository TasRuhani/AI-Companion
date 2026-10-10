import { useCallback, useEffect, useState } from 'react'
import {
  Archive,
  Heart,
  MessageCircle,
  Settings2,
  Sparkles,
} from 'lucide-react'
import { getIdentity } from './api/companion'
import { getConversationHistory, sendChatMessage } from './api/chat'
import { getOnboardingStatus } from './api/settings'
import { ApiError, type CompanionIdentity } from './api/types'
import PortraitImage from './components/PortraitImage'
import ChatScreen, { type ChatMessage } from './features/ChatScreen'
import MemoryPanel from './features/MemoryPanel'
import SettingsPanel from './features/SettingsPanel'

type Section = 'chat' | 'memory' | 'settings'

function toChatMessages(history: Awaited<ReturnType<typeof getConversationHistory>>): ChatMessage[] {
  return history.map((message) => ({
    id: String(message.id),
    role: message.role,
    content: message.content,
    createdAt: message.createdAt,
  }))
}

const navigation: { id: Section; label: string; icon: typeof MessageCircle }[] = [
  { id: 'chat', label: 'Conversation', icon: MessageCircle },
  { id: 'memory', label: 'Memory', icon: Archive },
  { id: 'settings', label: 'Settings', icon: Settings2 },
]

export default function App() {
  const [section, setSection] = useState<Section>('settings')
  const [profileComplete, setProfileComplete] = useState(false)
  const [identity, setIdentity] = useState<CompanionIdentity | null>(null)
  const [identityLoading, setIdentityLoading] = useState(true)
  const [identityLoadError, setIdentityLoadError] = useState<string | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [historyLoading, setHistoryLoading] = useState(true)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [conversationIdentityId, setConversationIdentityId] = useState<number | null>(null)
  const [pendingId, setPendingId] = useState<string | null>(null)

  const loadIdentity = useCallback(async () => {
    setIdentityLoading(true)
    setIdentityLoadError(null)
    try {
      const status = await getOnboardingStatus()
      setProfileComplete(status.userProfileComplete)
      const activeIdentity = await getIdentity()
      setIdentity(activeIdentity)
      setSection(status.nextStep === 'READY' ? 'chat' : 'settings')
    } catch (cause) {
      setIdentity(null)
      if (cause instanceof ApiError && cause.status === 404) {
        setSection('settings')
      } else if (cause instanceof ApiError && cause.message.includes('unexpected format')) {
        setIdentityLoadError(cause.message)
        setSection('settings')
      } else {
        setIdentityLoadError(
          cause instanceof ApiError ? cause.message : 'Could not load companion identity.',
        )
        setSection('settings')
      }
    } finally {
      setIdentityLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadIdentity()
  }, [loadIdentity])

  useEffect(() => {
    let current = true
    setMessages([])
    setConversationIdentityId(null)
    setHistoryError(null)
    if (!identity) {
      setHistoryLoading(false)
      return () => {
        current = false
      }
    }

    setHistoryLoading(true)
    void getConversationHistory()
      .then((history) => {
        if (!current) return
        setMessages(toChatMessages(history))
        setConversationIdentityId(identity.id)
      })
      .catch((cause: unknown) => {
        if (!current) return
        setHistoryError(
          cause instanceof ApiError
            ? cause.message
            : 'Could not load your saved conversation.',
        )
      })
      .finally(() => {
        if (current) setHistoryLoading(false)
      })

    return () => {
      current = false
    }
  }, [identity?.id])

  useEffect(() => {
    if (!identity || section !== 'chat') return

    let current = true
    let refreshInProgress = false
    const refreshHistory = async () => {
      if (refreshInProgress || document.visibilityState === 'hidden') return
      refreshInProgress = true
      try {
        const history = await getConversationHistory()
        if (!current) return
        const savedMessages = toChatMessages(history)
        setMessages((previous) => {
          const savedContent = new Set(
            savedMessages.map((message) => `${message.role}\u0000${message.content}`),
          )
          const failedMessages = previous.filter(
            (message) =>
              message.failure &&
              !savedContent.has(`${message.role}\u0000${message.content}`),
          )
          return [...savedMessages, ...failedMessages]
        })
        setConversationIdentityId(identity.id)
        setHistoryError(null)
      } catch (cause) {
        if (!current) return
        setHistoryError(
          cause instanceof ApiError
            ? cause.message
            : 'Could not refresh your saved conversation.',
        )
      } finally {
        refreshInProgress = false
      }
    }

    void refreshHistory()
    const intervalId = window.setInterval(() => void refreshHistory(), 5000)
    document.addEventListener('visibilitychange', refreshHistory)
    return () => {
      current = false
      window.clearInterval(intervalId)
      document.removeEventListener('visibilitychange', refreshHistory)
    }
  }, [identity?.id, section])

  function handleIdentityChange(nextIdentity: CompanionIdentity | null) {
    const createdIdentity = identity === null && nextIdentity !== null
    setIdentity(nextIdentity)
    setIdentityLoading(false)
    setIdentityLoadError(null)
    if (!nextIdentity) {
      setMessages([])
      setConversationIdentityId(null)
      setHistoryLoading(false)
      setHistoryError(null)
      setSection('settings')
    } else if (createdIdentity && profileComplete) {
      setSection('chat')
    }
  }

  async function handleProfileReset(refreshConversation = true) {
    setProfileComplete(false)
    setSection('settings')
    setHistoryError(null)
    if (!refreshConversation) {
      setMessages([])
      setConversationIdentityId(null)
      return
    }
    if (!identity) {
      setMessages([])
      setConversationIdentityId(null)
      return
    }
    try {
      const history = await getConversationHistory()
      setMessages(toChatMessages(history))
      setConversationIdentityId(identity.id)
    } catch (cause) {
      setHistoryError(
        cause instanceof ApiError ? cause.message : 'Could not refresh your saved conversation.',
      )
    }
  }

  async function deliverMessage(id: string, content: string) {
    setPendingId(id)
    setMessages((current) =>
      current.map((message) =>
        message.id === id ? { ...message, failure: undefined } : message,
      ),
    )
    try {
      const result = await sendChatMessage(content)
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: result.response,
          createdAt: new Date().toISOString(),
        },
      ])
    } catch (cause) {
      const error =
        cause instanceof ApiError
          ? cause.message
          : 'The message could not be sent. Please try again.'
      setMessages((current) =>
        current.map((message) =>
          message.id === id ? { ...message, failure: error } : message,
        ),
      )
    } finally {
      setPendingId(null)
    }
  }

  function sendMessage(content: string) {
    if (pendingId) return
    const id = crypto.randomUUID()
    setMessages((current) => [
      ...current,
      {
        id,
        role: 'user',
        content,
        createdAt: new Date().toISOString(),
      },
    ])
    void deliverMessage(id, content)
  }

  function retryMessage(message: ChatMessage) {
    if (pendingId) return
    void deliverMessage(message.id, message.content)
  }

  const companionName = identity?.name || 'Your companion'

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#" onClick={(event) => event.preventDefault()}>
          <span className="brand-mark"><Sparkles size={17} /></span>
          <span>companion<span className="brand-period">.</span></span>
        </a>

        <div className="sidebar-label">YOUR SPACE</div>
        <nav className="primary-nav" aria-label="Main navigation">
          {navigation.map(({ id, label, icon: Icon }) => (
            <button
              className={`nav-link${section === id ? ' active' : ''}`}
              type="button"
              key={id}
              aria-current={section === id ? 'page' : undefined}
              disabled={id !== 'settings' && (!identity || !profileComplete)}
              onClick={() => id === 'settings' || (identity && profileComplete) ? setSection(id) : undefined}
            >
              <Icon size={17} strokeWidth={1.8} />
              <span>{label}</span>
              {id === 'chat' && messages.length > 0 && (
                <span className="nav-count">{messages.length}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-orb-wrap">
            <PortraitImage
              identity={identity}
              name={companionName}
              className="sidebar-portrait"
            />
            <span className="sidebar-glint" />
          </div>
          <p className="sidebar-whisper">
            A quieter corner<br />of your day.
          </p>
          <div className="sidebar-caption">
            <span className={`presence-dot${pendingId ? ' dot-thinking' : ''}`} />
            {pendingId ? 'Listening closely' : 'Here when you need'}
          </div>
          <div className="sidebar-rule" />
          <div className="privacy-note">
            <Heart size={14} />
            <span>Your conversation stays yours.</span>
          </div>
        </div>
      </aside>

      <section className="workspace">
        <header className="mobile-header">
          <a className="brand" href="#" onClick={(event) => event.preventDefault()}>
            <span className="brand-mark"><Sparkles size={16} /></span>
            <span>companion<span className="brand-period">.</span></span>
          </a>
          <span className="mobile-name">{companionName}</span>
        </header>

        <div className="workspace-content">
          {section === 'chat' && identity && (
            <ChatScreen
              companionName={companionName}
              identity={identity}
              messages={conversationIdentityId === identity.id ? messages : []}
              pendingId={pendingId}
              historyLoading={historyLoading}
              historyError={historyError}
              onSend={sendMessage}
              onRetry={retryMessage}
            />
          )}
          {section === 'memory' && identity && <MemoryPanel />}
          {section === 'settings' && (
            <SettingsPanel
              identity={identity}
              identityLoading={identityLoading}
              loadError={identityLoadError}
              onIdentityChange={handleIdentityChange}
              onReload={loadIdentity}
              startAtProfile={!profileComplete}
              onProfileComplete={() => setProfileComplete(true)}
              onProfileReset={() => void handleProfileReset()}
            />
          )}
        </div>

        <nav className="mobile-nav" aria-label="Main navigation">
          {navigation.map(({ id, label, icon: Icon }) => (
            <button
              className={`mobile-nav-link${section === id ? ' active' : ''}`}
              type="button"
              key={id}
              aria-current={section === id ? 'page' : undefined}
              disabled={id !== 'settings' && (!identity || !profileComplete)}
              onClick={() => id === 'settings' || (identity && profileComplete) ? setSection(id) : undefined}
            >
              <Icon size={19} strokeWidth={1.8} />
              <span>{label}</span>
            </button>
          ))}
        </nav>
      </section>

      <aside className="presence-aside">
        <div className="presence-aside-top">
          <span className="eyebrow">A LITTLE PRESENCE</span>
          <span className="presence-status-mark" aria-hidden="true"><span /></span>
        </div>
        <div className="presence-orb">
          <PortraitImage
            identity={identity}
            name={companionName}
            className={`presence-portrait${pendingId ? ' portrait-thinking' : ''}`}
          />
        </div>
        <p className="presence-overline">
          {pendingId ? 'WITH YOU' : 'IN THIS MOMENT'}
        </p>
        <h2>{identity?.name || 'A space for you'}</h2>
        <p className="presence-description">
          {identity?.personality
            ? identity.personality
            : 'A gentle place to pause, share a thought, or just be.'}
        </p>
        <div className="presence-divider" />
        <button
          className="presence-link"
          type="button"
          onClick={() => setSection('settings')}
        >
          <span>Get to know them</span>
          <span aria-hidden="true">↗</span>
        </button>
      </aside>
    </main>
  )
}

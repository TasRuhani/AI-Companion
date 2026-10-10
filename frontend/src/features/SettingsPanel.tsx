import { useEffect, useState } from 'react'
import { LoaderCircle, Save, UserRound, UserRoundCog } from 'lucide-react'
import {
  getPersonalFacts,
  getUserProfile,
  resetAll,
  resetUserProfile,
  saveUserProfile,
} from '../api/settings'
import { ApiError } from '../api/types'
import type {
  CompanionIdentity,
  PersonalFact,
  UserProfileFields,
} from '../api/types'
import IdentityPanel from './IdentityPanel'

type Tab = 'profile' | 'companion' | 'resets'

const emptyProfile: UserProfileFields = {
  name: '',
  preferredNickname: '',
  age: null,
  gender: '',
  bio: '',
  interests: '',
  favoriteTopics: '',
  communicationPreferences: '',
  thingsToKnow: '',
  topicsToAvoid: '',
}

const profileFields: {
  key: keyof UserProfileFields
  label: string
  rows: number
  hint: string
}[] = [
  { key: 'name', label: 'Your name', rows: 1, hint: 'How you would like to be known.' },
  { key: 'preferredNickname', label: 'Preferred name', rows: 1, hint: 'What your companion should call you.' },
  { key: 'age', label: 'Age', rows: 1, hint: 'Optional.' },
  { key: 'gender', label: 'Gender', rows: 1, hint: 'Optional.' },
  { key: 'bio', label: 'About you', rows: 3, hint: 'Anything you want your companion to know about you.' },
  { key: 'interests', label: 'Interests', rows: 3, hint: 'Your hobbies, interests, or current pursuits.' },
  { key: 'favoriteTopics', label: 'Favorite topics', rows: 2, hint: 'Things you enjoy talking about.' },
  { key: 'communicationPreferences', label: 'Conversation preferences', rows: 3, hint: 'Tone, pacing, or response preferences.' },
  { key: 'thingsToKnow', label: 'Important context', rows: 3, hint: 'Personal context you want kept in mind.' },
  { key: 'topicsToAvoid', label: 'Topics to avoid', rows: 2, hint: 'Topics your companion should handle carefully or avoid.' },
]

const tabs: { id: Tab; label: string }[] = [
  { id: 'profile', label: 'My profile' },
  { id: 'companion', label: 'Companion' },
  { id: 'resets', label: 'Reset data' },
]

interface SettingsPanelProps {
  identity: CompanionIdentity | null
  identityLoading: boolean
  loadError: string | null
  onIdentityChange: (identity: CompanionIdentity | null) => void
  onReload: () => Promise<void>
  startAtProfile: boolean
  onProfileComplete: () => void
  onProfileReset: (refreshConversation?: boolean) => void
}

export default function SettingsPanel(props: SettingsPanelProps) {
  const [tab, setTab] = useState<Tab>(props.startAtProfile ? 'profile' : 'companion')
  const [profile, setProfile] = useState<UserProfileFields>(emptyProfile)
  const [profileExists, setProfileExists] = useState(false)
  const [facts, setFacts] = useState<PersonalFact[]>([])
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  async function loadSettings() {
    setLoading(true)
    setError(null)
    try {
      const [savedProfile, savedFacts] = await Promise.all([
        getUserProfile(),
        getPersonalFacts(),
      ])
      if (savedProfile) {
        setProfile({
          name: savedProfile.name,
          preferredNickname: savedProfile.preferredNickname ?? '',
          age: savedProfile.age,
          gender: savedProfile.gender ?? '',
          bio: savedProfile.bio ?? '',
          interests: savedProfile.interests ?? '',
          favoriteTopics: savedProfile.favoriteTopics ?? '',
          communicationPreferences: savedProfile.communicationPreferences ?? '',
          thingsToKnow: savedProfile.thingsToKnow ?? '',
          topicsToAvoid: savedProfile.topicsToAvoid ?? '',
        })
        setProfileExists(true)
        props.onProfileComplete()
      } else {
        setProfile(emptyProfile)
        setProfileExists(false)
      }
      setFacts(savedFacts)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Could not load settings.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadSettings()
  }, [])

  async function saveProfile(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (working) return
    setWorking(true)
    setError(null)
    setNotice(null)
    try {
      const saved = await saveUserProfile({
        ...profile,
        name: profile.name.trim(),
        preferredNickname: profile.preferredNickname?.trim() || null,
        gender: profile.gender?.trim() || null,
        bio: profile.bio?.trim() || null,
        interests: profile.interests?.trim() || null,
        favoriteTopics: profile.favoriteTopics?.trim() || null,
        communicationPreferences: profile.communicationPreferences?.trim() || null,
        thingsToKnow: profile.thingsToKnow?.trim() || null,
        topicsToAvoid: profile.topicsToAvoid?.trim() || null,
      })
      setProfileExists(true)
      props.onProfileComplete()
      setProfile({
        ...profile,
        name: saved.name,
        preferredNickname: saved.preferredNickname ?? '',
        age: saved.age,
      })
      setNotice('Your profile is saved and will guide the next conversation response.')
      if (!props.identity) setTab('companion')
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Could not save your profile.')
    } finally {
      setWorking(false)
    }
  }

  async function performReset(kind: 'profile' | 'all') {
    const message = kind === 'profile'
      ? 'Reset your profile and all personal facts? Companion identity and conversation will remain.'
      : 'Reset all companion and personal data? Saved identities and their portrait files will also be removed.'
    if (!window.confirm(message) || working) return
    setWorking(true)
    setError(null)
    setNotice(null)
    try {
      if (kind === 'profile') {
        await resetUserProfile()
        setProfile(emptyProfile)
        setProfileExists(false)
        setFacts([])
        setTab('profile')
        props.onProfileReset()
        setNotice('Your profile and personal facts were reset. Matching private text was removed from history and memories; other companion data was preserved.')
      } else {
        await resetAll()
        setProfile(emptyProfile)
        setProfileExists(false)
        setFacts([])
        props.onIdentityChange(null)
        props.onProfileReset(false)
        setTab('profile')
        setNotice('All companion and personal data was reset, including saved identities and their portraits.')
      }
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'The requested reset failed.')
    } finally {
      setWorking(false)
    }
  }

  return (
    <section className="secondary-panel">
      <header className="section-heading">
        <div>
          <p className="eyebrow">YOUR COMPANION, YOUR WAY</p>
          <h1>Settings</h1>
          <p className="section-intro">
            Your profile, companion identity, and data controls.
          </p>
        </div>
        <div className="section-icon"><UserRoundCog size={19} /></div>
      </header>

      <nav className="settings-tabs" aria-label="Settings sections">
        {tabs.map((item) => (
          <button
            type="button"
            className={tab === item.id ? 'selected' : ''}
            key={item.id}
            onClick={() => setTab(item.id)}
          >
            {item.label}
          </button>
        ))}
      </nav>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}
      {loading ? (
        <div className="panel-loading" role="status"><LoaderCircle className="spin" size={17} /> Loading settings…</div>
      ) : tab === 'profile' ? (
        <form className="identity-form" onSubmit={(event) => void saveProfile(event)}>
          <div className="form-heading">
            <div><p className="eyebrow">ABOUT YOU</p><h2>{profileExists ? 'Your user profile' : 'Set up your profile'}</h2></div>
            <UserRound size={18} />
          </div>
          {!profileExists && (
            <div className="setup-context">
              <p>Start by sharing what you want your companion to know. Your profile is stored locally in the app database and can be edited or cleared any time.</p>
            </div>
          )}
          <div className="identity-fields">
            {profileFields.map(({ key, label, rows, hint }) => (
              <label className={`field${rows > 1 ? ' field-full' : ''}`} key={key}>
                <span>{label}</span>
                {key === 'age' ? (
                  <input
                    type="number"
                    min="1"
                    max="130"
                    value={profile.age ?? ''}
                    placeholder={hint}
                    onChange={(event) => setProfile((current) => ({
                      ...current,
                      age: event.target.value ? Number(event.target.value) : null,
                    }))}
                  />
                ) : rows > 1 ? (
                  <textarea
                    rows={rows}
                    value={(profile[key] as string | null) ?? ''}
                    placeholder={hint}
                    onChange={(event) => setProfile((current) => ({ ...current, [key]: event.target.value }))}
                  />
                ) : (
                  <input
                    required={key === 'name'}
                    value={(profile[key] as string | null) ?? ''}
                    placeholder={hint}
                    onChange={(event) => setProfile((current) => ({ ...current, [key]: event.target.value }))}
                  />
                )}
              </label>
            ))}
          </div>
          <button className="button button-primary" type="submit" disabled={working}>
            {working ? <LoaderCircle className="spin" size={15} /> : <Save size={15} />}
            Save profile
          </button>
          {profileExists && <p className="quiet-note">Changes are used by the next companion response. A preferred name takes precedence over your account name.</p>}
        </form>
      ) : tab === 'companion' ? (
        <IdentityPanel
          key={props.identity?.id ?? 'new-identity'}
          identity={props.identity}
          identityLoading={props.identityLoading}
          loadError={props.loadError}
          onIdentityChange={props.onIdentityChange}
          onReload={props.onReload}
        />
      ) : (
        <div className="settings-section">
          <div className="form-heading"><div><p className="eyebrow">DATA CONTROLS</p><h2>Choose what to reset</h2></div></div>
          <article className="reset-option">
            <h3>Reset companion</h3>
            <p>Clears the current companion conversation, memories, profile facts, mood state, and active identity. Portrait files and the user profile are preserved.</p>
            <button className="button button-quiet" type="button" onClick={() => setTab('companion')}>Open companion controls</button>
          </article>
          <article className="reset-option">
            <h3>Reset my profile</h3>
            <p>Deletes your profile and personal facts. Matching text is removed from saved conversation history, and memories containing those details are deleted; other companion data remains.</p>
            <button className="button button-danger" type="button" disabled={working || !profileExists && facts.length === 0} onClick={() => void performReset('profile')}>Reset my profile</button>
          </article>
          <article className="reset-option reset-option-danger">
            <h3>Reset all</h3>
            <p>Clears companion state and user profile/facts, including all identity records and associated portrait files.</p>
            <button className="button button-danger" type="button" disabled={working} onClick={() => void performReset('all')}>Reset all data</button>
          </article>
        </div>
      )}
    </section>
  )
}

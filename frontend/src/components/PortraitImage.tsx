import { useState } from 'react'
import { RotateCcw } from 'lucide-react'
import type { CompanionIdentity } from '../api/types'
import { portraitUrl } from '../api/portrait'
import CompanionOrb from './CompanionOrb'

interface PortraitImageProps {
  identity: CompanionIdentity | null
  name: string
  className: string
}

export default function PortraitImage({
  identity,
  name,
  className,
}: PortraitImageProps) {
  const [failedPortraitId, setFailedPortraitId] = useState<string | null>(null)
  const [retryCount, setRetryCount] = useState(0)
  const portraitId = identity?.portraitId ?? null
  const showImage = identity !== null &&
    portraitId !== null &&
    failedPortraitId !== portraitId

  return (
    <div className={`portrait-frame ${className}`}>
      <div className="portrait-fallback" aria-hidden={showImage}>
        <CompanionOrb compact />
      </div>
      {showImage && identity && portraitId && (
        <img
          key={`${portraitId}-${retryCount}`}
          className="portrait-photo"
          src={portraitUrl(identity.id, portraitId)}
          alt={`${name} portrait`}
          onError={() => setFailedPortraitId(portraitId)}
        />
      )}
      {portraitId && failedPortraitId === portraitId && (
        <button
          className="portrait-retry"
          type="button"
          onClick={() => {
            setFailedPortraitId(null)
            setRetryCount((current) => current + 1)
          }}
        >
          <RotateCcw size={13} />
          Retry portrait
        </button>
      )}
    </div>
  )
}

interface CompanionOrbProps {
  isThinking?: boolean
  compact?: boolean
}

export default function CompanionOrb({
  isThinking = false,
  compact = false,
}: CompanionOrbProps) {
  return (
    <div
      className={`orb-scene${compact ? ' orb-scene-compact' : ''}${isThinking ? ' orb-thinking' : ''}`}
      role="img"
      aria-label={
        isThinking
          ? 'The companion is thinking'
          : 'A softly glowing abstract companion'
      }
    >
      <div className="orb-halo" />
      <div className="orb">
        <span className="orb-shine" />
        <span className="orb-core" />
      </div>
      <div className="orb-shadow" />
    </div>
  )
}

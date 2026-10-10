import { useEffect, useRef, useState } from 'react'
import {
  ImagePlus,
  LoaderCircle,
  RotateCcw,
  Trash2,
  Upload,
  X,
} from 'lucide-react'
import { uploadPortrait, removePortrait } from '../api/portrait'
import { ApiError, type CompanionIdentity } from '../api/types'
import PortraitImage from './PortraitImage'

const MAX_FILE_BYTES = 5 * 1024 * 1024
const MAX_IMAGE_DIMENSION = 8192
const MAX_IMAGE_PIXELS = 40_000_000

interface PortraitUploaderProps {
  identity: CompanionIdentity | null
  name: string
  onIdentityChange: (identity: CompanionIdentity) => void
  compact?: boolean
  onNeedIdentity?: () => void
}

export default function PortraitUploader({
  identity,
  name,
  onIdentityChange,
  compact = false,
  onNeedIdentity,
}: PortraitUploaderProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    if (!selectedFile) {
      setPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(selectedFile)
    setPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [selectedFile])

  function dismissDialog() {
    if (working) return
    setDialogOpen(false)
    setSelectedFile(null)
    setError(null)
    if (inputRef.current) inputRef.current.value = ''
  }

  async function chooseFile(file: File | undefined) {
    setError(null)
    setNotice(null)
    if (!file) return
    if (file.type !== 'image/png' || !file.name.toLowerCase().endsWith('.png')) {
      setSelectedFile(null)
      setError('Choose a PNG image.')
      return
    }
    if (file.size > MAX_FILE_BYTES) {
      setSelectedFile(null)
      setError('This image is larger than the 5 MB upload limit.')
      return
    }

    try {
      const signature = new Uint8Array(await file.slice(0, 8).arrayBuffer())
      const pngSignature = [137, 80, 78, 71, 13, 10, 26, 10]
      if (!pngSignature.every((byte, index) => signature[index] === byte)) {
        throw new Error('The file content is not a PNG image.')
      }
      const bitmap = await createImageBitmap(file)
      const validDimensions =
        bitmap.width > 0 &&
        bitmap.height > 0 &&
        bitmap.width <= MAX_IMAGE_DIMENSION &&
        bitmap.height <= MAX_IMAGE_DIMENSION &&
        bitmap.width * bitmap.height <= MAX_IMAGE_PIXELS
      bitmap.close()
      if (!validDimensions) {
        throw new Error('The PNG dimensions are too large.')
      }
      setSelectedFile(file)
    } catch (cause) {
      setSelectedFile(null)
      setError(
        cause instanceof Error
          ? cause.message
          : 'This PNG could not be decoded. Try another image.',
      )
    }
  }

  async function confirmUpload() {
    if (!identity || !selectedFile || working) return
    setWorking(true)
    setError(null)
    setNotice(null)
    try {
      const updatedIdentity = await uploadPortrait(identity.id, selectedFile)
      onIdentityChange(updatedIdentity)
      setNotice('Portrait saved to this companion.')
      setDialogOpen(false)
      setSelectedFile(null)
      if (inputRef.current) inputRef.current.value = ''
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : 'The portrait could not be uploaded. Your existing portrait is unchanged.',
      )
    } finally {
      setWorking(false)
    }
  }

  async function confirmRemove() {
    if (!identity?.portraitId || working) return
    if (!window.confirm(`Remove ${name}'s portrait?`)) return
    setWorking(true)
    setError(null)
    setNotice(null)
    try {
      await removePortrait(identity.id)
      onIdentityChange({ ...identity, portraitId: null })
      setNotice('Portrait removed.')
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : 'The portrait could not be removed.',
      )
    } finally {
      setWorking(false)
    }
  }

  return (
    <div className={`portrait-uploader${compact ? ' portrait-uploader-compact' : ''}`}>
      <input
        ref={inputRef}
        className="sr-only"
        type="file"
        accept="image/png,.png"
        aria-label="Choose PNG character portrait"
        onChange={(event) => void chooseFile(event.target.files?.[0])}
      />

      {!compact && (
        <PortraitImage
          identity={identity}
          name={name}
          className="identity-portrait"
        />
      )}

      <div className="portrait-actions">
        {!identity ? (
          <button
            className="button button-quiet"
            type="button"
            onClick={onNeedIdentity}
            disabled={!onNeedIdentity}
          >
            <ImagePlus size={15} />
            Create an identity to add a portrait
          </button>
        ) : (
          <>
            <button
              className="button button-quiet"
              type="button"
              disabled={working}
              onClick={() => {
                setError(null)
                setNotice(null)
                setDialogOpen(true)
              }}
            >
              <Upload size={15} />
              {identity.portraitId ? 'Replace portrait' : 'Upload character portrait'}
            </button>
            {identity.portraitId && (
              <button
                className="button button-quiet button-danger"
                type="button"
                disabled={working}
                onClick={() => void confirmRemove()}
              >
                {working ? <LoaderCircle className="spin" size={14} /> : <Trash2 size={14} />}
                Remove
              </button>
            )}
          </>
        )}
      </div>

      {notice && <p className="portrait-notice" role="status">{notice}</p>}
      {error && !dialogOpen && (
        <div className="portrait-inline-error" role="alert">
          <span>{error}</span>
          {identity && (
            <button
              className="text-action"
              type="button"
              onClick={() => setDialogOpen(true)}
            >
              Try again
            </button>
          )}
        </div>
      )}

      {dialogOpen && identity && (
        <div
          className="portrait-dialog-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) dismissDialog()
          }}
        >
          <section
            className="portrait-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="portrait-dialog-title"
            onKeyDown={(event) => {
              if (event.key === 'Escape') dismissDialog()
            }}
          >
            <header className="portrait-dialog-heading">
              <div>
                <p className="eyebrow">A FACE TO GO WITH THE VOICE</p>
                <h2 id="portrait-dialog-title">
                  {identity.portraitId ? 'Replace portrait' : 'Upload character portrait'}
                </h2>
              </div>
              <button
                className="icon-button"
                type="button"
                aria-label="Cancel portrait upload"
                disabled={working}
                onClick={dismissDialog}
              >
                <X size={17} />
              </button>
            </header>

            <button
              className={`portrait-dropzone${dragging ? ' portrait-dropzone-active' : ''}${previewUrl ? ' portrait-dropzone-preview' : ''}`}
              type="button"
              disabled={working}
              onClick={() => inputRef.current?.click()}
              onDragOver={(event) => {
                event.preventDefault()
                setDragging(true)
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={(event) => {
                event.preventDefault()
                setDragging(false)
                void chooseFile(event.dataTransfer.files[0])
              }}
            >
              {previewUrl ? (
                <>
                  <img src={previewUrl} alt="Preview of selected character portrait" />
                  <span className="portrait-preview-label">
                    {selectedFile?.name} · {(selectedFile?.size ?? 0) / 1024 / 1024 < 1
                      ? `${Math.max(1, Math.round((selectedFile?.size ?? 0) / 1024))} KB`
                      : `${((selectedFile?.size ?? 0) / 1024 / 1024).toFixed(1)} MB`}
                  </span>
                </>
              ) : (
                <>
                  <span className="portrait-drop-icon"><ImagePlus size={21} /></span>
                  <strong>Choose a PNG portrait</strong>
                  <span>Drag and drop an image here, or browse your files</span>
                  <span className="portrait-file-limit">PNG · up to 5 MB</span>
                </>
              )}
            </button>

            {error && <div className="notice notice-error" role="alert">{error}</div>}
            <div className="portrait-dialog-actions">
              <button
                className="button button-quiet"
                type="button"
                disabled={working}
                onClick={dismissDialog}
              >
                Cancel
              </button>
              <button
                className="button button-primary"
                type="button"
                disabled={!selectedFile || working}
                onClick={() => void confirmUpload()}
              >
                {working ? (
                  <>
                    <LoaderCircle className="spin" size={15} />
                    Saving portrait…
                  </>
                ) : error && selectedFile ? (
                  <>
                    <RotateCcw size={15} />
                    Retry upload
                  </>
                ) : (
                  <>
                    <Upload size={15} />
                    Save portrait
                  </>
                )}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

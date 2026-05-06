export const IMAGE_COMPRESS_MIN_BYTES = 900 * 1024
export const IMAGE_COMPRESS_MAX_SIDE = 1600
export const IMAGE_COMPRESS_QUALITY = 0.82

const COMPRESSIBLE_IMAGE_TYPES = new Set([
  'image/jpeg',
  'image/png',
  'image/webp'
])

export function shouldCompressImage(file) {
  if (!file || !COMPRESSIBLE_IMAGE_TYPES.has(file.type)) {
    return false
  }
  return file.size > IMAGE_COMPRESS_MIN_BYTES
}

export async function prepareUploadFiles(items, onProgress) {
  const list = Array.isArray(items) ? items : []
  const total = list.length
  const prepared = []

  for (let index = 0; index < total; index++) {
    const item = list[index]
    const file = item?.file
    let nextFile = file

    if (shouldCompressImage(file)) {
      nextFile = await compressImage(file)
    }

    prepared.push({
      ...item,
      file: nextFile,
      status: item?.status || 'done'
    })

    onProgress?.({
      current: index + 1,
      total,
      percent: total > 0 ? Math.round(((index + 1) / total) * 100) : 100
    })
  }

  return prepared
}

async function compressImage(file) {
  if (typeof document === 'undefined' || typeof Image === 'undefined') {
    return file
  }

  const dataUrl = await readAsDataUrl(file)
  const image = await loadImage(dataUrl)
  const { width, height } = getTargetSize(image.width, image.height)

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) {
    return file
  }

  context.drawImage(image, 0, 0, width, height)
  const outputType = file.type === 'image/png' ? 'image/jpeg' : file.type
  const blob = await canvasToBlob(canvas, outputType, IMAGE_COMPRESS_QUALITY)
  if (!blob || blob.size >= file.size) {
    return file
  }

  const extension = outputType === 'image/jpeg' ? '.jpg' : ''
  const filename = outputType === file.type ? file.name : replaceExtension(file.name, extension)
  return new File([blob], filename, {
    type: outputType,
    lastModified: Date.now()
  })
}

function getTargetSize(width, height) {
  const maxSide = Math.max(width, height)
  if (maxSide <= IMAGE_COMPRESS_MAX_SIDE) {
    return { width, height }
  }
  const scale = IMAGE_COMPRESS_MAX_SIDE / maxSide
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale))
  }
}

function readAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(reader.error || new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('图片处理失败'))
    image.src = src
  })
}

function canvasToBlob(canvas, type, quality) {
  return new Promise(resolve => {
    canvas.toBlob(blob => resolve(blob), type, quality)
  })
}

function replaceExtension(name, extension) {
  const dotIndex = name.lastIndexOf('.')
  if (dotIndex <= 0) {
    return `${name}${extension}`
  }
  return `${name.slice(0, dotIndex)}${extension}`
}

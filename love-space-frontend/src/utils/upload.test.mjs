import test from 'node:test'
import assert from 'node:assert/strict'
import {
  IMAGE_COMPRESS_MIN_BYTES,
  prepareUploadFiles,
  shouldCompressImage
} from './upload.js'

function makeFile(name, type, size) {
  return new File([new Uint8Array(size)], name, { type })
}

test('shouldCompressImage only selects large still images', () => {
  assert.equal(shouldCompressImage(makeFile('large.jpg', 'image/jpeg', IMAGE_COMPRESS_MIN_BYTES + 1)), true)
  assert.equal(shouldCompressImage(makeFile('small.jpg', 'image/jpeg', IMAGE_COMPRESS_MIN_BYTES - 1)), false)
  assert.equal(shouldCompressImage(makeFile('anim.gif', 'image/gif', IMAGE_COMPRESS_MIN_BYTES + 1)), false)
  assert.equal(shouldCompressImage(makeFile('clip.mp4', 'video/mp4', IMAGE_COMPRESS_MIN_BYTES + 1)), false)
})

test('prepareUploadFiles leaves videos untouched and reports progress', async () => {
  const video = makeFile('clip.mp4', 'video/mp4', 1024)
  const progress = []

  const prepared = await prepareUploadFiles([{ file: video, status: 'done' }], event => progress.push(event))

  assert.equal(prepared.length, 1)
  assert.equal(prepared[0].file, video)
  assert.equal(prepared[0].status, 'done')
  assert.deepEqual(progress.at(-1), { current: 1, total: 1, percent: 100 })
})

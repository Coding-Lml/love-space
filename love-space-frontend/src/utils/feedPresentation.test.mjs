import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { describeMomentMedia, getMomentAccent, visibilityLabel } from './feedPresentation.js'

describe('feedPresentation', () => {
  it('summarizes mixed media for moment cards', () => {
    assert.equal(
      describeMomentMedia([
        { type: 'image' },
        { type: 'video' },
        { type: 'image' }
      ]),
      '2 张照片 · 1 段视频'
    )
  })

  it('returns an empty summary when no media exists', () => {
    assert.equal(describeMomentMedia([]), '')
    assert.equal(describeMomentMedia(null), '')
  })

  it('uses stable accent colors from moment id', () => {
    assert.equal(getMomentAccent({ id: 1 }), '#ff7a59')
    assert.equal(getMomentAccent({ id: 6 }), '#ff7a59')
  })

  it('labels public and private feed visibility', () => {
    assert.equal(visibilityLabel('PUBLIC'), '广场可见')
    assert.equal(visibilityLabel('SPACE'), '只在我们之间')
  })
})

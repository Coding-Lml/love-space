import test from 'node:test'
import assert from 'node:assert/strict'
import { createDiaryListRequestState } from './diaryListState.js'

test('diary list request state is independent from Vant loading', () => {
  const requestState = createDiaryListRequestState()

  assert.equal(requestState.start(), true)
  assert.equal(requestState.start(), false)

  requestState.finish()

  assert.equal(requestState.start(), true)
})

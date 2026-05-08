export function createDiaryListRequestState() {
  let requesting = false

  return {
    start() {
      if (requesting) return false
      requesting = true
      return true
    },
    finish() {
      requesting = false
    }
  }
}

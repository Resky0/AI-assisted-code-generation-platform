const WRITE_FILE_OUTPUT_MARKER = '[工具调用] 写入文件 '
const DEFAULT_INTERVAL_MS = 40
const SINGLE_FILE_DURATION_MS = 3000
const BACKLOG_DURATION_MS = 4000

interface SegmentData {
  segment: string
}

interface SegmenterLike {
  segment(input: string): Iterable<SegmentData>
}

interface SegmenterConstructorLike {
  new (
    locales?: string | string[],
    options?: { granularity: 'grapheme' },
  ): SegmenterLike
}

interface ParsedWriteFileOutput {
  prefix: string
  code: string
  suffix: string
}

interface InstantQueueItem {
  kind: 'instant'
  content: string
}

interface AnimatedQueueItem extends ParsedWriteFileOutput {
  kind: 'animated'
  graphemes: string[]
  cursor: number
  started: boolean
}

type QueueItem = InstantQueueItem | AnimatedQueueItem

export interface TypewriterController {
  enqueue(content: string): void
  complete(): void
  cancel(): void
}

interface TypewriterOptions {
  append(content: string): void
  onDispose?: () => void
}

let cachedSegmenter: SegmenterLike | null | undefined

const splitGraphemes = (content: string): string[] => {
  if (cachedSegmenter === undefined) {
    const Segmenter = (
      Intl as unknown as {
        Segmenter?: SegmenterConstructorLike
      }
    ).Segmenter
    cachedSegmenter = Segmenter ? new Segmenter(undefined, { granularity: 'grapheme' }) : null
  }

  if (cachedSegmenter) {
    return Array.from(cachedSegmenter.segment(content), ({ segment }) => segment)
  }

  // Array.from 至少能保证代理对（例如 Emoji）不会被拆成两个无效字符
  return Array.from(content)
}

const parseWriteFileOutput = (content: string): ParsedWriteFileOutput | null => {
  const displayStart = content.search(/\S/)
  if (displayStart < 0 || !content.slice(displayStart).startsWith(WRITE_FILE_OUTPUT_MARKER)) {
    return null
  }

  const openingFenceStart = content.indexOf('```', displayStart)
  const openingFenceEnd =
    openingFenceStart >= 0 ? content.indexOf('\n', openingFenceStart) : -1
  const closingFenceStart = content.lastIndexOf('\n```')

  if (
    openingFenceStart < 0 ||
    openingFenceEnd < 0 ||
    closingFenceStart <= openingFenceEnd
  ) {
    return null
  }

  return {
    prefix: content.slice(0, openingFenceEnd + 1),
    code: content.slice(openingFenceEnd + 1, closingFenceStart),
    suffix: content.slice(closingFenceStart),
  }
}

/**
 * 将 writeFile 工具输出按顺序渐进显示，其他内容仍立即显示。
 * 网络接收与视觉动画相互独立，队列只负责保证页面上的内容顺序。
 */
export const createWriteFileTypewriter = ({
  append,
  onDispose,
}: TypewriterOptions): TypewriterController => {
  const queue: QueueItem[] = []
  let timerId: number | undefined
  let animationDeadline = 0
  let inputCompleted = false
  let cancelled = false
  let disposed = false

  const dispose = () => {
    if (disposed) return
    disposed = true
    onDispose?.()
  }

  const appendToView = (content: string) => {
    if (content) append(content)
  }

  const pendingAnimatedCount = () =>
    queue.reduce((count, item) => count + (item.kind === 'animated' ? 1 : 0), 0)

  const pendingGraphemeCount = () =>
    queue.reduce(
      (count, item) =>
        count + (item.kind === 'animated' ? item.graphemes.length - item.cursor : 0),
      0,
    )

  const finishIfPossible = () => {
    if (inputCompleted && queue.length === 0 && timerId === undefined) {
      dispose()
    }
  }

  const scheduleTick = () => {
    if (cancelled || timerId !== undefined || pendingAnimatedCount() === 0) return
    timerId = window.setTimeout(runTick, DEFAULT_INTERVAL_MS)
  }

  const revealReadyPrefixes = () => {
    let output = ''

    while (queue.length > 0) {
      const item = queue[0]
      if (item.kind === 'instant') {
        output += item.content
        queue.shift()
        continue
      }

      if (!item.started) {
        output += item.prefix
        item.started = true
      }
      break
    }

    appendToView(output)
    scheduleTick()
    finishIfPossible()
  }

  function runTick() {
    timerId = undefined
    if (cancelled) return

    const remainingGraphemes = pendingGraphemeCount()
    const remainingDuration = Math.max(DEFAULT_INTERVAL_MS, animationDeadline - Date.now())
    const remainingTicks = Math.max(1, Math.ceil(remainingDuration / DEFAULT_INTERVAL_MS))
    let budget = Math.max(1, Math.ceil(remainingGraphemes / remainingTicks))
    let output = ''

    while (queue.length > 0) {
      const item = queue[0]

      if (item.kind === 'instant') {
        output += item.content
        queue.shift()
        continue
      }

      if (!item.started) {
        output += item.prefix
        item.started = true
      }

      const remaining = item.graphemes.length - item.cursor
      const takeCount = Math.min(remaining, budget)
      if (takeCount > 0) {
        output += item.graphemes.slice(item.cursor, item.cursor + takeCount).join('')
        item.cursor += takeCount
        budget -= takeCount
      }

      if (item.cursor === item.graphemes.length) {
        output += item.suffix
        queue.shift()
        if (budget > 0) continue
      }
      break
    }

    appendToView(output)
    revealReadyPrefixes()
  }

  const enqueue = (content: string) => {
    if (cancelled || !content) return

    const parsed = parseWriteFileOutput(content)
    if (!parsed) {
      queue.push({ kind: 'instant', content })
      revealReadyPrefixes()
      return
    }

    const alreadyAnimating = pendingAnimatedCount() > 0
    queue.push({
      kind: 'animated',
      ...parsed,
      graphemes: splitGraphemes(parsed.code),
      cursor: 0,
      started: false,
    })
    animationDeadline =
      Date.now() + (alreadyAnimating ? BACKLOG_DURATION_MS : SINGLE_FILE_DURATION_MS)
    revealReadyPrefixes()
  }

  const complete = () => {
    if (cancelled) return
    inputCompleted = true
    revealReadyPrefixes()
  }

  const cancel = () => {
    if (cancelled) return
    cancelled = true
    queue.length = 0
    if (timerId !== undefined) {
      window.clearTimeout(timerId)
      timerId = undefined
    }
    dispose()
  }

  return { enqueue, complete, cancel }
}

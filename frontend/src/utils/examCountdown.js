const examSchedules = {
  cet4: {
    label: '下次考试',
    date: '2026-12-12'
  },
  cet6: {
    label: '下次考试',
    date: '2026-12-12'
  },
  toefl: {
    label: '最近场次',
    date: '2026-07-12'
  },
  ielts: {
    label: '最近场次',
    date: '2026-07-19'
  },
  kaoyan: {
    label: '初试时间',
    date: '2026-12-20'
  },
  gre: {
    label: '最近场次',
    date: '2026-07-26'
  }
}

function parseDate(dateString) {
  const [year, month, day] = dateString.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function startOfToday() {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}.${month}.${day}`
}

export function getExamCountdown(moduleCode) {
  const schedule = examSchedules[moduleCode]

  if (!schedule) {
    return {
      label: '考试安排',
      dateText: '待更新',
      countdownText: '敬请期待',
      isUrgent: false
    }
  }

  const examDate = parseDate(schedule.date)
  const today = startOfToday()
  const diffDays = Math.ceil((examDate - today) / 86400000)

  let countdownText = '已开考'
  if (diffDays > 0) {
    countdownText = `倒计时 ${diffDays} 天`
  } else if (diffDays === 0) {
    countdownText = '今天考试'
  }

  return {
    label: schedule.label,
    dateText: formatDate(examDate),
    countdownText,
    isUrgent: diffDays >= 0 && diffDays <= 30
  }
}

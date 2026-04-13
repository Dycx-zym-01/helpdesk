import { getDateLocale } from '@/i18n'

export function formatDateTime(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat(getDateLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

export function statusTagType(status) {
  return {
    DRAFT: 'info',
    NEW: 'info',
    PENDING: 'warning',
    PROCESSING: 'primary',
    RESOLVED: 'success',
    CLOSED: 'success'
  }[status] || 'info'
}

export function priorityTagType(priority) {
  return {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger',
    URGENT: 'danger'
  }[priority] || 'info'
}

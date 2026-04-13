import { computed, ref } from 'vue'

const LOCALE_KEY = 'helpdesk-locale'
const SUPPORTED_LOCALES = ['zh', 'en']

function readStoredLocale() {
  if (typeof window === 'undefined') {
    return 'zh'
  }
  const saved = window.localStorage.getItem(LOCALE_KEY)
  return SUPPORTED_LOCALES.includes(saved) ? saved : 'zh'
}

const localeState = ref(readStoredLocale())

const messages = {
  zh: {
    common: {
      appName: '系统问题反馈平台',
      requestFailed: '请求失败',
      cancel: '取消',
      confirm: '确认',
      save: '保存',
      search: '查询',
      reset: '重置',
      refresh: '刷新数据',
      detail: '详情',
      edit: '编辑',
      delete: '删除',
      actions: '操作',
      backToList: '返回列表',
      submit: '提交',
      export: '导出数据',
      unassigned: '未分配',
      noData: '暂无数据',
      enabled: '启用',
      disabled: '禁用'
    },
    language: {
      zh: '中文',
      en: 'EN'
    },
    nav: {
      tickets: '问题列表',
      create: '提交反馈',
      stats: '统计分析',
      users: '用户管理'
    },
    layout: {
      roleTitle: '当前角色',
      welcome: '欢迎回来，{name}',
      welcomeText: '把问题反馈、处理流转、通知提醒和统计分析统一到一个入口里。',
      notifications: '通知',
      logout: '退出登录',
      drawerTitle: '站内通知',
      unreadCount: '未读 {count} 条',
      markAllRead: '全部已读',
      markAllReadSuccess: '所有通知已标记为已读',
      relatedTicket: '关联问题：{ticketNo}',
      noNotifications: '暂无通知'
    },
    login: {
      badge: '系统问题反馈平台',
      heroTitle: '让用户在使用企业内部系统时，遇到问题可以快速反馈、跟进和确认结果。',
      heroDesc: '这套演示覆盖员工、开发人员、管理员三类角色，完整贯通问题反馈到处理闭环。',
      title: '账号登录',
      desc: '内置演示账号，点一下就能切换角色体验。',
      username: '用户名',
      usernamePlaceholder: '请输入用户名',
      password: '密码',
      passwordPlaceholder: '请输入密码',
      submit: '登录系统',
      success: '登录成功',
      demoTitle: '演示账号',
      demoAdmin: '管理员',
      demoEmployee: '普通员工',
      demoDeveloper: '开发人员'
    },
    ticketList: {
      title: '问题反馈中心',
      subtitle: '按状态、类别、优先级、处理人快速筛选，并支持直接推进处理动作。',
      status: '状态',
      category: '类别',
      priority: '优先级',
      assignee: '处理人',
      keyword: '关键字',
      quickFilter: '快捷筛选',
      mineOnly: '只看我负责的问题',
      allStatus: '全部状态',
      allCategory: '全部类别',
      allPriority: '全部优先级',
      allAssignee: '全部处理人',
      keywordPlaceholder: '标题 / 描述 / 反馈编号',
      total: '共 {count} 条记录',
      ticketNo: '反馈编号',
      titleColumn: '标题',
      creator: '提交人',
      updatedAt: '更新时间',
      assign: '分配',
      claim: '接单',
      start: '开始处理',
      resolve: '标记解决',
      assignTitle: '分派问题',
      assignPlaceholder: '请选择开发人员',
      assignSuccess: '问题分派成功',
      resolveTitle: '标记问题已解决',
      resolveComment: '处理说明',
      resolvePlaceholder: '可填写处理结果和回归建议',
      resolveSuccess: '问题已标记为解决',
      claimSuccess: '接单成功',
      startSuccess: '已开始处理',
      submitTicket: '提交反馈',
      exportSuccessName: 'feedback-list.csv',
      editDraft: '继续编辑',
      deleteDraft: '删除草稿',
      deleteDraftSuccess: '草稿已删除',
      deleteDraftConfirm: '删除后不可恢复，是否继续？'
    },
    ticketCreate: {
      title: '提交反馈',
      editTitle: '编辑草稿',
      subtitle: '员工可直接反馈使用企业内部系统时遇到的问题，系统会自动生成反馈编号。',
      ticketTitle: '问题标题',
      ticketTitlePlaceholder: '例如：ERP 登录白屏、打印机异常、权限申请',
      category: '问题类别',
      categoryPlaceholder: '请选择类别',
      priority: '优先级',
      priorityPlaceholder: '请选择优先级',
      description: '问题描述',
      descriptionPlaceholder: '请补充发生时间、影响范围、复现步骤、期望结果等关键信息',
      attachments: '附件上传',
      attachmentsHint: '将截图、日志或说明文档拖到这里，或点击选择文件',
      attachmentsTip: '支持多文件上传，单文件最大 20MB。',
      existingAttachments: '已有附件',
      saveDraft: '保存草稿',
      saveDraftSuccess: '草稿保存成功',
      submit: '提交反馈',
      submitDraft: '提交草稿',
      success: '反馈提交成功'
    },
    ticketDetail: {
      createdAtBy: '提交于 {createdAt}，当前由 {assignee} 负责。',
      ticketInfo: '问题信息',
      description: '问题描述',
      attachments: '附件',
      comments: '处理备注',
      noAttachments: '暂无附件',
      noComments: '还没有处理备注',
      commentPlaceholder: '补充处理进展、临时绕过方案或用户反馈',
      commentAttachmentsHint: '\u53ef\u4e0a\u4f20\u56fe\u7247\u3001\u6587\u6863\u3001\u538b\u7f29\u5305\u3001\u65e5\u5fd7\u7b49\u4efb\u610f\u6587\u4ef6',
      commentAttachmentsTip: '\u652f\u6301\u591a\u6587\u4ef6\u4e0a\u4f20\uff0c\u5355\u6587\u4ef6\u6700\u5927 20MB\u3002',
      sendCommentEmail: '\u53d1\u9001\u90ae\u4ef6\u901a\u77e5\u5bf9\u65b9',
      sendCommentEmailTip: '\u52fe\u9009\u540e\u4f1a\u5411\u5de5\u5355\u76f8\u5173\u4eba\u90ae\u7bb1\u53d1\u9001\u5907\u6ce8\u901a\u77e5\u3002',
      sendCommentEmailClaimRequired: '\u5de5\u5355\u9700\u5148\u63a5\u5355\u6216\u5206\u914d\u540e\uff0c\u624d\u80fd\u53d1\u9001\u90ae\u4ef6\u901a\u77e5\u3002',
      addComment: '添加备注',
      commentSuccess: '备注已添加',
      assignSuccess: '问题分派成功',
      claimSuccess: '接单成功',
      startSuccess: '已开始处理',
      resolveSuccess: '问题已标记为解决',
      closeSuccess: '问题已关闭',
      close: '确认关闭',
      resolveDialogComment: '处理结论',
      resolveDialogPlaceholder: '例如：已修复配置、已开通权限、请用户刷新后重试',
      creator: '提交人',
      assignee: '处理人',
      updatedAt: '最近更新',
      resolvedAt: '解决时间',
      closedAt: '关闭时间',
      uploadedMeta: '{user} · {time}',
      authorMeta: '{role} · {time}',
      assignTitle: '分派问题',
      resolveTitle: '标记问题已解决'
    },
    stats: {
      title: '统计分析',
      subtitle: '关注总量、解决率、分类趋势和开发处理效率。',
      total: '问题总量',
      resolved: '已解决',
      unresolved: '未解决',
      resolvedRate: '解决率',
      averageHours: '平均处理时长',
      hoursUnit: '{value} 小时',
      categoryChart: '问题类别分布',
      developerChart: '开发人员处理效率',
      trendChart: '近 14 天趋势',
      developerDetails: '开发处理明细',
      developerDetailsSub: '按累计工时排序',
      newTickets: '新增反馈',
      resolvedTickets: '已解决问题',
      developerName: '开发人员',
      handledHours: '累计工时',
      handledHoursColumn: '累计工时（小时）',
      resolvedCount: '已解决问题数',
      processingCount: '处理中问题数',
      averageHoursColumn: '平均处理时长（小时）'
    },
    users: {
      title: '用户管理',
      subtitle: '维护员工、开发人员、管理员账号和角色权限。',
      addUser: '新增用户',
      username: '用户名',
      name: '姓名',
      email: '邮箱',
      role: '角色',
      status: '状态',
      createTitle: '新增用户',
      editTitle: '编辑用户',
      initialPassword: '初始密码',
      resetPassword: '重置密码（可选）',
      accountStatus: '账号状态',
      createSuccess: '用户创建成功',
      updateSuccess: '用户更新成功'
    },
    notification: {
      ticketCreatedTitle: '新问题待处理',
      ticketCreatedContent: '问题 {ticketNo} 已提交，请及时处理。',
      assignedToYouTitle: '问题已分派给你',
      assignedToYouContent: '{ticketNo} 已分派到你名下，请开始处理。',
      assignmentConfirmedTitle: '问题已完成分派',
      assignmentConfirmedContent: '{ticketNo} 已分派给：{actorName}，并进入处理中。',
      claimedTitle: '问题已被接收',
      claimedContent: '{ticketNo} 已由 {actorName} 接单处理。',
      processingTitle: '问题处理中',
      processingContent: '{ticketNo} 已进入处理中状态。',
      commentAddedTitle: '问题有新备注',
      commentAddedContent: '{ticketNo} 新增了一条处理备注。',
      resolvedTitle: '问题已解决',
      resolvedContent: '{ticketNo} 已处理完成，请查看处理结果。',
      closedTitle: '问题已关闭',
      closedContent: '{ticketNo} 已由提交人确认关闭。'
    }
  },
  en: {
    common: {
      appName: 'System Issue Feedback Platform',
      requestFailed: 'Request failed',
      cancel: 'Cancel',
      confirm: 'Confirm',
      save: 'Save',
      search: 'Search',
      reset: 'Reset',
      refresh: 'Refresh',
      detail: 'Details',
      edit: 'Edit',
      delete: 'Delete',
      actions: 'Actions',
      backToList: 'Back to list',
      submit: 'Submit',
      export: 'Export',
      unassigned: 'Unassigned',
      noData: 'No data',
      enabled: 'Enabled',
      disabled: 'Disabled'
    },
    language: {
      zh: '中文',
      en: 'EN'
    },
    nav: {
      tickets: 'Issues',
      create: 'Report Issue',
      stats: 'Analytics',
      users: 'Users'
    },
    layout: {
      roleTitle: 'Current role',
      welcome: 'Welcome back, {name}',
      notifications: 'Notifications',
      logout: 'Log out',
      drawerTitle: 'Inbox',
      unreadCount: '{count} unread',
      markAllRead: 'Mark all read',
      markAllReadSuccess: 'All notifications marked as read',
      relatedTicket: 'Related issue: {ticketNo}',
      noNotifications: 'No notifications yet'
    },
    login: {
      badge: 'System Issue Feedback Platform',
      heroTitle: 'Help users quickly report issues they encounter in internal enterprise systems and track the outcome.',
      title: 'Sign in',
      desc: 'Built-in demo accounts let you switch roles instantly.',
      username: 'Username',
      usernamePlaceholder: 'Enter your username',
      password: 'Password',
      passwordPlaceholder: 'Enter your password',
      submit: 'Sign in',
      success: 'Signed in successfully',
      demoTitle: 'Demo accounts',
      demoAdmin: 'Administrator',
      demoEmployee: 'Employee',
      demoDeveloper: 'Developer'
    },
    ticketList: {
      title: 'Issue Feedback Center',
      status: 'Status',
      category: 'Category',
      priority: 'Priority',
      assignee: 'Assignee',
      keyword: 'Keyword',
      quickFilter: 'Quick filter',
      mineOnly: 'Only issues assigned to me',
      allStatus: 'All statuses',
      allCategory: 'All categories',
      allPriority: 'All priorities',
      allAssignee: 'All assignees',
      keywordPlaceholder: 'Title / Description / Feedback ID',
      total: '{count} records',
      ticketNo: 'Feedback ID',
      titleColumn: 'Title',
      creator: 'Creator',
      updatedAt: 'Updated at',
      assign: 'Assign',
      claim: 'Claim',
      start: 'Start',
      resolve: 'Resolve',
      assignTitle: 'Assign issue',
      assignPlaceholder: 'Select a developer',
      assignSuccess: 'Issue assigned',
      resolveTitle: 'Mark issue as resolved',
      resolveComment: 'Resolution note',
      resolvePlaceholder: 'Add a summary or verification note',
      resolveSuccess: 'Issue marked as resolved',
      claimSuccess: 'Issue accepted',
      startSuccess: 'Processing started',
      submitTicket: 'Submit feedback',
      exportSuccessName: 'issues.csv',
      editDraft: 'Continue editing',
      deleteDraft: 'Delete draft',
      deleteDraftSuccess: 'Draft deleted',
      deleteDraftConfirm: 'This draft will be permanently deleted. Continue?'
    },
    ticketCreate: {
      title: 'Submit Feedback',
      editTitle: 'Edit Draft',
      ticketTitle: 'Issue title',
      ticketTitlePlaceholder: 'For example: ERP blank screen, printer issue, access request',
      category: 'Issue category',
      categoryPlaceholder: 'Select a category',
      priority: 'Priority',
      priorityPlaceholder: 'Select a priority',
      description: 'Description',
      descriptionPlaceholder: 'Include when it happened, impact scope, reproduction steps, and expected result',
      attachments: 'Attachments',
      attachmentsHint: 'Drop screenshots, logs, or docs here, or click to upload',
      attachmentsTip: 'Multiple files supported. Max 20MB per file.',
      existingAttachments: 'Existing attachments',
      saveDraft: 'Save draft',
      saveDraftSuccess: 'Draft saved',
      submit: 'Submit feedback',
      submitDraft: 'Submit draft',
      success: 'Feedback submitted'
    },
    ticketDetail: {
      createdAtBy: 'Created at {createdAt}, currently handled by {assignee}.',
      ticketInfo: 'Issue details',
      description: 'Description',
      attachments: 'Attachments',
      comments: 'Processing notes',
      noAttachments: 'No attachments',
      noComments: 'No notes yet',
      commentPlaceholder: 'Add progress updates, workaround details, or user feedback',
      commentAttachmentsHint: 'Upload images, docs, zip packages, logs, or any other files',
      commentAttachmentsTip: 'Multiple files supported. Max 20MB per file.',
      sendCommentEmail: 'Send email notification',
      sendCommentEmailTip: 'When checked, email notices will be sent to the other related users on this ticket.',
      sendCommentEmailClaimRequired: 'This issue must be claimed or assigned before email notifications can be sent.',
      addComment: 'Add note',
      commentSuccess: 'Note added',
      assignSuccess: 'Issue assigned',
      claimSuccess: 'Issue accepted',
      startSuccess: 'Processing started',
      resolveSuccess: 'Issue marked as resolved',
      closeSuccess: 'Issue closed',
      close: 'Close issue',
      resolveDialogComment: 'Resolution summary',
      resolveDialogPlaceholder: 'For example: configuration fixed, permission granted, ask the user to refresh and retry',
      creator: 'Creator',
      assignee: 'Assignee',
      updatedAt: 'Last updated',
      resolvedAt: 'Resolved at',
      closedAt: 'Closed at',
      uploadedMeta: '{user} · {time}',
      authorMeta: '{role} · {time}',
      assignTitle: 'Assign issue',
      resolveTitle: 'Mark issue as resolved'
    },
    stats: {
      title: 'Analytics',
      total: 'Total issues',
      resolved: 'Resolved',
      unresolved: 'Open issues',
      resolvedRate: 'Resolution rate',
      averageHours: 'Avg. resolution time',
      hoursUnit: '{value} h',
      categoryChart: 'Issue category distribution',
      developerChart: 'Developer efficiency',
      trendChart: '14-day trend',
      developerDetails: 'Developer breakdown',
      developerDetailsSub: 'Sorted by handled hours',
      newTickets: 'New feedback',
      resolvedTickets: 'Resolved issues',
      developerName: 'Developer',
      handledHours: 'Handled hours',
      handledHoursColumn: 'Handled hours',
      resolvedCount: 'Resolved issues',
      processingCount: 'Issues in progress',
      averageHoursColumn: 'Avg. hours'
    },
    users: {
      title: 'User Management',
      addUser: 'Add user',
      username: 'Username',
      name: 'Name',
      email: 'Email',
      role: 'Role',
      status: 'Status',
      createTitle: 'Create user',
      editTitle: 'Edit user',
      initialPassword: 'Initial password',
      resetPassword: 'Reset password (optional)',
      accountStatus: 'Account status',
      createSuccess: 'User created',
      updateSuccess: 'User updated'
    },
    notification: {
      ticketCreatedTitle: 'New issue pending',
      ticketCreatedContent: 'Issue {ticketNo} has been reported. Please review it in time.',
      assignedToYouTitle: 'Issue assigned to you',
      assignedToYouContent: '{ticketNo} has been assigned to you. Processing can start now.',
      assignmentConfirmedTitle: 'Issue assignment completed',
      assignmentConfirmedContent: '{ticketNo} has been assigned to {actorName} and is now in progress.',
      claimedTitle: 'Issue accepted',
      claimedContent: '{ticketNo} has been claimed by {actorName} and is now in progress.',
      processingTitle: 'Issue in progress',
      processingContent: '{ticketNo} is now being processed.',
      commentAddedTitle: 'New note on issue',
      commentAddedContent: '{ticketNo} has a new processing note.',
      resolvedTitle: 'Issue resolved',
      resolvedContent: '{ticketNo} has been resolved. Please review the result.',
      closedTitle: 'Issue closed',
      closedContent: '{ticketNo} has been confirmed closed by the submitter.'
    }
  }
}

const roleLabels = {
  EMPLOYEE: { zh: '普通员工', en: 'Employee' },
  DEVELOPER: { zh: '开发人员', en: 'Developer' },
  ADMIN: { zh: '管理员', en: 'Administrator' }
}

const priorityLabels = {
  LOW: { zh: '低', en: 'Low' },
  MEDIUM: { zh: '中', en: 'Medium' },
  HIGH: { zh: '高', en: 'High' },
  URGENT: { zh: '紧急', en: 'Urgent' }
}

const statusLabels = {
  DRAFT: { zh: '草稿', en: 'Draft' },
  NEW: { zh: '待处理', en: 'Pending' },
  PENDING: { zh: '待处理', en: 'Pending' },
  PROCESSING: { zh: '处理中', en: 'Processing' },
  RESOLVED: { zh: '已解决', en: 'Resolved' },
  CLOSED: { zh: '已解决', en: 'Resolved' }
}

const categoryLabelEntries = [
  ['软件问题', { zh: '软件问题', en: 'Software Issue' }],
  ['权限申请', { zh: '权限申请', en: 'Access Request' }],
  ['设备故障', { zh: '设备故障', en: 'Hardware Issue' }],
  ['网络问题', { zh: '网络问题', en: 'Network Issue' }],
  ['数据支持', { zh: '数据支持', en: 'Data Support' }],
  ['其他', { zh: '其他', en: 'Other' }],
  ['杞欢闂', { zh: '软件问题', en: 'Software Issue' }],
  ['鏉冮檺鐢宠', { zh: '权限申请', en: 'Access Request' }],
  ['璁惧鏁呴殰', { zh: '设备故障', en: 'Hardware Issue' }],
  ['缃戠粶闂', { zh: '网络问题', en: 'Network Issue' }],
  ['鏁版嵁鏀寔', { zh: '数据支持', en: 'Data Support' }],
  ['鍏朵粬', { zh: '其他', en: 'Other' }]
]

const categoryLabels = Object.fromEntries(categoryLabelEntries)

const notificationMessageKeys = {
  TICKET_CREATED: {
    title: 'notification.ticketCreatedTitle',
    content: 'notification.ticketCreatedContent'
  },
  ASSIGNED_TO_YOU: {
    title: 'notification.assignedToYouTitle',
    content: 'notification.assignedToYouContent'
  },
  ASSIGNMENT_CONFIRMED: {
    title: 'notification.assignmentConfirmedTitle',
    content: 'notification.assignmentConfirmedContent'
  },
  CLAIMED: {
    title: 'notification.claimedTitle',
    content: 'notification.claimedContent'
  },
  PROCESSING: {
    title: 'notification.processingTitle',
    content: 'notification.processingContent'
  },
  COMMENT_ADDED: {
    title: 'notification.commentAddedTitle',
    content: 'notification.commentAddedContent'
  },
  RESOLVED: {
    title: 'notification.resolvedTitle',
    content: 'notification.resolvedContent'
  },
  CLOSED: {
    title: 'notification.closedTitle',
    content: 'notification.closedContent'
  }
}

function readMessage(locale, key) {
  return key.split('.').reduce((result, segment) => result?.[segment], messages[locale])
}

function fillParams(text, params) {
  if (typeof text !== 'string') {
    return text
  }
  return text.replace(/\{(\w+)\}/g, (_, key) => params?.[key] ?? '')
}

function enumLabel(map, value) {
  if (!value) {
    return '-'
  }
  return map[value]?.[localeState.value] || value
}

function notificationParams(notification) {
  return {
    ticketNo: notification?.ticketNo || '-',
    actorName: notification?.actorName || translate('common.unassigned')
  }
}

export function setLocale(locale) {
  if (!SUPPORTED_LOCALES.includes(locale)) {
    return
  }
  localeState.value = locale
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(LOCALE_KEY, locale)
  }
}

export function getLocale() {
  return localeState.value
}

export function getDateLocale() {
  return localeState.value === 'en' ? 'en-US' : 'zh-CN'
}

export function translate(key, params) {
  const fallback = readMessage('zh', key) || key
  const current = readMessage(localeState.value, key) || fallback
  return fillParams(current, params)
}

export function localizeRole(value) {
  return enumLabel(roleLabels, value)
}

export function localizePriority(value) {
  return enumLabel(priorityLabels, value)
}

export function localizeStatus(value) {
  return enumLabel(statusLabels, value)
}

export function localizeCategory(value) {
  if (!value) {
    return '-'
  }
  return categoryLabels[value]?.[localeState.value] || value
}

export function localizeNotificationTitle(notification) {
  const messageKey = notificationMessageKeys[notification?.typeKey]?.title
  if (!messageKey) {
    return notification?.title || '-'
  }
  return translate(messageKey, notificationParams(notification))
}

export function localizeNotificationContent(notification) {
  const messageKey = notificationMessageKeys[notification?.typeKey]?.content
  if (!messageKey) {
    return notification?.content || ''
  }
  return translate(messageKey, notificationParams(notification))
}

export function useI18n() {
  return {
    locale: computed(() => localeState.value),
    setLocale,
    t: (key, params) => translate(key, params)
  }
}

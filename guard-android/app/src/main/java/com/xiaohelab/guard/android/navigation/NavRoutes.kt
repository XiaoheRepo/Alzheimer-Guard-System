package com.xiaohelab.guard.android.navigation

object NavRoutes {
    // Auth
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHANGE_PASSWORD = "change_password"

    // Home
    const val HOME = "home"

    // Task
    const val TASK_LIST = "tasks"
    const val TASK_DETAIL = "tasks/{taskId}"
    const val TASK_DETAIL_ROUTE = "tasks"
    const val CREATE_TASK = "tasks/create"
    const val CLOSE_TASK = "tasks/{taskId}/close"
    const val CLOSE_TASK_ROUTE = "tasks/{taskId}/close"
    const val TASK_TRACK = "tasks/{taskId}/track"
    const val TASK_TRACK_ROUTE = "tasks/{taskId}/track"

    // Clue
    const val SCAN_QR = "clues/scan"
    const val MANUAL_ENTRY = "clues/manual"
    const val REPORT_CLUE = "clues/{taskId}/report"
    const val REPORT_CLUE_ROUTE = "clues/{taskId}/report"
    const val CLUE_DETAIL = "clues/{clueId}"
    const val CLUE_DETAIL_ROUTE = "clues/{clueId}"

    // Patient
    const val PATIENT_DETAIL = "patients/{patientId}"
    const val PATIENT_DETAIL_ROUTE = "patients/{patientId}"
    const val PATIENT_EDIT = "patients/{patientId}/edit"
    const val PATIENT_EDIT_ROUTE = "patients/{patientId}/edit"
    const val PATIENT_NEW = "patients/new"
    const val FENCE_SETTING = "patients/{patientId}/fence"
    const val FENCE_SETTING_ROUTE = "patients/{patientId}/fence"
    const val GUARDIAN = "patients/{patientId}/guardians"
    const val GUARDIAN_ROUTE = "patients/{patientId}/guardians"
    const val TAG = "patients/{patientId}/tag"
    const val TAG_ROUTE = "patients/{patientId}/tag"

    // Notification
    const val NOTIFICATION = "notifications"

    // AI
    const val AI_SESSION_LIST = "ai/sessions"
    const val AI_CHAT = "ai/sessions/{sessionId}"
    const val AI_CHAT_ROUTE = "ai/sessions/{sessionId}"
    const val AI_QUOTA = "ai/quota"
    const val AI_MEMORY = "ai/memory"

    // Order
    const val ORDER_LIST = "orders"
    const val ORDER_DETAIL = "orders/{orderId}"
    const val ORDER_DETAIL_ROUTE = "orders/{orderId}"
    const val CREATE_ORDER = "orders/create"

    // Helper builders
    fun taskDetail(taskId: String) = "tasks/$taskId"
    fun closeTask(taskId: String) = "tasks/$taskId/close"
    fun taskTrack(taskId: String) = "tasks/$taskId/track"
    fun reportClue(taskId: String) = "clues/$taskId/report"
    fun clueDetail(clueId: String) = "clues/$clueId"
    fun patientDetail(patientId: String) = "patients/$patientId"
    fun patientEdit(patientId: String) = "patients/$patientId/edit"
    fun fenceSetting(patientId: String) = "patients/$patientId/fence"
    fun guardian(patientId: String) = "patients/$patientId/guardians"
    fun tag(patientId: String) = "patients/$patientId/tag"
    fun aiChat(sessionId: String) = "ai/sessions/$sessionId"
    fun orderDetail(orderId: String) = "orders/$orderId"
}

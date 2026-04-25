package com.xiaohelab.guard.android.core.navigation

/**
 * 页面 ID 统一 `MH-*` 前缀（handbook §1.4）。导航参数走 NavType.StringType（HC-ID-String）。
 *
 * Routes map to handbook §16 / §17 page IDs verbatim.
 */
object MhRoutes {
    // Auth flow
    const val AUTH_LOGIN = "mh_auth_login"         // MH-AUTH-01
    const val AUTH_REGISTER = "mh_auth_register"   // MH-AUTH-02
    const val AUTH_RESET_REQUEST = "mh_auth_reset_request" // MH-AUTH-03
    const val AUTH_RESET_CONFIRM = "mh_auth_reset_confirm" // MH-AUTH-04

    // Main shell
    const val HOME = "mh_home"                      // MH-HOME-01
    const val ME = "mh_me"                          // MH-ME-01
    const val SETTINGS = "mh_me_settings"           // MH-ME-02
    const val ME_CHANGE_PASSWORD = "mh_me_change_password" // MH-ME-03

    // Profile (§17) – argument names kept consistent with API field names
    const val PATIENT_LIST = "mh_patient_list"      // MH-PAT-00
    const val PATIENT_DETAIL = "mh_patient_detail/{patient_id}" // MH-PAT-01
    const val PATIENT_CREATE = "mh_patient_create"  // MH-PAT-02
    const val PATIENT_EDIT = "mh_patient_edit/{patient_id}"     // MH-PAT-02
    const val GUARDIAN_MANAGE = "mh_guardian_manage/{patient_id}" // MH-GUA-01
    const val GUARDIAN_INVITE = "mh_guardian_invite/{patient_id}" // MH-GUA-02
    const val GUARDIAN_TRANSFER = "mh_guardian_transfer/{patient_id}" // MH-GUA-04

    // Tags (M3-A, MH-TAG-*)
    const val TAG_LIST = "mh_tag_list/{patient_id}"                        // MH-TAG-00
    const val TAG_BIND = "mh_tag_bind/{patient_id}?tag_code={tag_code}"    // MH-TAG-01

    // Material orders (M3-B, MH-MAT-*)
    const val MAT_ORDER_LIST = "mh_mat_order_list/{patient_id}"    // MH-MAT-00
    const val MAT_ORDER_CREATE = "mh_mat_order_create/{patient_id}" // MH-MAT-01

    // Fence edit (M4, MH-FENCE-*)
    const val FENCE_EDIT = "mh_fence_edit/{patient_id}"            // MH-FENCE-01

    // Tasks (M5-A, MH-TASK-*)
    const val TASK_LIST = "mh_task_list"                            // MH-TASK-00
    const val TASK_CREATE = "mh_task_create"                        // MH-TASK-01
    const val TASK_DETAIL = "mh_task_detail/{task_id}"              // MH-TASK-02

    // Clues (M5-B, MH-CLUE-*)
    const val CLUE_LIST = "mh_clue_list/{task_id}"                  // MH-CLUE-00
    const val CLUE_CREATE = "mh_clue_create/{task_id}"              // MH-CLUE-01

    // Notifications (M6, MH-NOTIF-*)
    const val NOTIFICATION_LIST = "mh_notification_list"            // MH-NOTIF-00

    // AI (M7, MH-AI-*)
    const val AI_CHAT = "mh_ai_chat?session_id={session_id}"        // MH-AI-00

    // QR Scan (M3-C, MH-SCAN)
    const val QR_SCAN = "mh_qr_scan?target={target}"                // MH-SCAN
    const val SCAN_RESULT = "mh_scan_result?tag_code={tag_code}"    // MH-SCAN-RESULT

    // ==== Arg keys (HC-ID-String: all NavType.StringType) ====
    const val ARG_PATIENT_ID = "patient_id"
    const val ARG_TAG_CODE = "tag_code"
    const val ARG_TASK_ID = "task_id"
    const val ARG_CLUE_ID = "clue_id"
    const val ARG_ORDER_ID = "order_id"
    const val ARG_SESSION_ID = "session_id"
    const val ARG_TARGET = "target"

    // ==== Helper functions ====
    fun patientDetail(patientId: String) = "mh_patient_detail/$patientId"
    fun patientEdit(patientId: String) = "mh_patient_edit/$patientId"
    fun guardianManage(patientId: String) = "mh_guardian_manage/$patientId"
    fun guardianInvite(patientId: String) = "mh_guardian_invite/$patientId"
    fun guardianTransfer(patientId: String) = "mh_guardian_transfer/$patientId"

    fun tagList(patientId: String) = "mh_tag_list/$patientId"
    fun tagBind(patientId: String, tagCode: String? = null) =
        if (tagCode != null) "mh_tag_bind/$patientId?tag_code=$tagCode"
        else "mh_tag_bind/$patientId"

    fun matOrderList(patientId: String) = "mh_mat_order_list/$patientId"
    fun matOrderCreate(patientId: String) = "mh_mat_order_create/$patientId"
    fun fenceEdit(patientId: String) = "mh_fence_edit/$patientId"

    fun taskDetail(taskId: String) = "mh_task_detail/$taskId"
    fun clueList(taskId: String) = "mh_clue_list/$taskId"
    fun clueCreate(taskId: String) = "mh_clue_create/$taskId"

    fun aiChat(sessionId: String? = null) =
        if (sessionId != null) "mh_ai_chat?session_id=$sessionId" else "mh_ai_chat"

    fun qrScan(target: String) = "mh_qr_scan?target=$target"
    fun scanResult(tagCode: String) = "mh_scan_result?tag_code=$tagCode"

    /** Me 页扫码入口的 target 前缀（handbook §9 扫码 - 我的页入口）。 */
    const val SCAN_TARGET_ME_ENTRY = "me_entry:scan"
}

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

    // Profile (§17) – argument names kept consistent with API field names
    const val PATIENT_LIST = "mh_patient_list"      // MH-PAT-00
    const val PATIENT_DETAIL = "mh_patient_detail/{patient_id}" // MH-PAT-01
    const val PATIENT_CREATE = "mh_patient_create"  // MH-PAT-02
    const val PATIENT_EDIT = "mh_patient_edit/{patient_id}"     // MH-PAT-02
    const val GUARDIAN_MANAGE = "mh_guardian_manage/{patient_id}" // MH-GUA-01
    const val GUARDIAN_INVITE = "mh_guardian_invite/{patient_id}" // MH-GUA-02
    const val GUARDIAN_TRANSFER = "mh_guardian_transfer/{patient_id}" // MH-GUA-04

    fun patientDetail(patientId: String) = "mh_patient_detail/$patientId"
    fun patientEdit(patientId: String) = "mh_patient_edit/$patientId"
    fun guardianManage(patientId: String) = "mh_guardian_manage/$patientId"
    fun guardianInvite(patientId: String) = "mh_guardian_invite/$patientId"
    fun guardianTransfer(patientId: String) = "mh_guardian_transfer/$patientId"

    const val ARG_PATIENT_ID = "patient_id"
}

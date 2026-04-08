<template>
  <div class="page-container">
    <!-- Tab切换 -->
    <a-tabs v-model:activeKey="activeTab" @change="() => {}">
      <!-- 工单列表 -->
      <a-tab-pane key="orders" tab="申领工单">
        <a-card :bordered="false">
          <a-space wrap style="margin-bottom: 12px">
            <a-select
              v-model:value="orderParams.status"
              placeholder="工单状态"
              allow-clear
              style="width: 160px"
              @change="
                () => {
                  orderParams.page_no = 1
                  loadOrders()
                }
              "
            >
              <a-select-option value="PENDING">待审核</a-select-option>
              <a-select-option value="PROCESSING">处理中</a-select-option>
              <a-select-option value="CANCEL_PENDING">待取消审核</a-select-option>
              <a-select-option value="SHIPPED">已发货</a-select-option>
              <a-select-option value="EXCEPTION">物流异常</a-select-option>
              <a-select-option value="COMPLETED">已完成</a-select-option>
              <a-select-option value="CANCELLED">已取消</a-select-option>
            </a-select>
            <a-button
              type="primary"
              @click="
                () => {
                  orderParams.page_no = 1
                  loadOrders()
                }
              "
              >查询</a-button
            >
            <a-button @click="resetOrderFilters">重置</a-button>
          </a-space>
          <a-table
            :columns="orderColumns"
            :data-source="orders"
            :loading="ordersLoading"
            :pagination="{
              current: orderParams.page_no,
              pageSize: orderParams.page_size,
              total: ordersTotal,
              showSizeChanger: true,
              showTotal: (t: number) => `共 ${t} 条`,
            }"
            row-key="order_id"
            @change="handleOrderTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="orderStatusColors[record.status as OrderStatus]">
                  {{ orderStatusLabels[record.status as OrderStatus] ?? record.status }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="viewOrder(record.order_id)"
                  >详情</a-button
                >
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <!-- 标签台账 -->
      <a-tab-pane key="tags" tab="标签台账">
        <a-card :bordered="false">
          <a-space wrap style="margin-bottom: 12px">
            <a-select
              v-model:value="tagParams.status"
              placeholder="标签状态"
              allow-clear
              style="width: 140px"
              @change="
                () => {
                  tagParams.page_no = 1
                  loadTags()
                }
              "
            >
              <a-select-option value="UNBOUND">未绑定</a-select-option>
              <a-select-option value="ALLOCATED">已分配</a-select-option>
              <a-select-option value="BOUND">已绑定</a-select-option>
              <a-select-option value="LOST">已丢失</a-select-option>
              <a-select-option value="VOID">已作废</a-select-option>
            </a-select>
            <a-button
              type="primary"
              @click="
                () => {
                  tagParams.page_no = 1
                  loadTags()
                }
              "
              >查询</a-button
            >
          </a-space>
          <a-table
            :columns="tagColumns"
            :data-source="tags"
            :loading="tagsLoading"
            :pagination="{
              current: tagParams.page_no,
              pageSize: tagParams.page_size,
              total: tagsTotal,
              showSizeChanger: true,
              showTotal: (t: number) => `共 ${t} 条`,
            }"
            row-key="tag_code"
            @change="handleTagTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="tagStatusColors[record.status as TagStatus]">
                  {{ tagStatusLabels[record.status as TagStatus] ?? record.status }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button
                    v-if="record.status === 'UNBOUND'"
                    type="link"
                    size="small"
                    @click="openAllocateTag(record.tag_code)"
                    >分配</a-button
                  >
                  <a-button
                    v-if="record.status === 'ALLOCATED'"
                    type="link"
                    size="small"
                    @click="openReleaseTag(record.tag_code)"
                    >释放</a-button
                  >
                  <a-button
                    type="link"
                    size="small"
                    danger
                    :disabled="!['ALLOCATED', 'BOUND'].includes(record.status)"
                    @click="openVoidTag(record.tag_code)"
                    >作废</a-button
                  >
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <!-- 作废标签弹窗 -->
    <a-modal
      v-model:open="voidModalVisible"
      title="作废标签"
      :ok-button-props="{ loading: voidSubmitting, disabled: voidReason.length < 5, danger: true }"
      ok-text="确认作废"
      @ok="submitVoidTag"
    >
      <p>标签：{{ voidTagCode }}</p>
      <a-form layout="vertical">
        <a-form-item label="作废原因（必填，5-256 字）" required>
          <a-textarea
            v-model:value="voidReason"
            :rows="3"
            :maxlength="256"
            show-count
            placeholder="请填写作废原因"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配标签弹窗 -->
    <a-modal
      v-model:open="allocateModalVisible"
      title="分配标签"
      :ok-button-props="{ loading: allocateSubmitting, disabled: !allocateOrderId }"
      ok-text="确认分配"
      @ok="submitAllocateTag"
    >
      <p>标签：{{ allocateTagCode }}</p>
      <a-form layout="vertical">
        <a-form-item label="目标工单 ID" required>
          <a-input v-model:value="allocateOrderId" placeholder="请输入工单 ID" />
        </a-form-item>
        <a-form-item label="备注（可选）">
          <a-textarea
            v-model:value="allocateReason"
            :rows="2"
            :maxlength="256"
            show-count
            placeholder="可选备注"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 释放标签弹窗 -->
    <a-modal
      v-model:open="releaseModalVisible"
      title="释放标签"
      :ok-button-props="{ loading: releaseSubmitting, disabled: releaseReason.length < 5 }"
      ok-text="确认释放"
      @ok="submitReleaseTag"
    >
      <p>标签：{{ releaseTagCode }}</p>
      <a-form layout="vertical">
        <a-form-item label="释放原因（必填，5-256 字）" required>
          <a-textarea
            v-model:value="releaseReason"
            :rows="3"
            :maxlength="256"
            show-count
            placeholder="请填写释放原因"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getAdminOrderList,
  getTagList,
  voidTag,
  allocateTag,
  releaseTag,
  type OrderStatus,
  type TagStatus,
  type MaterialOrderItem,
  type TagItem,
  type OrderListParams,
  type TagListParams,
} from '@/api/material'

const router = useRouter()
const activeTab = ref('orders')

// ── 工单部分 ──────────────────────────────────────────────────────
const orderParams = reactive<OrderListParams & { page_no: number; page_size: number }>({
  page_no: 1,
  page_size: 20,
  status: 'PENDING,PROCESSING',
})
const orders = ref<MaterialOrderItem[]>([])
const ordersTotal = ref(0)
const ordersLoading = ref(false)

const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING: '待审核',
  PROCESSING: '处理中',
  CANCEL_PENDING: '待取消审核',
  SHIPPED: '已发货',
  EXCEPTION: '物流异常',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
const orderStatusColors: Record<OrderStatus, string> = {
  PENDING: 'warning',
  PROCESSING: 'processing',
  CANCEL_PENDING: 'orange',
  SHIPPED: 'blue',
  EXCEPTION: 'error',
  COMPLETED: 'success',
  CANCELLED: 'default',
}

const orderColumns = [
  { title: '工单 ID', dataIndex: 'order_id', key: 'order_id', width: 120 },
  { title: '患者 ID', dataIndex: 'patient_id', key: 'patient_id', width: 100 },
  { title: '申请人', dataIndex: 'applicant_user_id', key: 'applicant_user_id', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '申请备注', dataIndex: 'apply_note', key: 'apply_note', ellipsis: true },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 180 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

async function loadOrders() {
  ordersLoading.value = true
  try {
    const res = await getAdminOrderList(orderParams)
    orders.value = res.items
    ordersTotal.value = res.total
  } catch {
    message.error('加载工单列表失败')
  } finally {
    ordersLoading.value = false
  }
}

function handleOrderTableChange(pagination: { current: number; pageSize: number }) {
  orderParams.page_no = pagination.current
  orderParams.page_size = pagination.pageSize
  loadOrders()
}

function resetOrderFilters() {
  orderParams.status = 'PENDING,PROCESSING'
  orderParams.page_no = 1
  loadOrders()
}

function viewOrder(orderId: string) {
  router.push(`/admin/material/orders/${orderId}`)
}

// ── 标签部分 ──────────────────────────────────────────────────────
const tagParams = reactive<TagListParams & { page_no: number; page_size: number }>({
  page_no: 1,
  page_size: 20,
})
const tags = ref<TagItem[]>([])
const tagsTotal = ref(0)
const tagsLoading = ref(false)

const tagStatusLabels: Record<TagStatus, string> = {
  UNBOUND: '未绑定',
  ALLOCATED: '已分配',
  BOUND: '已绑定',
  LOST: '已丢失',
  VOID: '已作废',
}
const tagStatusColors: Record<TagStatus, string> = {
  UNBOUND: 'default',
  ALLOCATED: 'blue',
  BOUND: 'success',
  LOST: 'warning',
  VOID: 'error',
}

const tagColumns = [
  { title: '标签编码', dataIndex: 'tag_code', key: 'tag_code', width: 140 },
  { title: '类型', dataIndex: 'tag_type', key: 'tag_type', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '患者 ID', dataIndex: 'patient_id', key: 'patient_id', width: 100 },
  { title: '更新时间', dataIndex: 'updated_at', key: 'updated_at', width: 180 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

async function loadTags() {
  tagsLoading.value = true
  try {
    const res = await getTagList(tagParams)
    tags.value = res.items
    tagsTotal.value = res.total
  } catch {
    message.error('加载标签列表失败')
  } finally {
    tagsLoading.value = false
  }
}

function handleTagTableChange(pagination: { current: number; pageSize: number }) {
  tagParams.page_no = pagination.current
  tagParams.page_size = pagination.pageSize
  loadTags()
}

// ── 作废标签 ──────────────────────────────────────────────────────
const voidModalVisible = ref(false)
const voidTagCode = ref('')
const voidReason = ref('')
const voidSubmitting = ref(false)

function openVoidTag(tagCode: string) {
  voidTagCode.value = tagCode
  voidReason.value = ''
  voidModalVisible.value = true
}

async function submitVoidTag() {
  voidSubmitting.value = true
  try {
    await voidTag(voidTagCode.value, { void_reason: voidReason.value })
    message.success('标签已作废')
    voidModalVisible.value = false
    await loadTags()
  } catch {
    // error handled by interceptor
  } finally {
    voidSubmitting.value = false
  }
}

// ── 分配标签 ──────────────────────────────────────────────────────
const allocateModalVisible = ref(false)
const allocateTagCode = ref('')
const allocateOrderId = ref('')
const allocateReason = ref('')
const allocateSubmitting = ref(false)

function openAllocateTag(tagCode: string) {
  allocateTagCode.value = tagCode
  allocateOrderId.value = ''
  allocateReason.value = ''
  allocateModalVisible.value = true
}

async function submitAllocateTag() {
  allocateSubmitting.value = true
  try {
    await allocateTag(allocateTagCode.value, {
      order_id: allocateOrderId.value,
      ...(allocateReason.value ? { reason: allocateReason.value } : {}),
    })
    message.success('标签已分配')
    allocateModalVisible.value = false
    await loadTags()
  } catch {
    // error handled by interceptor
  } finally {
    allocateSubmitting.value = false
  }
}

// ── 释放标签 ──────────────────────────────────────────────────────
const releaseModalVisible = ref(false)
const releaseTagCode = ref('')
const releaseReason = ref('')
const releaseSubmitting = ref(false)

function openReleaseTag(tagCode: string) {
  releaseTagCode.value = tagCode
  releaseReason.value = ''
  releaseModalVisible.value = true
}

async function submitReleaseTag() {
  releaseSubmitting.value = true
  try {
    await releaseTag(releaseTagCode.value, { reason: releaseReason.value })
    message.success('标签已释放')
    releaseModalVisible.value = false
    await loadTags()
  } catch {
    // error handled by interceptor
  } finally {
    releaseSubmitting.value = false
  }
}

onMounted(() => {
  loadOrders()
  loadTags()
})
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>

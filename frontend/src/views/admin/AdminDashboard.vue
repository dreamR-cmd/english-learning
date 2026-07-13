<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand">
        <strong>管理后台</strong>
        <span>{{ user?.nickname || user?.username }}</span>
      </div>
      <button
        v-for="item in visibleMenus"
        :key="item.code"
        class="menu-btn"
        :class="{ active: activeMenu === item.code }"
        type="button"
        @click="activeMenu = item.code"
      >
        {{ item.name }}
      </button>
      <button class="exit-btn" type="button" @click="goUserApp">进入学习端</button>
      <button class="exit-btn muted" type="button" @click="logout">退出登录</button>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div>
          <p>English Learning Admin</p>
          <h1>{{ currentMenu?.name || '后台首页' }}</h1>
        </div>
        <button class="refresh-btn" type="button" @click="loadActive">刷新</button>
      </header>

      <section v-if="message" class="message">{{ message }}</section>

      <section v-if="activeMenu === 'ADMIN_DASHBOARD'" class="summary-grid">
        <article class="summary-card">
          <span>订单</span>
          <strong>{{ orders.length }}</strong>
        </article>
        <article class="summary-card">
          <span>模块</span>
          <strong>{{ modules.length }}</strong>
        </article>
        <article class="summary-card">
          <span>用户</span>
          <strong>{{ users.length }}</strong>
        </article>
        <article class="summary-card">
          <span>角色</span>
          <strong>{{ roles.length }}</strong>
        </article>
      </section>

      <section v-if="activeMenu === 'ORDER_MANAGE'" class="panel">
        <table>
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>商品</th>
              <th>金额</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td>{{ order.orderNo }}</td>
              <td>{{ order.userId }}</td>
              <td>{{ order.productName }}</td>
              <td>{{ order.amount }}</td>
              <td>
                <select v-model="order.status">
                  <option value="pending">pending</option>
                  <option value="paid">paid</option>
                  <option value="canceled">canceled</option>
                </select>
              </td>
              <td>{{ formatDate(order.createdAt) }}</td>
              <td><button type="button" @click="saveOrder(order)">保存</button></td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-if="activeMenu === 'MODULE_MANAGE'" class="panel">
        <div class="panel-toolbar">
          <button type="button" @click="openModuleDialog()">新增模块</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>编码</th>
              <th>图标</th>
              <th>入口路径</th>
              <th>描述</th>
              <th>排序</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="module in modules" :key="module.id">
              <td>{{ module.id }}</td>
              <td>{{ module.name }}</td>
              <td>{{ module.code }}</td>
              <td>{{ module.icon }}</td>
              <td>{{ module.routePath || '-' }}</td>
              <td>{{ module.description }}</td>
              <td>{{ module.sortOrder }}</td>
              <td>
                <button type="button" @click="editModule(module)">编辑</button>
                <button type="button" class="danger" @click="removeModule(module.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-if="activeMenu === 'USER_MANAGE'" class="panel">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>每日目标</th>
              <th>角色</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in users" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.username }}</td>
              <td>{{ item.nickname }}</td>
              <td>{{ item.dailyWordTarget }}</td>
              <td>
                <select v-model="item.roleId">
                  <option v-for="role in roles" :key="role.id" :value="role.id">{{ role.name }}</option>
                </select>
              </td>
              <td>
                <button type="button" @click="saveUserRole(item)">保存</button>
                <button type="button" class="danger" @click="removeUser(item.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-if="activeMenu === 'ROLE_MANAGE'" class="panel">
        <div class="panel-toolbar">
          <button type="button" @click="openRoleDialog()">新增角色</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>编码</th>
              <th>描述</th>
              <th>权限</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="role in roles" :key="role.id">
              <td>{{ role.id }}</td>
              <td>{{ role.name }}</td>
              <td>{{ role.code }}</td>
              <td>{{ role.description }}</td>
              <td><button type="button" @click="openPermissionAssign(role)">分配权限</button></td>
              <td>
                <button type="button" @click="editRole(role)">编辑</button>
                <button type="button" class="danger" @click="removeRole(role.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-if="activeMenu === 'PERMISSION_MANAGE'" class="panel permission-grid">
        <article v-for="permission in permissions" :key="permission.id">
          <strong>{{ permission.name }}</strong>
          <span>{{ permission.code }}</span>
          <small>{{ permission.menuPath }}</small>
          <p>{{ permission.description }}</p>
        </article>
      </section>

      <section v-if="activeMenu === 'AUDIT_LOGS'" class="panel">
        <h2>管理员操作日志</h2>
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>管理员</th>
              <th>模块</th>
              <th>动作</th>
              <th>对象</th>
              <th>结果</th>
              <th>IP</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in operationLogs" :key="log.id">
              <td>{{ formatDate(log.createdAt) }}</td>
              <td>{{ log.adminUsername || '-' }}</td>
              <td>{{ log.module }}</td>
              <td>{{ log.action }}</td>
              <td>{{ log.targetId || '-' }}</td>
              <td>{{ log.success ? '成功' : '失败' }}</td>
              <td>{{ log.ipAddress || '-' }}</td>
            </tr>
          </tbody>
        </table>

        <h2>权限变更记录</h2>
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>管理员</th>
              <th>角色</th>
              <th>新增权限</th>
              <th>移除权限</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in permissionChangeLogs" :key="log.id">
              <td>{{ formatDate(log.createdAt) }}</td>
              <td>{{ log.adminUsername || '-' }}</td>
              <td>{{ log.roleCode || log.roleId }}</td>
              <td>{{ log.addedPermissionIds || '[]' }}</td>
              <td>{{ log.removedPermissionIds || '[]' }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-if="activeMenu === 'SWAGGER_DOCS'" class="swagger-panel">
        <iframe title="Swagger 接口文档" src="/swagger-ui.html"></iframe>
      </section>
    </main>

    <div v-if="moduleDialogVisible" class="dialog-mask" @click.self="closeModuleDialog">
      <section class="dialog role-dialog">
        <h2>{{ moduleForm.id ? '编辑模块' : '新增模块' }}</h2>
        <form class="dialog-form" @submit.prevent="saveModule">
          <label>
            <span>模块名称</span>
            <input v-model="moduleForm.name" placeholder="请输入模块名称" required />
          </label>
          <label>
            <span>模块编码</span>
            <input v-model="moduleForm.code" placeholder="例如 cet4" required />
          </label>
          <label>
            <span>图标/标识</span>
            <input v-model="moduleForm.icon" placeholder="请输入图标或标识" />
          </label>
          <label>
            <span>入口路径</span>
            <input v-model="moduleForm.routePath" placeholder="考试模块留空；如 /shop" />
          </label>
          <label>
            <span>排序</span>
            <input v-model.number="moduleForm.sortOrder" type="number" placeholder="请输入排序值" />
          </label>
          <label>
            <span>描述</span>
            <input v-model="moduleForm.description" placeholder="请输入模块描述" />
          </label>
          <div class="dialog-actions">
            <button type="submit">{{ moduleForm.id ? '保存修改' : '新增模块' }}</button>
            <button type="button" class="secondary-btn" @click="closeModuleDialog">取消</button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="roleDialogVisible" class="dialog-mask" @click.self="closeRoleDialog">
      <section class="dialog role-dialog">
        <h2>{{ roleForm.id ? '编辑角色' : '新增角色' }}</h2>
        <form class="dialog-form" @submit.prevent="saveRole">
          <label>
            <span>角色名称</span>
            <input v-model="roleForm.name" placeholder="请输入角色名称" required />
          </label>
          <label>
            <span>角色编码</span>
            <input v-model="roleForm.code" placeholder="请输入角色编码" required />
          </label>
          <label>
            <span>描述</span>
            <input v-model="roleForm.description" placeholder="请输入角色描述" />
          </label>
          <div class="dialog-actions">
            <button type="submit">{{ roleForm.id ? '保存修改' : '新增角色' }}</button>
            <button type="button" class="secondary-btn" @click="closeRoleDialog">取消</button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="permissionDialogRole" class="dialog-mask" @click.self="permissionDialogRole = null">
      <section class="dialog">
        <h2>分配权限：{{ permissionDialogRole.name }}</h2>
        <label v-for="permission in permissions" :key="permission.id" class="check-row">
          <input v-model="selectedPermissionIds" type="checkbox" :value="permission.id" />
          <span>{{ permission.name }}</span>
        </label>
        <div class="dialog-actions">
          <button type="button" @click="saveRolePermissions">保存权限</button>
          <button type="button" @click="permissionDialogRole = null">取消</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  assignAdminRolePermissions,
  createAdminModule,
  createAdminRole,
  deleteAdminModule,
  deleteAdminRole,
  deleteAdminUser,
  getAdminOperationLogs,
  getAdminModules,
  getAdminOrders,
  getAdminPermissions,
  getAdminPermissionChangeLogs,
  getAdminRolePermissions,
  getAdminRoles,
  getAdminUsers,
  updateAdminModule,
  updateAdminOrderStatus,
  updateAdminRole,
  updateAdminUserRole
} from '../../utils/api'
import { clearCurrentUser, currentUser } from '../../utils/currentUser'

const router = useRouter()
const user = currentUser
const activeMenu = ref('ADMIN_DASHBOARD')
const message = ref('')
const orders = ref([])
const modules = ref([])
const users = ref([])
const roles = ref([])
const permissions = ref([])
const operationLogs = ref([])
const permissionChangeLogs = ref([])
const permissionDialogRole = ref(null)
const moduleDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const selectedPermissionIds = ref([])

const moduleForm = reactive({ id: null, name: '', code: '', icon: '', routePath: '', description: '', sortOrder: 0 })
const roleForm = reactive({ id: null, name: '', code: '', description: '' })

const menus = [
  { code: 'ADMIN_DASHBOARD', name: '后台首页' },
  { code: 'ORDER_MANAGE', name: '订单管理' },
  { code: 'MODULE_MANAGE', name: '模块管理' },
  { code: 'USER_MANAGE', name: '用户管理' },
  { code: 'ROLE_MANAGE', name: '角色管理' },
  { code: 'PERMISSION_MANAGE', name: '权限管理' },
  { code: 'AUDIT_LOGS', name: '审计日志' },
  { code: 'SWAGGER_DOCS', name: '接口文档' }
]

const visibleMenus = computed(() => {
  const owned = new Set(user.value?.permissions || [])
  return menus.filter(item => owned.has(item.code))
})
const currentMenu = computed(() => menus.find(item => item.code === activeMenu.value))

watch(activeMenu, loadActive)

function showMessage(text) {
  message.value = text
  window.setTimeout(() => {
    if (message.value === text) message.value = ''
  }, 2200)
}

function formatDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : ''
}

function confirmRisk(text) {
  return window.confirm(`${text}\n\n确认后将记录管理员审计日志。`) ? 'CONFIRM' : null
}

async function loadBase() {
  const [roleRes, permissionRes] = await Promise.all([getAdminRoles(), getAdminPermissions()])
  roles.value = roleRes.data.data || []
  permissions.value = permissionRes.data.data || []
}

async function loadActive() {
  if (activeMenu.value === 'ORDER_MANAGE' || activeMenu.value === 'ADMIN_DASHBOARD') {
    orders.value = (await getAdminOrders()).data.data || []
  }
  if (activeMenu.value === 'MODULE_MANAGE' || activeMenu.value === 'ADMIN_DASHBOARD') {
    modules.value = (await getAdminModules()).data.data || []
  }
  if (activeMenu.value === 'USER_MANAGE' || activeMenu.value === 'ADMIN_DASHBOARD') {
    users.value = (await getAdminUsers()).data.data || []
  }
  if (activeMenu.value === 'ROLE_MANAGE' || activeMenu.value === 'ADMIN_DASHBOARD') {
    roles.value = (await getAdminRoles()).data.data || []
  }
  if (activeMenu.value === 'PERMISSION_MANAGE') {
    permissions.value = (await getAdminPermissions()).data.data || []
  }
  if (activeMenu.value === 'AUDIT_LOGS') {
    const [operationRes, changeRes] = await Promise.all([getAdminOperationLogs(), getAdminPermissionChangeLogs()])
    operationLogs.value = operationRes.data.data || []
    permissionChangeLogs.value = changeRes.data.data || []
  }
}

async function saveOrder(order) {
  const confirmText = confirmRisk('确认修改订单状态吗？')
  if (!confirmText) return
  await updateAdminOrderStatus(order.id, order.status, confirmText)
  showMessage('订单状态已更新')
}

function resetModuleForm() {
  Object.assign(moduleForm, { id: null, name: '', code: '', icon: '', routePath: '', description: '', sortOrder: 0 })
}

function openModuleDialog(module = null) {
  resetModuleForm()
  if (module) {
    Object.assign(moduleForm, module)
  }
  moduleDialogVisible.value = true
}

function closeModuleDialog() {
  moduleDialogVisible.value = false
  resetModuleForm()
}

function editModule(module) {
  openModuleDialog(module)
}

async function saveModule() {
  if (moduleForm.id) {
    await updateAdminModule(moduleForm.id, moduleForm)
  } else {
    await createAdminModule(moduleForm)
  }
  closeModuleDialog()
  await loadActive()
  showMessage('模块已保存')
}

async function removeModule(id) {
  const confirmText = confirmRisk('确认删除模块吗？系统内置模块会被后端拦截。')
  if (!confirmText) return
  await deleteAdminModule(id, confirmText)
  await loadActive()
  showMessage('模块已删除')
}

async function saveUserRole(item) {
  const confirmText = confirmRisk('确认修改用户角色吗？')
  if (!confirmText) return
  await updateAdminUserRole(item.id, item.roleId, confirmText)
  showMessage('用户角色已更新')
}

async function removeUser(id) {
  const confirmText = confirmRisk('确认禁用该用户吗？')
  if (!confirmText) return
  await deleteAdminUser(id, confirmText)
  await loadActive()
  showMessage('用户已删除')
}

function resetRoleForm() {
  Object.assign(roleForm, { id: null, name: '', code: '', description: '' })
}

function openRoleDialog(role = null) {
  resetRoleForm()
  if (role) {
    Object.assign(roleForm, role)
  }
  roleDialogVisible.value = true
}

function closeRoleDialog() {
  roleDialogVisible.value = false
  resetRoleForm()
}

function editRole(role) {
  openRoleDialog(role)
}

async function saveRole() {
  if (roleForm.id) {
    await updateAdminRole(roleForm.id, roleForm)
  } else {
    await createAdminRole(roleForm)
  }
  closeRoleDialog()
  await loadActive()
  showMessage('角色已保存')
}

async function removeRole(id) {
  const confirmText = confirmRisk('确认删除角色吗？内置角色和已绑定用户的角色会被后端拦截。')
  if (!confirmText) return
  await deleteAdminRole(id, confirmText)
  await loadActive()
  showMessage('角色已删除')
}

async function openPermissionAssign(role) {
  permissionDialogRole.value = role
  const response = await getAdminRolePermissions(role.id)
  selectedPermissionIds.value = (response.data.data || []).map(item => item.id)
}

async function saveRolePermissions() {
  const confirmText = confirmRisk('确认修改该角色权限吗？')
  if (!confirmText) return
  await assignAdminRolePermissions(permissionDialogRole.value.id, selectedPermissionIds.value, confirmText)
  permissionDialogRole.value = null
  showMessage('角色权限已保存')
}

function goUserApp() {
  router.push('/modules')
}

function logout() {
  clearCurrentUser()
  router.push('/login')
}

onMounted(async () => {
  if (!visibleMenus.value.some(item => item.code === activeMenu.value)) {
    activeMenu.value = visibleMenus.value[0]?.code || 'ADMIN_DASHBOARD'
  }
  await loadBase()
  await loadActive()
})
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: #f4f6f8;
}
.admin-sidebar {
  background: #15202b;
  color: #fff;
  padding: 22px 16px;
}
.brand {
  display: grid;
  gap: 6px;
  margin-bottom: 24px;
}
.brand strong {
  font-size: 22px;
}
.brand span {
  color: #aab8c2;
  font-size: 13px;
}
.menu-btn,
.exit-btn {
  width: 100%;
  border: none;
  border-radius: 8px;
  margin-bottom: 8px;
  padding: 12px 14px;
  text-align: left;
  cursor: pointer;
}
.menu-btn {
  background: transparent;
  color: #dce3ea;
}
.menu-btn.active,
.menu-btn:hover {
  background: #1d9bf0;
  color: #fff;
}
.exit-btn {
  background: #263645;
  color: #fff;
}
.exit-btn.muted {
  color: #cbd5df;
}
.admin-main {
  padding: 28px;
  overflow-x: auto;
}
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.admin-header p {
  color: #64748b;
  font-size: 13px;
}
.admin-header h1 {
  font-size: 28px;
  color: #0f172a;
}
.refresh-btn,
button {
  border: none;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  padding: 8px 12px;
  cursor: pointer;
}
button.danger {
  background: #dc2626;
  margin-left: 8px;
}
button.secondary-btn {
  background: #64748b;
}
.message {
  margin-bottom: 14px;
  border-radius: 8px;
  background: #ecfdf5;
  color: #047857;
  padding: 12px;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}
.summary-card,
.panel,
.permission-grid article,
.dialog {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}
.summary-card {
  padding: 20px;
}
.summary-card span {
  display: block;
  color: #64748b;
  margin-bottom: 10px;
}
.summary-card strong {
  font-size: 34px;
  color: #0f172a;
}
.panel {
  padding: 16px;
}
.panel-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
.inline-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}
input,
select {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 8px 10px;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th,
td {
  border-bottom: 1px solid #e2e8f0;
  padding: 10px;
  text-align: left;
  vertical-align: middle;
}
th {
  color: #475569;
  font-size: 13px;
}
.permission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}
.permission-grid article {
  padding: 16px;
}
.permission-grid span,
.permission-grid small {
  display: block;
  color: #64748b;
  margin-top: 6px;
}
.permission-grid p {
  margin-top: 10px;
  color: #334155;
}
.swagger-panel {
  height: calc(100vh - 120px);
  min-height: 620px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.swagger-panel iframe {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}
.dialog-mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.5);
}
.dialog {
  width: min(420px, calc(100vw - 32px));
  padding: 22px;
}
.role-dialog {
  width: min(520px, calc(100vw - 32px));
}
.dialog h2 {
  margin-bottom: 14px;
}
.dialog-form {
  display: grid;
  gap: 14px;
}
.dialog-form label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}
.dialog-form input {
  font-weight: 400;
}
.check-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
}
.check-row input {
  width: auto;
}
.dialog-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
@media (max-width: 760px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }
  .admin-sidebar {
    position: static;
  }
}
</style>

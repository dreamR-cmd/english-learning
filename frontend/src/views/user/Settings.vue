<template>
  <div class="settings-page">
    <NavBar />

    <div class="settings-content">
      <div class="settings-hero">
        <div>
          <p class="settings-kicker">Settings</p>
          <h1>学习与账户设置</h1>
          <p class="settings-subtitle">把每日单词练习和个人资料配置集中放在这里，后续其他偏好也统一从这里扩展。</p>
        </div>

        <router-link to="/profile" class="back-link">返回个人中心</router-link>
      </div>

      <div v-if="user" class="settings-grid">
        <section class="settings-card profile-card">
          <div class="card-header">
            <div>
              <h2>个人资料</h2>
              <p>修改头像、昵称和基础展示信息。</p>
            </div>
            <span class="card-badge">Profile</span>
          </div>

          <div class="avatar-panel">
            <button class="avatar-display" type="button" @click="showAvatarPicker = !showAvatarPicker">
              {{ currentAvatar }}
            </button>

            <div class="avatar-meta">
              <div class="display-name">{{ user.nickname || user.username }}</div>
              <div class="account-name">@{{ user.username }}</div>
            </div>
          </div>

          <div v-if="showAvatarPicker" class="avatar-picker">
            <button
              v-for="avatar in avatarOptions"
              :key="avatar"
              type="button"
              class="avatar-option"
              :class="{ active: avatar === currentAvatar }"
              @click="selectAvatar(avatar)"
            >
              {{ avatar }}
            </button>
          </div>

          <label class="form-field">
            <span>昵称</span>
            <input
              v-model.trim="nicknameInput"
              maxlength="20"
              placeholder="请输入昵称"
            />
          </label>

          <div class="card-actions">
            <p v-if="profileMessage" class="action-message success">{{ profileMessage }}</p>
            <p v-else-if="profileError" class="action-message error">{{ profileError }}</p>
            <button class="inline-save-btn" type="button" :disabled="savingProfile" @click="saveProfileSettings">
              {{ savingProfile ? '保存中...' : '保存个人资料' }}
            </button>
          </div>
        </section>

        <section class="settings-card practice-card">
          <div class="card-header">
            <div>
              <h2>每日单词练习设置</h2>
              <p>控制每天抽取的单词数量，影响单词练习页的学习任务规模。</p>
            </div>
            <span class="card-badge">Daily Words</span>
          </div>

          <label class="form-field">
            <span>每日练习目标</span>
            <input
              v-model.number="dailyWordTargetInput"
              type="number"
              min="1"
              max="100"
            />
          </label>

          <div class="target-preview">
            <span class="preview-label">当前预览</span>
            <strong>{{ normalizedDailyWordTarget }} 个 / 天</strong>
          </div>

          <p class="setting-hint">建议设置在 10 到 40 个之间，过高会影响完成率。</p>
          <div class="card-actions">
            <p v-if="practiceMessage" class="action-message success">{{ practiceMessage }}</p>
            <p v-else-if="practiceError" class="action-message error">{{ practiceError }}</p>
            <button class="inline-save-btn" type="button" :disabled="savingPractice" @click="savePracticeSettings">
              {{ savingPractice ? '保存中...' : '保存每日练习设置' }}
            </button>
          </div>
        </section>

        <section class="settings-card extras-card">
          <div class="card-header">
            <div>
              <h2>其他设置</h2>
              <p>预留给后续学习提醒、主题偏好、音频播放等扩展配置。</p>
            </div>
            <span class="card-badge">Coming Next</span>
          </div>

          <div class="placeholder-list">
            <div class="placeholder-item">
              <span class="placeholder-dot"></span>
              学习提醒时间
            </div>
            <div class="placeholder-item">
              <span class="placeholder-dot"></span>
              听力自动播放偏好
            </div>
            <div class="placeholder-item">
              <span class="placeholder-dot"></span>
              页面主题与字体大小
            </div>
          </div>
        </section>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { updateProfile } from '../../utils/api'
import { currentUser, setCurrentUser } from '../../utils/currentUser'

const user = currentUser
const showAvatarPicker = ref(false)
const savingProfile = ref(false)
const savingPractice = ref(false)
const profileMessage = ref('')
const profileError = ref('')
const practiceMessage = ref('')
const practiceError = ref('')
const currentAvatar = ref(user.value?.avatar || '👤')
const nicknameInput = ref(user.value?.nickname || user.value?.username || '')
const dailyWordTargetInput = ref(user.value?.dailyWordTarget || 20)

const avatarOptions = [
  '👤', '😊', '🎯', '📖', '⭐', '👍', '🦉', '🎧',
  '📝', '🧠', '🚀', '🎗️', '🌛', '🐣', '🎃', '😃'
]

const normalizedDailyWordTarget = computed(() => (
  Math.max(1, Math.min(100, Number(dailyWordTargetInput.value) || 20))
))

function selectAvatar(avatar) {
  currentAvatar.value = avatar
  showAvatarPicker.value = false
  profileMessage.value = ''
  profileError.value = ''
}

async function saveProfileSettings() {
  if (!user.value) return

  const nickname = nicknameInput.value.trim()
  if (!nickname) {
    profileError.value = '昵称不能为空'
    profileMessage.value = ''
    return
  }

  savingProfile.value = true
  profileMessage.value = ''
  profileError.value = ''

  try {
    const response = await updateProfile(user.value.id, nickname, null)
    if (response.data.code === 200 && response.data.data) {
      setCurrentUser({ ...response.data.data, avatar: currentAvatar.value })
      nicknameInput.value = response.data.data.nickname || response.data.data.username || nickname
      profileMessage.value = '个人资料已保存'
      return
    }

    profileError.value = response.data.message || '保存失败'
  } catch (error) {
    console.error('Failed to save profile settings', error)
    profileError.value = '保存失败，请稍后重试'
  } finally {
    savingProfile.value = false
  }
}

async function savePracticeSettings() {
  if (!user.value) return

  savingPractice.value = true
  practiceMessage.value = ''
  practiceError.value = ''

  try {
    const response = await updateProfile(user.value.id, null, normalizedDailyWordTarget.value)
    if (response.data.code === 200 && response.data.data) {
      setCurrentUser({ ...response.data.data, avatar: currentAvatar.value })
      dailyWordTargetInput.value = response.data.data.dailyWordTarget || normalizedDailyWordTarget.value
      practiceMessage.value = '每日练习设置已保存'
      return
    }

    practiceError.value = response.data.message || '保存失败'
  } catch (error) {
    console.error('Failed to save daily word settings', error)
    practiceError.value = '保存失败，请稍后重试'
  } finally {
    savingPractice.value = false
  }
}
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(26, 115, 232, 0.12), transparent 24%),
    linear-gradient(180deg, #f7faff 0%, #eef3fb 100%);
}

.settings-content {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 40px;
}

.settings-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.settings-kicker {
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #1a73e8;
  font-weight: 700;
  margin-bottom: 10px;
}

.settings-hero h1 {
  font-size: 34px;
  color: #132238;
  margin-bottom: 10px;
}

.settings-subtitle {
  max-width: 720px;
  color: #5f6b7a;
  line-height: 1.7;
}

.back-link {
  align-self: center;
  text-decoration: none;
  color: #1a73e8;
  background: #fff;
  border: 1px solid #d8e4fb;
  border-radius: 999px;
  padding: 10px 16px;
  transition: all 0.2s;
}

.back-link:hover {
  border-color: #1a73e8;
  box-shadow: 0 8px 18px rgba(26, 115, 232, 0.12);
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 20px;
}

.settings-card {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(215, 227, 244, 0.9);
  border-radius: 24px;
  padding: 24px;
  backdrop-filter: blur(8px);
  box-shadow: 0 18px 40px rgba(31, 54, 88, 0.08);
}

.profile-card {
  grid-column: span 5;
}

.practice-card,
.extras-card {
  grid-column: span 7;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.card-header h2 {
  font-size: 22px;
  color: #15263d;
  margin-bottom: 8px;
}

.card-header p {
  color: #66758a;
  line-height: 1.6;
}

.card-badge {
  flex-shrink: 0;
  padding: 7px 12px;
  border-radius: 999px;
  background: #edf4ff;
  color: #1a73e8;
  font-size: 12px;
  font-weight: 700;
}

.avatar-panel {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
}

.avatar-display {
  width: 88px;
  height: 88px;
  border: none;
  border-radius: 28px;
  background: linear-gradient(135deg, #1a73e8 0%, #68a4ff 100%);
  color: #fff;
  font-size: 42px;
  box-shadow: 0 16px 32px rgba(26, 115, 232, 0.24);
  cursor: pointer;
  transition: transform 0.2s;
}

.avatar-display:hover {
  transform: translateY(-2px);
}

.display-name {
  font-size: 24px;
  font-weight: 700;
  color: #132238;
}

.account-name {
  margin-top: 6px;
  color: #718096;
}

.avatar-picker {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.avatar-option {
  border: 1px solid #d8e4fb;
  border-radius: 16px;
  background: #f8fbff;
  padding: 12px;
  font-size: 28px;
  cursor: pointer;
  transition: all 0.18s;
}

.avatar-option:hover,
.avatar-option.active {
  border-color: #1a73e8;
  background: #e9f2ff;
  transform: translateY(-1px);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-field + .form-field {
  margin-top: 18px;
}

.form-field span {
  font-size: 14px;
  font-weight: 600;
  color: #314257;
}

.form-field input {
  width: 100%;
  border: 1px solid #cfdbef;
  border-radius: 16px;
  padding: 14px 16px;
  font-size: 16px;
  color: #1f2d3d;
  background: #fff;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-field input:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.12);
}

.target-preview {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #1a73e8, #4a90d9);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.preview-label {
  opacity: 0.8;
}

.setting-hint {
  margin-top: 14px;
  color: #6b7280;
  line-height: 1.6;
}

.card-actions {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.inline-save-btn {
  border: none;
  border-radius: 999px;
  background: #15263d;
  color: #fff;
  padding: 11px 18px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.inline-save-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(21, 38, 61, 0.2);
}

.inline-save-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.placeholder-list {
  display: grid;
  gap: 12px;
}

.placeholder-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fbff;
  color: #304255;
  display: flex;
  align-items: center;
  gap: 10px;
}

.placeholder-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #1a73e8;
  box-shadow: 0 0 0 5px rgba(26, 115, 232, 0.12);
}

.action-message {
  font-size: 14px;
}

.action-message.success {
  color: #15803d;
}

.action-message.error {
  color: #b91c1c;
}

@media (max-width: 900px) {
  .profile-card,
  .practice-card,
  .extras-card {
    grid-column: span 12;
  }
}

@media (max-width: 768px) {
  .settings-content {
    padding: 24px 16px 32px;
  }

  .settings-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .settings-hero h1 {
    font-size: 28px;
  }

  .card-header,
  .card-actions,
  .target-preview {
    flex-direction: column;
    align-items: flex-start;
  }

  .inline-save-btn {
    width: 100%;
  }
}
</style>

<template>
  <el-dialog v-model="visible" title="更换头像" width="400px" align-center @closed="destroyCropper">
    <div class="crop-box">
      <img v-show="rawUrl" ref="imgRef" :src="rawUrl" class="crop-img" alt="头像预览" />
      <div v-if="!rawUrl" class="crop-empty">
        <div class="crop-empty-icon">🖼️</div>
        <div class="crop-empty-text">选择一张图片，裁剪为方形头像</div>
      </div>
    </div>
    <input ref="fileRef" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="onPick" />
    <template #footer>
      <el-button @click="pickFile">选择图片</el-button>
      <el-button type="primary" :disabled="!rawUrl" :loading="saving" @click="save">保存头像</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
import { userApi } from '../api'

const emit = defineEmits<{ (e: 'updated', url: string): void }>()

const visible = ref(false)
const rawUrl = ref('')
const saving = ref(false)
const fileRef = ref<HTMLInputElement>()
const imgRef = ref<HTMLImageElement>()
let cropper: Cropper | null = null
let objectUrl = ''

function open() {
  visible.value = true
}

function pickFile() {
  fileRef.value?.click()
}

async function onPick(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!/image\/(jpeg|png|webp)/.test(file.type)) {
    ElMessage.warning('仅支持 jpg / png / webp 图片')
    input.value = ''
    return
  }
  destroyCropper()
  objectUrl = URL.createObjectURL(file)
  rawUrl.value = objectUrl
  await nextTick()
  if (imgRef.value) {
    cropper = new Cropper(imgRef.value, {
      aspectRatio: 1,
      viewMode: 1,
      autoCropArea: 0.9,
      guides: true,
      background: false,
    })
  }
  input.value = ''
}

async function save() {
  if (!cropper) return
  saving.value = true
  try {
    const canvas = cropper.getCroppedCanvas({ width: 256, height: 256 })
    const blob: Blob = await new Promise((resolve) => canvas.toBlob((b) => resolve(b!), 'image/jpeg', 0.9))
    const res: any = await userApi.uploadAvatar(blob)
    const url = res.data.url as string
    localStorage.setItem('avatar', url)
    emit('updated', url)
    ElMessage.success('头像已更新')
    visible.value = false
  } finally {
    saving.value = false
  }
}

function destroyCropper() {
  cropper?.destroy()
  cropper = null
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl)
    objectUrl = ''
  }
  rawUrl.value = ''
}

defineExpose({ open })
</script>

<style scoped>
.crop-box {
  width: 100%;
  height: 300px;
  background: #f5f6fa;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.crop-img { max-width: 100%; display: block; }
/* cropperjs 需要块级图片且容器限定尺寸 */
.crop-box :deep(.cropper-container) { max-width: 100%; max-height: 300px; }
.crop-empty { text-align: center; color: #999; }
.crop-empty-icon { font-size: 40px; margin-bottom: 8px; }
.crop-empty-text { font-size: 13px; }
</style>

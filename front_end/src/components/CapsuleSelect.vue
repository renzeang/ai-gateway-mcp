<template>
  <div class="capsule-select" :class="{ 'is-full-width': width === '100%' }" :style="width !== '100%' ? { width: width } : {}">
    <div
      ref="inputRef"
      class="capsule-input"
      :class="{ 'is-open': isOpen, 'is-disabled': disabled, 'is-clearable': clearable && modelValue }"
      @click="toggle"
    >
      <span class="capsule-input__text">
        {{ modelValue || modelValue === false ? getLabel(modelValue) : placeholder }}
      </span>
      <div class="capsule-input__suffix">
        <span
          v-if="clearable && modelValue"
          class="capsule-input__clear"
          @click.stop="clearValue"
        >
          <svg viewBox="0 0 1024 1024" width="14" height="14">
            <path d="M512 449.333333L667.733333 293.6 730.4 356.266667 574.666667 512 730.4 667.733333 667.733333 730.4 512 574.666667 356.266667 730.4 293.6 667.733333 449.333333 512 293.6 356.266667 356.266667 293.6 512 449.333333z" fill="currentColor"/>
          </svg>
        </span>
        <span class="capsule-input__arrow" :class="{ 'is-open': isOpen }">
          <svg viewBox="0 0 1024 1024" width="16" height="16">
            <path d="M512 682.666667L170.666667 341.333333 256 256l256 256 256-256 85.333333 85.333334z" fill="currentColor"/>
          </svg>
        </span>
      </div>
    </div>

    <div v-show="isOpen" class="capsule-dropdown-wrapper" ref="dropdownWrapperRef" :style="dropdownStyle">
      <div class="capsule-dropdown" ref="dropdownRef">
        <div
          v-for="option in filteredOptions"
          :key="option.value"
          class="capsule-option"
          :class="{ 'is-selected': modelValue === option.value }"
          @click.stop="selectOption(option.value)"
        >
          {{ option.label }}
        </div>
        <div v-if="filteredOptions.length === 0" class="capsule-empty">
          无数据
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'

const props = defineProps({
  modelValue: [String, Number, Boolean],
  options: {
    type: Array,
    required: true
  },
  placeholder: {
    type: String,
    default: '请选择'
  },
  width: {
    type: String,
    default: '200px'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  clearable: {
    type: Boolean,
    default: false
  },
  filterable: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const dropdownRef = ref(null)
const searchTerm = ref('')
const dropdownStyle = ref({})

const inputRef = ref(null)
const dropdownWrapperRef = ref(null)

const filteredOptions = computed(() => {
  if (!props.filterable || !searchTerm.value) return props.options
  const keyword = searchTerm.value.toLowerCase()
  return props.options.filter(opt =>
    opt.label.toLowerCase().includes(keyword) ||
    String(opt.value).toLowerCase().includes(keyword)
  )
})

const getLabel = (value) => {
  const option = props.options.find(o => o.value === value)
  return option ? option.label : value
}

const toggle = () => {
  if (props.disabled) return
  isOpen.value = !isOpen.value
  if (isOpen.value && props.filterable) {
    searchTerm.value = ''
  }
  if (isOpen.value) {
    nextTick(() => {
      updateDropdownPosition()
    })
  }
}

const updateDropdownPosition = () => {
  if (!dropdownWrapperRef.value || !inputRef.value) return

  const rect = inputRef.value.getBoundingClientRect()
  const inputHeight = rect.height
  // y 轴位置越小（越靠上），z-index 越大
  // 下拉面板的 z-index = 基础值 + y 轴偏移
  // 输入框的 z-index = 下拉面板 z-index + 1（确保输入框在自己的下拉面板上方）
  const baseZIndex = 10000
  const yBasedZIndex = Math.floor((window.innerHeight - rect.top) / 10)
  const dropdownZIndex = baseZIndex + yBasedZIndex

  // 设置输入框的 z-index 比下拉面板高 1
  if (inputRef.value) {
    inputRef.value.style.zIndex = (dropdownZIndex + 1).toString()
  }

  dropdownStyle.value = {
    position: 'fixed',
    top: rect.top + inputHeight / 2 + 4 + 'px',
    left: rect.left + 'px',
    width: rect.width + 'px',
    zIndex: dropdownZIndex
  }
}

watch(isOpen, (val) => {
  if (val) {
    nextTick(() => {
      updateDropdownPosition()
    })
  }
})

const selectOption = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
  isOpen.value = false
}

const clearValue = () => {
  emit('update:modelValue', '')
  emit('change', '')
}

const handleClickOutside = (event) => {
  // 如果点击的是输入框或下拉菜单内部，不关闭
  if (inputRef.value?.contains(event.target) || dropdownRef.value?.contains(event.target)) {
    return
  }
  isOpen.value = false
}

const handleScroll = () => {
  if (isOpen.value) {
    updateDropdownPosition()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('scroll', handleScroll, true)
  window.addEventListener('resize', handleScroll)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('scroll', handleScroll, true)
  window.removeEventListener('resize', handleScroll)
})
</script>

<style scoped>
.capsule-select {
  position: relative;
  display: inline-block;
}

.capsule-select.is-full-width {
  display: block;
  width: 100%;
}

.capsule-select.is-full-width .capsule-input {
  width: 100%;
}

.capsule-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 32px;
  padding: 4px 16px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: var(--md3-radius-medium);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  box-sizing: border-box;
  position: relative;
}

.capsule-input:hover {
  border-color: #409eff;
}

.capsule-input.is-open {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.capsule-input.is-disabled {
  background: #f5f7fa;
  cursor: not-allowed;
}

.capsule-input__text {
  flex: 1;
  font-size: 14px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.capsule-input__suffix {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  flex-shrink: 0;
}

.capsule-input__clear {
  display: inline-flex;
  align-items: center;
  color: #c0c4cc;
  cursor: pointer;
  transition: color 0.2s;
}

.capsule-input__clear:hover {
  color: #909399;
}

.capsule-input__arrow {
  display: inline-flex;
  color: #c0c4cc;
  transition: transform 0.3s;
}

.capsule-input__arrow.is-open {
  transform: rotate(180deg);
}

.capsule-dropdown-wrapper {
  position: fixed;
  z-index: 9998;
  pointer-events: none;
}

.capsule-dropdown {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
  border-bottom-left-radius: var(--md3-radius-medium);
  border-bottom-right-radius: var(--md3-radius-medium);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 8px;
  padding-top: 16px;
  max-height: 200px;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: auto;
  pointer-events: auto;
}

.capsule-option {
  padding: 8px 16px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-radius: var(--md3-radius-medium);
  transition: all 0.2s;
  margin: 2px 0;
  user-select: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.capsule-option:hover {
  background: #f5f7fa;
}

.capsule-option.is-selected {
  background: #409eff;
  color: #fff;
}

.capsule-empty {
  padding: 8px 16px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

/* 下拉动画 */
.capsule-drop-enter-active,
.capsule-drop-leave-active {
  transition: all 0.2s ease;
}

.capsule-drop-enter-from,
.capsule-drop-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>

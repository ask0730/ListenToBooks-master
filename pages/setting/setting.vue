<template>
    <gui-page>
        <template v-slot:gBody>
            <view class="gui-padding-x gui-padding">
                <view class="setting-item" @click="openDialog">
                    <uni-section title="退出登录" type="line">
                        <template v-slot:right>
                            <uni-icons type="forward" size="20"></uni-icons>
                        </template>
                    </uni-section>
                </view>
            </view>
        </template>
    </gui-page>
    <!--  微信登录弹窗-->
    <uni-popup ref="dialogRef" type="dialog">
        <uni-popup-dialog type="center" cancelText="取消" confirmText="确定" title="提示" content="是否退出登录？" @confirm="userStore.logout"></uni-popup-dialog>
    </uni-popup>
</template>
<script setup lang="ts">
import { ref } from "vue"
import { storeToRefs } from "pinia"
import { useUserStore } from "../../stores/user"
import GuiImage from "../../Grace6/components/gui-image.vue"
import UniPopup from "../../uni_modules/uni-popup/components/uni-popup/uni-popup.vue"
import { getCurrentPageInfo } from "../../utils/utils"
/* 响应式数据 */
const dialogRef = ref<InstanceType<typeof UniPopup>>()
const userStore = useUserStore()
const props = defineProps({
  redirect: {
    type: String,
    default: "",
  },
})
// 分类导航

/* 方法 */
// 打开微信登录弹窗
const openDialog = () => {
  dialogRef.value.open()
}

// 使用微信登录

//获取用户个人信息方法

/* 生命周期 */
</script>

<style scoped>
.setting-item {
  border-bottom: 1px solid rgba(0, 0, 0, 0.5);
}

:deep(.uni-section .uni-section-header__decoration) {
  background-color: #FFD700  !important;
}
</style>

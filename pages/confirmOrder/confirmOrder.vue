<template>
	<gui-page>
		<template v-slot:gBody>
			<view class="gui-flex gui-column gui-m-40 gui-bg-black gui-p-20 gui-border-radius">
				<view class="gui-flex gui-row gui-space-between">
					<text class="gui-color-white">购买账号</text>
					<text class="gui-color-white">账号余额</text>
				</view>
				<view class="gui-flex gui-row gui-space-between gui-m-t-20 gui-color-white">
					<text class="gui-h4">{{ userStore.user.nickname }}</text>
					<view>
						<text class="gui-h4 gui-bold gui-color-white">{{ userStore.amount }}</text>
					</view>
				</view>
			</view>

			<view class="gui-bg-black gui-p-20 gui-border-radius">
				<uni-card
					margin="5px"
					padding="5px"
					v-for="item in orderStore.submitOrderInfo.orderDetailVoList"
					:key="item.itemId">
					<view class="gui-flex gui-align-items-center">
						<image
							class="gui-album-img"
							mode="aspectFill"
							:src="item.itemUrl"
						></image>

						<view
							class="gui-flex gui-column gui-flex1 gui-m-l-20">
							<text class="gui-block gui-color-white gui-ellipsis-line2 gui-text">{{item.itemName}}</text>
							<text class="gui-text-small gui-m-t-20 gui-m-b-20 gui-block gui-color-red">7天无忧退</text>
							<view class="gui-flex gui-space-between gui-align-items-end">
								<text class="gui-color-white gui-h3 gui-bold">￥{{ item.itemPrice }}</text>
								<text class="gui-block gui-color-white">
									<text class="gui-text-small">X</text>
									1
								</text>
							</view>
						</view>

					</view>
				</uni-card>


				<!-- <view class="split-line gui-m-t-20 gui-m-b-20"></view> -->

				<view class="gui-flex gui-justify-content-end gui-m-t-40 gui-m-b-100">
					<text class="gui-bold gui-color-white">
						小计:
						<text class="gui-color-red">￥{{orderStore.submitOrderInfo.originalAmount}}</text>
					</text>
				</view>
			</view>

			<view class="gui-shoppingcard-footer gui-border-box" style="background-color: #000000;">
				<view class="gui-flex gui-row gui-space-between gui-align-items-center">
					<view class="gui-flex gui-column">
						<text class="gui-color-white gui-text gui-primary-text gui-flex gui-row gui-nowrap">
							合计:
							<text class="gui-color-red gui-h4">￥{{ orderStore.submitOrderInfo.orderAmount }}</text>
						</text>
						<text v-if="Number(orderStore.submitOrderInfo.derateAmount)" class="gui-text-small gui-color-white">已优惠￥{{ orderStore.submitOrderInfo.derateAmount }}</text>
					</view>
					<text class="gui-shoppingcard-checkout gui-bg-orange gui-block gui-color-black" @click="handleCheckout">立即结算</text>
				</view>
				<view><gui-iphone-bottom></gui-iphone-bottom></view>
			</view>
		</template>
	</gui-page>
	<PayTypeSelect :alipaySetting="{isShow:false}" ref="payTypeSelectRef"></PayTypeSelect>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useUserStore } from "../../stores/user"
import { useOrderStore } from "../../stores/order"
import PayTypeSelect from "../../components/PayTypeSelect/PayTypeSelect.vue"
import { PAY_WAY_MAP } from "../../utils/constant"
/* 响应式数据 */
const userStore = useUserStore()
const orderStore = useOrderStore()
const payTypeSelectRef= ref<InstanceType<typeof PayTypeSelect>>()
// 分类导航

/* 方法 */
// 结算
const handleCheckout = () => {
	// 判断支付方式是否为余额支付，否则弹出支付方式选择框
	if (orderStore.submitOrderInfo.payWay === PAY_WAY_MAP.Balance) {
		orderStore.submitOrder()
	} else {
		payTypeSelectRef.value.open()
	}


}

/* 生命周期 */
</script>

<style lang="scss">
.gui-album-img {
	width: 150rpx;
	height: 150rpx;
	border-radius: 30rpx;
}

.split-line {
	height: 1rpx;
	width: 100%;
	background-color: #e5e5e5;
}

.gui-shoppingcard-footer {
	position: fixed;
	z-index: 99;
	left: 0;
	bottom: 0;
	width: 750rpx;
	padding-left: 30rpx;
}


.gui-shoppingcard-checkout {
	width: 260rpx;
	height: 100rpx;
	line-height: 100rpx;
	font-size: 28rpx;
	text-align: center;
}

/* 如果购物车放在 switch 页面并且是 h5 端请打开注释启用下面的样式 */
/* #ifdef H5 */
/* .gui-shoppingcard-footer{bottom:50px;} */
/* #endif */

/* 深度选择器设置背景颜色（如果样式有作用域） */
::v-deep .uni-card {
  background-color: #000 !important;
  border: 1px solid #FFD700 !important;
}
</style>

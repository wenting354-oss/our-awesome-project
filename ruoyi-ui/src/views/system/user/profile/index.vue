<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6" :xs="24">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>🎓 校园身份卡</span>
          </div>
          <div>
            <div class="text-center">
              <userAvatar :user="user" />
            </div>
            <ul class="list-group list-group-striped">
              <li class="list-group-item">
                <svg-icon icon-class="user" /> 用户昵称
                <div class="pull-right">{{ user.nickName }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="phone" /> 手机号码
                <div class="pull-right">{{ user.phonenumber }}</div>
              </li>
              <li class="list-group-item">
                <i class="el-icon-postcard"></i> 学号 / 账号
                <div class="pull-right">{{ user.userName }}</div>
              </li>

              <template v-if="user.userName !== 'admin'">
                <li class="list-group-item">
                  <i class="el-icon-school"></i> 所属学院
                  <div class="pull-right">{{ user.college || '未填写' }}</div>
                </li>
                <li class="list-group-item">
                  <i class="el-icon-reading"></i> 主修专业
                  <div class="pull-right">{{ user.major || '未填写' }}</div>
                </li>
                <li class="list-group-item">
                  <i class="el-icon-user"></i> 所在班级
                  <div class="pull-right">{{ user.className || '未填写' }}</div>
                </li>
              </template>

              <template v-else>
                <li class="list-group-item">
                  <i class="el-icon-s-custom"></i> 平台身份
                  <div class="pull-right" style="color: #f5222d; font-weight: bold;">官方超级管理员</div>
                </li>
              </template>

              <li class="list-group-item">
                <svg-icon icon-class="date" /> 注册时间
                <div class="pull-right">{{ user.createTime }}</div>
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
      <el-col :span="18" :xs="24">
        <el-card>
          <div slot="header" class="clearfix">
            <span>基本资料</span>
          </div>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="userinfo">
              <userInfo :user="user" />
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="resetPwd">
              <resetPwd :user="user" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import userAvatar from "./userAvatar";
import userInfo from "./userInfo";
import resetPwd from "./resetPwd";
import { getUserProfile } from "@/api/system/user";

export default {
  name: "Profile",
  components: { userAvatar, userInfo, resetPwd },
  data() {
    return {
      user: {},
      roleGroup: {},
      postGroup: {},
      activeTab: "userinfo"
    };
  },
  created() {
    this.getUser();
  },
  methods: {
    getUser() {
      getUserProfile().then(response => {
        this.user = response.data;
        this.roleGroup = response.roleGroup;
        this.postGroup = response.postGroup;
      });
    }
  }
};
</script>

<style scoped>
.list-group-item i {
  margin-right: 5px;
  color: #8a979e;
}
</style>

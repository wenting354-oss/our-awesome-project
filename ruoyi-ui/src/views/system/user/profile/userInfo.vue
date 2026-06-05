<template>
  <el-form ref="form" :model="user" :rules="rules" label-width="80px">
    <el-row>
      <el-col :span="12">
        <el-form-item label="用户昵称" prop="nickName">
          <el-input v-model="user.nickName" maxlength="30" placeholder="请输入你在校园圈的昵称" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="手机号码" prop="phonenumber">
          <el-input v-model="user.phonenumber" maxlength="11" placeholder="买家可能通过此号码联系你" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="12">
        <el-form-item label="用户性别">
          <el-radio-group v-model="user.sex">
            <el-radio label="0">男同学</el-radio>
            <el-radio label="1">女同学</el-radio>
            <el-radio label="2">保密</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="联系邮箱" prop="email">
          <el-input v-model="user.email" maxlength="50" placeholder="请输入你的邮箱地址" />
        </el-form-item>
      </el-col>
    </el-row>

    <div v-if="user.userName !== 'admin'">
      <el-divider content-position="left">🎓 完善校园实名档案 (增加买家信任度)</el-divider>

      <el-form-item label="你的学号">
        <el-input v-model="user.userName" disabled placeholder="学号即登录账号，不可在此修改" />
      </el-form-item>

      <el-row>
        <el-col :span="12">
          <el-form-item label="所属学院" prop="college">
            <el-select v-model="user.college" placeholder="请选择或输入学院" filterable allow-create style="width: 100%;">
              <el-option label="计算机科学与技术学院" value="计算机科学与技术学院" />
              <el-option label="经济管理学院" value="经济管理学院" />
              <el-option label="外国语学院" value="外国语学院" />
              <el-option label="机械与动力工程学院" value="机械与动力工程学院" />
              <el-option label="艺术与传媒学院" value="艺术与传媒学院" />
              <el-option label="理学院" value="理学院" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="主修专业" prop="major">
            <el-input v-model="user.major" maxlength="30" placeholder="例如: 软件工程" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="所在班级" prop="className">
        <el-input v-model="user.className" maxlength="20" placeholder="例如: 计科2101班" style="width: 48%;" />
      </el-form-item>
    </div>

    <el-form-item style="margin-top: 30px;">
      <el-button type="primary" size="small" icon="el-icon-check" @click="submit">保存更改</el-button>
      <el-button type="danger" size="small" icon="el-icon-close" @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserProfile } from "@/api/system/user";

export default {
  props: {
    user: {
      type: Object
    }
  },
  data() {
    return {
      // 表单校验
      rules: {
        nickName: [
          { required: true, message: "用户昵称不能为空", trigger: "blur" }
        ],
        email: [
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        phonenumber: [
          { required: true, message: "手机号码不能为空", trigger: "blur" },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ]
      }
    };
  },
  methods: {
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateUserProfile(this.user).then(response => {
            this.$modal.msgSuccess("修改成功");
            // 同步更新 Vuex 中缓存的名字和头像等
            this.$store.commit("SET_NAME", this.user.nickName);
          });
        }
      });
    },
    close() {
      this.$store.dispatch("tagsView/delView", this.$route).then(() => {
        this.$router.push({ path: "/index" });
      });
    }
  }
};
</script>

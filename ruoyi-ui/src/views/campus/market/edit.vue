<template>
  <div class="edit-container">
    <el-card class="box-card">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small" v-loading="loading">
        <el-form-item label="商品标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入商品标题，不超过50个字符"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="商品分类" prop="category">
          <el-select v-model="form.category" placeholder="选择商品分类">
            <el-option label="电子产品" value="1" />
            <el-option label="图书教材" value="2" />
            <el-option label="生活用品" value="3" />
            <el-option label="服装配饰" value="4" />
            <el-option label="其他" value="5" />
          </el-select>
        </el-form-item>

        <el-form-item label="商品价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0.01"
            :max="99999"
            :precision="2"
            placeholder="请输入价格"
          />
        </el-form-item>

        <el-form-item label="新旧程度" prop="condition">
          <el-select v-model="form.condition" placeholder="选择商品新旧程度">
            <el-option label="全新" value="1" />
            <el-option label="几乎全新" value="2" />
            <el-option label="九成新" value="3" />
            <el-option label="八成新" value="4" />
            <el-option label="七成新" value="5" />
            <el-option label="较旧" value="6" />
          </el-select>
        </el-form-item>

        <el-form-item label="商品图片" prop="imageUrls">
          <el-upload
            action="/upload"
            list-type="picture-card"
            :auto-upload="true"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :file-list="fileList"
            multiple
            :limit="9"
          >
            <i class="el-icon-plus"></i>
          </el-upload>
          <div class="upload-tip">最多上传9张图片，首张图为商品列表展示图</div>
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            placeholder="请详细描述商品信息、使用情况、缺陷等，越详细越好"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系电话" prop="sellerPhone">
          <el-input v-model="form.sellerPhone" placeholder="请输入您的联系电话（可选）" />
        </el-form-item>

        <el-form-item label="微信号" prop="sellerWeChat">
          <el-input v-model="form.sellerWeChat" placeholder="请输入您的微信号（可选）" />
        </el-form-item>

        <el-form-item label="QQ号" prop="sellerQQ">
          <el-input v-model="form.sellerQQ" placeholder="请输入您的QQ号（可选）" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">保存更改</el-button>
          <el-button @click="$router.go(-1)">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getProduct, updateProduct } from '@/api/campus/product'

export default {
  name: 'EditProduct',
  data() {
    return {
      loading: false,
      submitLoading: false,
      fileList: [],
      form: {
        productId: '',
        title: '',
        category: '',
        price: '',
        condition: '',
        imageUrls: '',
        description: '',
        sellerPhone: '',
        sellerWeChat: '',
        sellerQQ: ''
      },
      rules: {
        title: [{ required: true, message: '商品标题不能为空', trigger: 'blur' }],
        category: [{ required: true, message: '商品分类不能为空', trigger: 'change' }],
        price: [{ required: true, message: '商品价格不能为空', trigger: 'blur' }],
        condition: [{ required: true, message: '新旧程度不能为空', trigger: 'change' }],
        description: [{ required: true, message: '商品描述不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getDetail()
  },
  methods: {
    getDetail() {
      const productId = this.$route.query.id
      if (!productId) {
        this.$message.error('商品ID不存在')
        this.$router.go(-1)
        return
      }

      this.loading = true
      getProduct(productId)
        .then(response => {
          this.form = response.data || {}
          this.loading = false
        })
        .catch(() => {
          this.$message.error('加载商品信息失败')
          this.loading = false
        })
    },
    handleUploadSuccess(response, file) {
      if (response.code === 0) {
        const urls = this.form.imageUrls ? this.form.imageUrls.split(',') : []
        urls.push(response.fileName)
        this.form.imageUrls = urls.join(',')
        this.$message.success('图片上传成功')
      } else {
        this.$message.error(response.msg || '图片上传失败')
      }
    },
    handleUploadError() {
      this.$message.error('图片上传失败，请重试')
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (!this.form.imageUrls) {
            this.$message.error('请至少上传一张商品图片')
            return
          }

          this.submitLoading = true
          updateProduct(this.form)
            .then(() => {
              this.$message.success('商品更新成功')
              this.$router.push({ name: 'MyProducts' })
            })
            .catch(() => {
              this.$message.error('商品更新失败，请重试')
              this.submitLoading = false
            })
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.edit-container {
  padding: 20px;

  .upload-tip {
    margin-top: 10px;
    color: #999;
    font-size: 12px;
  }
}
</style>

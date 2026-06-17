// 通义千问 API 配置
export const QWEN_API_KEY = 'sk-ad82ffbaa1a6462dbbcea79fa465f148';

// PC端使用本地代理路径解决跨域问题（对应 vue.config.js 里的配置）
export const QWEN_API_URL = `/dashscope-api/api/v1/services/aigc/text-generation/generation`;

/**
 * 调用通义千问 API
 * @param {Array} messages - 对话历史
 * @param {Array} tools - Function定义（可选）
 */
export async function callQwenAI(messages, tools = null) {
  const requestBody = {
    model: 'qwen-plus', // 使用qwen-plus模型
    input: {
      messages: messages
    },
    parameters: {
      result_format: 'message'
    }
  };

  if (tools) {
    requestBody.parameters.tools = tools;
  }

  console.log('通义千问请求:', requestBody);

  try {
    // 🚀 将 uni.request 替换为标准的 fetch API
    const response = await fetch(QWEN_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${QWEN_API_KEY}`
      },
      body: JSON.stringify(requestBody)
    });

    // 解析响应 JSON
    const data = await response.json();
    console.log('通义千问响应:', data);

    if (response.ok && data && data.output) {
      return {
        success: true,
        data: data.output
      };
    } else {
      return {
        success: false,
        error: `API错误: ${JSON.stringify(data)}`
      };
    }
  } catch (error) {
    console.error('通义千问请求异常:', error);
    return {
      success: false,
      error: `请求失败: ${error.message || '网络错误'}`
    };
  }
}

/**
 * 定义Agent Functions（通义千问格式）
 * (这部分工具定义完全是标准的 JS 对象，无需修改，直接复用)
 */
export const agentTools = [
  {
    type: 'function',
    function: {
      name: 'query_schedule',
      description: '查询用户的课程表，可以查询今天、明天、或指定星期几的课程',
      parameters: {
        type: 'object',
        properties: {
          day_type: {
            type: 'string',
            description: '查询的日期类型',
            enum: ['today', 'tomorrow', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday']
          }
        },
        required: ['day_type']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'create_errand_task',
      description: '帮用户创建并发布跑腿任务到系统',
      parameters: {
        type: 'object',
        properties: {
          task_type: {
            type: 'string',
            description: '任务类型',
            enum: ['取快递', '外卖代购', '代购', '其他']
          },
          title: {
            type: 'string',
            description: '任务标题，简短描述任务'
          },
          detail: {
            type: 'string',
            description: '任务详细说明'
          },
          reward: {
            type: 'number',
            description: '悬赏金额（元）'
          },
          delivery_address: {
            type: 'string',
            description: '送达地址'
          }
        },
        required: ['task_type', 'title', 'detail', 'reward', 'delivery_address']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'create_circle_post',
      description: '帮用户在校园圈子发布帖子',
      parameters: {
        type: 'object',
        properties: {
          category: {
            type: 'string',
            description: '帖子分类',
            enum: ['二手市场', '求助板块', '兼职发布', '恋爱交友']
          },
          title: {
            type: 'string',
            description: '帖子标题'
          },
          content: {
            type: 'string',
            description: '帖子正文内容'
          }
        },
        required: ['category', 'title', 'content']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'create_secondhand_product',
      description: '帮用户在二手市场发布商品',
      parameters: {
        type: 'object',
        properties: {
          title: {
            type: 'string',
            description: '商品标题'
          },
          description: {
            type: 'string',
            description: '商品描述'
          },
          category: {
            type: 'string',
            description: '商品分类',
            enum: ['数码产品', '图书教材', '生活用品', '其他']
          },
          price: {
            type: 'number',
            description: '商品价格（元）'
          },
          contact_info: {
            type: 'string',
            description: '联系方式（微信/QQ/电话）'
          }
        },
        required: ['title', 'description', 'category', 'price', 'contact_info']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'search_secondhand_product',
      description: '帮用户搜索二手市场的商品',
      parameters: {
        type: 'object',
        properties: {
          keyword: {
            type: 'string',
            description: '搜索关键词（商品名称或描述）'
          },
          category: {
            type: 'string',
            description: '商品分类（可选）',
            enum: ['数码产品', '图书教材', '生活用品', '其他', '全部']
          }
        },
        required: ['keyword']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'buy_secondhand_product',
      description: '帮用户购买二手商品，创建订单',
      parameters: {
        type: 'object',
        properties: {
          product_id: {
            type: 'number',
            description: '要购买的商品ID'
          },
          address: {
            type: 'string',
            description: '收货地址'
          },
          remark: {
            type: 'string',
            description: '订单备注（可选）'
          }
        },
        required: ['product_id', 'address']
      }
    }
  }
];

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// Vant 组件
import { 
  Button, NavBar, Tabbar, TabbarItem, Icon, Image as VanImage,
  Form, Field, CellGroup, Cell, Toast, Dialog, ActionSheet,
  Uploader, List, PullRefresh, Empty, Loading, Popup, Picker,
  Calendar, DatePicker, TextArea, Tag, Divider, Grid, GridItem,
  FloatingBubble, Swipe, SwipeItem, ImagePreview, Tab, Tabs
} from 'vant'
import 'vant/lib/index.css'

// 全局样式
import './styles/global.css'

const app = createApp(App)

// 注册 Vant 组件
const vantComponents = [
  Button, NavBar, Tabbar, TabbarItem, Icon, VanImage,
  Form, Field, CellGroup, Cell, Toast, Dialog, ActionSheet,
  Uploader, List, PullRefresh, Empty, Loading, Popup, Picker,
  Calendar, DatePicker, TextArea, Tag, Divider, Grid, GridItem,
  FloatingBubble, Swipe, SwipeItem, ImagePreview, Tab, Tabs
]
vantComponents.forEach(component => {
  app.use(component)
})

app.use(createPinia())
app.use(router)
app.mount('#app')

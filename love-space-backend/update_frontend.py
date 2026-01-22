
import os

def update_api_index():
    file_path = '../love-space-frontend/src/api/index.js'
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'uploadAvatar:' in content:
        print("API already updated")
        return

    new_code = """  // 用户相关
  user: {
    uploadAvatar: (file) => {
      const formData = new FormData()
      formData.append('file', file)
      return http.post('/users/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    }
  },
  
  // 仪表盘"""
    
    new_content = content.replace('// 仪表盘', new_code)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Updated api/index.js")

def update_profile_vue():
    file_path = '../love-space-frontend/src/views/Profile.vue'
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    old_code = """    const res = await api.file.upload(file)
    if (res.code === 200) {
      await userStore.updateProfile({ avatar: res.data })
      showToast({ message: '头像更新成功', icon: 'success' })
    }"""
    
    new_code = """    const res = await api.user.uploadAvatar(file)
    if (res.code === 200) {
      // 更新 store 中的用户信息
      userStore.user.avatar = res.data
      // 更新本地存储
      localStorage.setItem('user', JSON.stringify(userStore.user))
      showToast({ message: '头像更新成功', icon: 'success' })
    } else {
      showToast(res.message || '上传失败')
    }"""
    
    if new_code in content:
         print("Profile.vue already updated")
         return

    # Normalize line endings just in case
    content = content.replace('\r\n', '\n')
    
    if old_code in content:
        new_content = content.replace(old_code, new_code)
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print("Updated Profile.vue")
    else:
        print("Could not find code block in Profile.vue to replace")
        # Fallback: try to find without exact whitespace match if needed, but let's see.
        # Actually, let's try to be robust.
        pass

if __name__ == '__main__':
    try:
        update_api_index()
        update_profile_vue()
    except Exception as e:
        print(f"Error: {e}")

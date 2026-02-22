# 推送到 GitHub 指南

## 当前状态

所有更改已提交到本地 Git 仓库，包括：
- ✅ Bug 修复和代码改进
- ✅ 成功构建的 APK (7.1MB)
- ✅ GitHub Actions 工作流 (build-javaV3.1.yaml)
- ✅ 完整的文档 (README.md, BUGFIXES.md)
- ✅ 本地构建脚本 (build-apk.sh)

## 推送方法

### 方法 1: 使用 HTTPS (推荐)

需要 GitHub Personal Access Token (PAT)

```bash
cd "/Users/charlie-macmini/Documents/python/优化工具/安卓测试软件"

# 推送到 GitHub
git push https://github.com/Charlielian/SignalTestApp.git main
```

**获取 Personal Access Token:**
1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token" → "Generate new token (classic)"
3. 选择权限: `repo` (完整仓库访问)
4. 生成并复制 token
5. 在推送时使用 token 作为密码

### 方法 2: 配置 Git 凭据助手

```bash
# 配置凭据助手（macOS）
git config --global credential.helper osxkeychain

# 然后推送（会提示输入用户名和 token）
git push origin main
```

### 方法 3: 使用 SSH (需要配置 SSH 密钥)

```bash
# 1. 生成 SSH 密钥（如果还没有）
ssh-keygen -t ed25519 -C "your_email@example.com"

# 2. 添加 SSH 密钥到 ssh-agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# 3. 复制公钥并添加到 GitHub
cat ~/.ssh/id_ed25519.pub
# 访问 https://github.com/settings/keys 添加密钥

# 4. 更改远程 URL 为 SSH
git remote set-url origin git@github.com:Charlielian/SignalTestApp.git

# 5. 推送
git push origin main
```

## 推送后的自动化

推送成功后，GitHub Actions 会自动：
1. ✅ 构建 Debug 和 Release APK
2. ✅ 运行 Lint 检查
3. ✅ 生成构建报告
4. ✅ 创建 GitHub Release (如果推送到 main 分支)
5. ✅ 上传 APK 文件作为 artifacts

## 查看构建结果

推送后，访问以下链接查看构建状态：
- Actions: https://github.com/Charlielian/SignalTestApp/actions
- Releases: https://github.com/Charlielian/SignalTestApp/releases

## 手动触发构建

在 GitHub 上可以手动触发构建：
1. 访问 https://github.com/Charlielian/SignalTestApp/actions
2. 选择 "Build Android APK V3.1" 工作流
3. 点击 "Run workflow"
4. 可选：输入自定义版本号

## 待推送的提交

```
2f6e4d35 Add enhanced GitHub Actions workflow V3.1
41fd072b Add comprehensive documentation and build script
57c3550f Fix build issues and generate APK successfully
dde62e08 Fix critical bugs and improve SignalTestApp
```

## 推送命令（快速复制）

```bash
# 使用 HTTPS 推送
cd "/Users/charlie-macmini/Documents/python/优化工具/安卓测试软件"
git push https://github.com/Charlielian/SignalTestApp.git main
```

## 验证推送成功

推送成功后，你应该能看到：
1. GitHub 仓库中的新提交
2. Actions 标签页中的构建任务
3. 几分钟后生成的 Release（如果推送到 main 分支）

## 故障排除

### 问题: 推送被拒绝 (rejected)
```bash
# 先拉取远程更改
git pull origin main --rebase
# 然后再推送
git push origin main
```

### 问题: 认证失败
- 确保使用正确的 Personal Access Token
- Token 需要有 `repo` 权限
- 用户名是你的 GitHub 用户名

### 问题: 远程仓库不存在
- 确认仓库 URL: https://github.com/Charlielian/SignalTestApp
- 确认你有该仓库的写入权限

## 下一步

推送成功后：
1. 检查 GitHub Actions 构建状态
2. 下载构建的 APK 进行测试
3. 查看自动生成的 Release notes
4. 如有需要，更新 README 中的 Google Maps API 密钥配置

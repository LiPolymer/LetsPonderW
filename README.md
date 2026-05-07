# Let's PonderW!
## 一起思索!

本 Mod 会根据您已安装的mod自动从[思索索引仓库](https://gitlab.com/LiPolymer/LetsPonderWIndex)下载思索文件

通过向索引仓库提交思索来实现思索共享!

[`GitLab 索引(接受MR)`](https://gitlab.com/LiPolymer/LetsPonderWIndex)
[`GitHub 索引(镜像, 接受PR)`](https://github.com/LiPolymer/LetsPonderWIndex)

<img src="https://lipoly.ink/assets/badges/humanity.svg" alt="This project was built by human" width="200">

本 Mod 使用 [Ponderer](https://github.com/Nobodiiiii/Ponderer) 来编辑与加载自定义思索

*我们也欢迎各位开发者向我们的索引仓库提交 MR/PR，与所有玩家分享你的思索创作！*

### 核心特性

* **智能按需下载**：启动时，模组会读取云端索引并与本地安装的模组进行比对，仅下载当前环境真正需要的思索（Ponder）片段。
* **基于哈希的增量更新**：通过在本地缓存的 `assemble.json` 中记录文件哈希值，模组能精准识别线上资源的变更并跳过未修改的文件，从而大幅节省带宽。
* **无缝自动加载**：下载完成后，模组会自动构建一个名为 `Let'sPw Assembled Pack` 的内置资源包。它会将相应的脚本和结构文件直接迁移并注册到 Ponderer 的配置目录中，免去了玩家手动解压或移动任何文件的繁琐步骤。
* **状态可视化** *（可选）*：资源加载和组装完成后，游戏右上角会弹出一个 Toast 提示，直观地告知你当前已加载思索片段的状态。

### 配置文件

首次运行后，模组会在 config 目录生成一个 `letsPonderW.json` 文件，支持以下高度可定制的选项：

* `repository`（字符串）：思索索引仓库的自定义地址。默认为[GitLab 索引](https://gitlab.com/LiPolymer/LetsPonderWIndex)。你可以将其替换为自己维护的私有思索源。
* `onlyLoad`（布尔值）：设置为 `true` 时，模组将完全跳过网络下载和更新阶段，仅加载本地缓存的思索资源。这非常适合离线游玩，或是希望锁定内容的整合包作者使用。
* `normalToast`（布尔值）：切换 Toast 提示的显示样式。设置为 `true` 时，将显示简化的加载提示。

### 前置要求

请确保你的游戏环境中已安装以下前置模组：

* **NeoForge**：模组加载器。
* **Kotlin for Forge (≥ 5.0.0)**：提供必要的 Kotlin 运行环境支持。
* **Ponderer**：游戏内的思索加载与编辑器。
* 
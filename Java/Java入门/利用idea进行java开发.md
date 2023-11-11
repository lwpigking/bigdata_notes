## 安装前的准备工作

开始安装之前，我们需要进行一些准备，首先是**社区版和旗舰版该选谁**，接着是**软硬件的支持**。

### 旗舰版 VS 社区版

对于旗舰版和社区版，我从官网给出的对比从而摘选了如下图所示的区别。可以看出旗舰版无论在 **语言支持、框架支持、Build 工具、部署工具、版本控制** 等多方面都比社区版更加强大，但是最大的区别也是我们最为关心的一点：**旗舰版收费，而社区版免费**， 所以可以根据自己的需求进行选择。

![](../assets/20220703-learn-java-with-idea/ul-vs-community.png)

### 软件

- IntelliJ IDEA 安装包，下载地址：https://www.jetbrains.com/idea/download/，选择自己系统对应安装包即可。

### 硬件

| 配置项目     | 最低配置                                                                                                                                              | 推荐配置                                |
| ------------ | :---------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------- |
| **内存**     | 2G +                                                                                                                                                  | 8 G +                                   |
| **硬盘**     | 2.5 GB + 1 GB 缓存                                                                                                                                    | 至少 5GB+ 空间的 SSD                    |
| **显示器**   | 1024 x 768                                                                                                                                            | 1920×1080                               |
| **操作系统** | 官方放出的 64 位操作系统：<br />1. Windows 8 +<br />2. macOS 10.13 +<br />3. Linux 的 Gnome、KDE、Unity 桌面环境的支持<br />4. 预览版系统可能支持不好 | 最新的 64 位 Windows、macOS、Linux 系统 |

## 安装工作

### Ubuntu

#### 安装

1. 下载安装包，根据自己的需求选择旗舰版或是社区版

![](../assets/20220703-learn-java-with-idea/ubuntu-download.png)

2. 将下载好的安装包 `ideaIU-2020.1.1.tar.gz` 移动到平时存放软件的目录下进行解压缩

```shell
tar -zxvf ideaIU-2020.1.1.tar.gz
```

![解压](../assets/20220703-learn-java-with-idea/unzip.png)

![解压后的目录结构](../assets/20220703-learn-java-with-idea/strucrure.png)

3. 进入解压后的 `bin` 目录，然后在终端启动如下命令即可：

```shell
./idea.sh
```

![](../assets/20220703-learn-java-with-idea/sh-idea.png)

4. 进入初始化启动后的设置即可。

首先是 UI 主题选择，根据自己的喜好选择 Light 还是 Darcula 模式；

![图片源自 IDEA 官网](../assets/20220703-learn-java-with-idea/ui.png)

接着是否创建启动器脚本，同时选择自己的脚本保存目录

![图片源自 IDEA 官网](../assets/20220703-learn-java-with-idea/shell-path.png)

接着选择自己想要的插件和禁用无关插件，根据自己的需求安装即可，如果不知道怎么选择，默认即可。

![图片源自 IDEA 官网](../assets/20220703-learn-java-with-idea/plugin.png)

选择额外的插件支持，按需安装，默认不勾选，后续也可以自行安装。

![图片源自 IDEA 官网](../assets/20220703-learn-java-with-idea/other-pulgin.png)

#### 卸载

Linux 下卸载只需要将对应目录（即刚才解压后的目录）删除即可。

### Windows

#### 安装

安装过程和 Ubuntu 大致相同，只是 Windows 中的安装包是可执行文件，只需要双击进行安装即可，然后后面选择自己的安装路径即可，安装好之后的设置和 Ubuntu 一致。

#### 卸载

进入 Windows 设置，然后进入应用，然后搜索 IDEA，选择卸载即可。

![](../assets/20220703-learn-java-with-idea/uninstall.png)

### macOS

#### 安装

1.  首先去 [官网下载](https://www.jetbrains.com/idea/download/#section=mac) 对应安装包；

![](../assets/20220703-learn-java-with-idea/macos-down.png)

2.  然后双击下载好的 `.dmg` 安装包，等待验证后拖拽到 `Applications` 即可；

![](../assets/20220703-learn-java-with-idea/macos-install.png)

3.  安装成功，打开的主页面如下图所示；

![](../assets/20220703-learn-java-with-idea/idea-main-ui.png)

#### 卸载

打开访达中的 **应用程序**，然后找到 IDEA，将其拖到 **🗑️ 废纸篓** 即可，也可以单击鼠标右键，然后 **移到废纸篓**；

![](../assets/20220703-learn-java-with-idea/uninstall-macos.png)

## 创建 Java 项目

[上一篇](https://mp.weixin.qq.com/s/geOWlAwVMhtmmIMvFvjSpQ) 文章中，我们介绍了如何利用编辑器写我们的 `Hello World`，接下来就讲讲如何利用 IDEA 来创建并书写我们的 `Hello World` 程序。

1.  依次进入 `File -> New -> Project`，然后选择左侧 Java，配置项目 JDK；

![](../assets/20220703-learn-java-with-idea/new-project.png)

2.  下一步之后，如果勾选上面的选项，就会从模板创建一个项目（一般都是 `HelloWorld`），不勾选就是一个空项目，看自己选择；

![](../assets/20220703-learn-java-with-idea/template.png)

3.  再接下来，就是设置项目名以及项目存储目录了；

![](../assets/20220703-learn-java-with-idea/pro-name.png)

4.  最后点击完成，我们的普通 Java 项目就创建成功了；

![](../assets/20220703-learn-java-with-idea/finish-pro.png)

5.  项目创建成功了，我们在项目 `src` 目录上右键新建一个 Java 类文件。注意，一般来讲，我们倾向于把类的命名方式定义为首字母大写的驼峰命名法。然后填入如下代码，一个 简单的 `HelloWorld` 就完成了；

```java
/**
 * @author : cunyu
 * @version : 1.0
 * @className : HelloWorld
 * @date : 2020/10/23 11:14
 * @description : HelloWorld
 */

public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
```

![](../assets/20220703-learn-java-with-idea/helloworld.png)

6.  最后运行我们刚才写的代码，可以看到输出了 `Hello World`，和我们在上一篇文章中通过控制台打印出的一样！

```bash
Hello World!
```


# WSL 安装配置

安装过程可以参考 [微软官方文档](https://docs.microsoft.com/zh-cn/windows/wsl/install) ，此处暂时省略。

- [WSL 安装配置](#wsl-安装配置)
  - [配置root账户](#配置root账户)
  - [配置root账户命令行颜色](#配置root账户命令行颜色)
  - [配置默认登陆账号为root](#配置默认登陆账号为root)
  - [配置默认WSL](#配置默认wsl)
  - [切换软件源(此处以Debian为例)](#切换软件源此处以debian为例)
  - [安装Docker](#安装docker)

## 配置root账户
WSL安装好后，默认是进入普通账户，需要配置root账户后才能进如root账户

首先设置root账户密码

```bash
sudo passwd root
```

设置完密码后，切换到root账户(su root)，输入密码即可

## 配置root账户命令行颜色
root账户默认是没有命令行颜色的，看起来不好看，但普通账户是有配色的，所以将普通账户的配色文件复制给root账户即可。

颜色文件保存在用户文件夹下( **～/.bashrc** )下。

所以切换至普通账户，将账户文件夹下的./bashrc文件复制到root账户的文件夹下即可

```bash
sudo cp ~/.bashrc /root/.bashrc
```

## 配置默认登陆账号为root
WSL默认登陆账号是普通账户，需要配置修改设置才能默认登陆root账号，只需要以管理员模式打开powershell下执行如下命令即可

```bash
# Debian是系统名，如果是其他系统则更改系统名即可
Debian config --default-user root
```

## 配置默认WSL
如果安装了多个WSL，想修改默认的WSL，只需要以管理员模式打开powershell下执行如下命令即可

```bash
# Debian是系统名，如果是其他系统则更改系统名即可
wslconfig /setdefault Debian
```

## 切换软件源(此处以Debian为例)
默认的软件源速度比较慢，一般都会切换其他软件源，此处以阿里云的源为例

1. 执行一次更新
    ```bash
    # 刷信软件包信息
    sudo apt update
    # 升级软件包
    sudo apt upgrade
    ```
2. 打开[阿里巴巴镜像网站](https://developer.aliyun.com/mirror/) ，选择所需要的系统
3. 查看当前系统版本号
    ```bash
    # 执行指令
    cat /etc/issue
    # 返回信息，代表当前是Debian 11，所以选择Debian11相关的源
    Debian GNU/Linux 11 \n \l
    ```
4. 备份默认源
   ```bash
   sudo cp /etc/apt/sources.list /etc/apt/sources.list_bak
   ```
5. 修改镜像源
   ```bash
   # 1. 打开源文件
   sudo vi /etc/apt/sources.list
   
   # 2. 注释默认的内容
   # deb http://deb.debian.org/debian bullseye main
   # deb http://deb.debian.org/debian bullseye-updates main
   # deb http://security.debian.org/debian-security bullseye-security main
   # deb http://ftp.debian.org/debian bullseye-backports main
   
   # 3. 粘贴阿里镜像内容
   deb http://mirrors.aliyun.com/debian/ bullseye main non-free contrib
   deb-src http://mirrors.aliyun.com/debian/ bullseye main non-free contrib
   deb http://mirrors.aliyun.com/debian-security/ bullseye-security main
   deb-src http://mirrors.aliyun.com/debian-security/ bullseye-security main
   deb http://mirrors.aliyun.com/debian/ bullseye-updates main non-free contrib
   deb-src http://mirrors.aliyun.com/debian/ bullseye-updates main non-free contrib
   deb http://mirrors.aliyun.com/debian/ bullseye-backports main non-free contrib
   deb-src http://mirrors.aliyun.com/debian/ bullseye-backports main non-free contrib
   
   # 4. 保存并退出
   ```
6. 再次执行更新指令
   ```bash
   # 刷信软件包信息
   sudo apt update
   # 升级软件包
   sudo apt upgrade
    ```
   
## 配置ll指令
Debian默认是不支持ll的，ll其实就是ls -l，只需要修改.bashrc文件即可
```bash
# 1. 打开bash.rc文件
vi ~/.bashrc

# 2. 找到如下内容
# some more ls aliases
#alias ll='ls -l'
#alias la='ls -A'
#alias l='ls -CF'

# 3. 去掉对应注释
# some more ls aliases
alias ll='ls -l'
alias la='ls -A'
alias l='ls -CF'

# 4. 保存并退出
```
然后重新链接WSL即可

## 安装Docker
Docker安装Windows版本即可。

安装完后，打开Docker，右上角点击设置(齿轮图标)，选择Resource -> WSL INTEGRATION

勾上Enable integration with my default WSL distro，然后选择所需要的WSL系统即可。

注：需要WSL2才可以使用Windows的Docker，如果是WSL1，可以以管理员打开powershell执行如下操作

```bash
# 1. 查看wsl版本
wsl -l -v

# 2. 修改版本(Debian是系统名，如果是其他系统则更改系统名即可)
wsl --set-version Debian 2

# 3. 也可以设置默认的WSL版本号为2，这样以后所有安装的WSL版本号都是2
wsl --set-default-version 2
```

## WLS2使用Windows代理
1. 检查WSL2能不能PING通Windows主机
   ```bash
   cat /etc/resolv.conf |grep -oP '(?<=nameserver\ ).*'
   172.29.160.1
   ```
2. 在WSL2下Ping该地址
   ```bash
   ping 172.29.160.1
   ```
   如果Ping不通，在Windows下已管理员模式打开powershell，输入以下命令
   ```bash
   New-NetFirewallRule -DisplayName "WSL" -Direction Inbound  -InterfaceAlias "vEthernet (WSL)"  -Action Allow
   ```
   输入后，就可以ping通了
3. 打开Windows的代理，勾选如(允许来自局域网的连接)或(ALLOW LAN)之类的选项，并确认代理的端口(如我的是7890)
4. 在WSL2下输入以下指令
   ```bash
   export hostip=$(cat /etc/resolv.conf |grep -oP '(?<=nameserver\ ).*')
   export https_proxy="http://${hostip}:7890"
   export http_proxy="http://${hostip}:7890"
   ```
5. 如果不需要代理了，执行如下指令删除配置即可
   ```bash
   export -n https_proxy
   export -n http_proxy
   ```

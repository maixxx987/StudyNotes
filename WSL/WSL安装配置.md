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

## WSL2固定IP
参考: https://www.cnblogs.com/RainFate/p/15796866.html


WSL2的IP会经常自己发生改变，可以通过安装一个软件，wsl2host，每次将wsl2的ip写入host，用来固定域名。

### 安装并启动WSL2

[wsl2host](https://github.com/shayne/go-wsl2-host)

下载后，切换到对应wsl2host的目录，执行如下命令
```bash
.\wsl2host.exe install
# 输入用户名
Windows Username: <username-you-use-to-login-to-windows>
# 输入密码
Windows Password: <password-for-this-user>
```
输入完后，可以去服务中，查看windows服务中的WSL2 HOST服务是否启动，若未启动可以手动启动。

如果启动失败，可以执行如下操作
1. 命令行输入 **secpol.msc**
2. 在左侧窗口展开本地策略，选择用户权限分配
3. 在右侧窗口双击作为服务登录
4. 点击添加用户组，将当前电脑的用户名输入进去，微软会自动补全对应的用户名
5. 重启windows服务中的WSL2 HOST服务

启动成功后，前往host目录(C:\Windows\System32\drivers\etc\hosts)检查host文件，此时应该会多出一行wsl2的ip和对应的域名

### 卸载WSL2
以管理员模式打开powershell，输入一下命令
```bash
wsl2host.exe stop
wsl2host.exe remove
```

### 自定义域名
进入 WSL ，新建 ~/.wsl2hosts 文件，输入自定义的域名，多个则用逗号隔开，如下所示
```
debian.wsl  myServer.wsl wsl.loacl
```
退出 WSL ，重启 WSL2 Host 服务。


## 配置SSH使其他电脑能远程登录WSL2
1. 部分系统可能需要先安装ssh服务
    ```bash
    apt instal openssh-server
    ```
2. 打开配置文件，修改端口
    ```bash
    vim /etc/ssh/sshd_config
    
    # 找到以下几个配置
    # ssh端口
    Port 2211
    AddressFamily any
    ListenAddress 0.0.0.0
    ListenAddress ::
    # 允许root登录
    PermitRootLogin yes
    # 允许密码登录
    PasswordAuthentication yes
    ```
3. 在win10中以管理员模式打开powershell，输入以下指令
    ```bash
    # listenport     win10监听的端口号
    # listenaddress  win10监听的外网地址，0.0.0.0指所有地址
    # connectport    映射的linux的端口，也就是上述配置文件中的端口
    # connectaddress linux的ip，此处因为我已经修改了host，所以直接写host中的地址即可
    # 可以理解为，所有主机发往win10:2222端口的tcp信息都会转发到debian.wsl:2211
    netsh interface portproxy set v4tov4 listenport=2222 listenaddress=0.0.0.0 connectport=2211 connectaddress=debian.wsl
    ```
4. 配置win10防火墙
   1. 打开WIN10防火墙
   2. 选择高级设置
   3. 点击左侧入站规则
   4. 点击右侧新建规则
   5. 规则类型选择端口
   6. 协议和端口，在本地特定端口配置刚刚填写的2222
   7. 其他默认下一步即可
5. 查看win10的ip(使用ipconfig命令)
6. 使用其他电脑，直接执行ssh命令即可
    ```bash
    # ip是win10的ip
    # 端口是win10配置的端口
    # 用户是linux的用户
    ssh root@192.168.3.1 -p 2222
    ```
   

## WLS2使用Windows的软件上网
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
3. 打开Windows的软件，勾选如(允许来自局域网的连接)或(ALLOW LAN)之类的选项，并确认软件的端口(如我的是7890)
4. 在WSL2下输入以下指令
   ```bash
   export hostip=$(cat /etc/resolv.conf |grep -oP '(?<=nameserver\ ).*')
   export https_proxy="http://${hostip}:7890"
   export http_proxy="http://${hostip}:7890"
   ```
5. 如果不需要软件了，执行如下指令删除配置即可
   ```bash
   export -n https_proxy
   export -n http_proxy
   ```
